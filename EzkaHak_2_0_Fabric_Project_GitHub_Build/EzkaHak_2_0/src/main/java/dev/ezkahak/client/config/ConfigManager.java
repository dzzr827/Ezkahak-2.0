package dev.ezkahak.client.config;

import dev.ezkahak.client.HudLayout;
import dev.ezkahak.client.module.ClientModule;
import dev.ezkahak.client.module.ModuleManager;
import dev.ezkahak.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/** Named, persistent profiles for EzkaHak 2.0. */
public final class ConfigManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("ezkahak-2.0");
    private static final Path LEGACY_CONFIG = FabricLoader.getInstance().getConfigDir().resolve("ezkahak-2.0.properties");
    private static final Path ACTIVE_PROFILE_FILE = CONFIG_DIR.resolve("active-profile.txt");
    private static final Pattern SAFE_PROFILE = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static String activeProfile = "default";
    private static boolean profileInitialized;

    private ConfigManager() { }

    private static String normalizeProfile(String profile) {
        if (profile == null) return "";
        String value = profile.trim();
        return SAFE_PROFILE.matcher(value).matches() ? value : "";
    }

    private static Path configPath(String profile) {
        String safe = normalizeProfile(profile);
        if (safe.isEmpty()) safe = "default";
        return CONFIG_DIR.resolve(safe + ".properties");
    }

    public static Path path() { return configPath(activeProfile); }
    public static String activeProfile() { return activeProfile; }

    public static List<String> profiles() throws IOException {
        Files.createDirectories(CONFIG_DIR);
        try (var stream = Files.list(CONFIG_DIR)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".properties"))
                    .map(p -> p.getFileName().toString().replaceFirst("\\.properties$", ""))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
    }

    public static void useProfile(String profile, boolean load) throws IOException {
        String safe = normalizeProfile(profile);
        if (safe.isEmpty()) throw new IOException("Profile name must use 1-32 letters, numbers, _ or -");
        activeProfile = safe;
        profileInitialized = true;
        persistActiveProfile();
        if (load) load(); else save();
    }

    public static void createProfile(String name) throws IOException {
        String safe = normalizeProfile(name);
        if (safe.isEmpty()) throw new IOException("Use 1-32 letters, numbers, _ or -");
        Files.createDirectories(CONFIG_DIR);
        Path target = configPath(safe);
        if (Files.exists(target)) throw new IOException("Profile already exists");
        if (!Files.exists(path())) save();
        Files.copy(path(), target);
        useProfile(safe, true);
    }

    public static void duplicateProfile(String source, String name) throws IOException {
        String src = normalizeProfile(source), dest = normalizeProfile(name);
        if (src.isEmpty() || dest.isEmpty()) throw new IOException("Invalid profile name");
        Files.createDirectories(CONFIG_DIR);
        Path sourcePath = configPath(src), targetPath = configPath(dest);
        if (!Files.exists(sourcePath)) throw new IOException("Source profile does not exist");
        if (Files.exists(targetPath)) throw new IOException("Profile already exists");
        Files.copy(sourcePath, targetPath);
        useProfile(dest, true);
    }

    public static void renameProfile(String oldName, String newName) throws IOException {
        String oldSafe = normalizeProfile(oldName), newSafe = normalizeProfile(newName);
        if (oldSafe.isEmpty() || newSafe.isEmpty()) throw new IOException("Invalid profile name");
        if (oldSafe.equalsIgnoreCase("default")) throw new IOException("The default profile cannot be renamed");
        Path oldPath = configPath(oldSafe), newPath = configPath(newSafe);
        if (!Files.exists(oldPath)) throw new IOException("Profile does not exist");
        if (Files.exists(newPath)) throw new IOException("Target profile already exists");
        Files.move(oldPath, newPath);
        if (activeProfile.equals(oldSafe)) {
            activeProfile = newSafe;
            profileInitialized = true;
            persistActiveProfile();
        }
    }

    public static void deleteProfile(String name) throws IOException {
        String safe = normalizeProfile(name);
        if (safe.isEmpty()) throw new IOException("Invalid profile name");
        if (safe.equalsIgnoreCase("default")) throw new IOException("The default profile cannot be deleted");
        Path target = configPath(safe);
        if (!Files.exists(target)) throw new IOException("Profile does not exist");
        Files.delete(target);
        if (activeProfile.equals(safe)) useProfile("default", true);
    }

    public static void resetCurrentProfile() throws IOException {
        for (ClientModule module : ModuleManager.all()) {
            module.setEnabled(false);
            module.setKeyCode(-1);
            module.resetSettings();
        }
        HudLayout.x = 8;
        HudLayout.y = 8;
        save();
    }

    private static void persistActiveProfile() throws IOException {
        Files.createDirectories(CONFIG_DIR);
        Files.writeString(ACTIVE_PROFILE_FILE, activeProfile, StandardCharsets.UTF_8);
    }

    public static void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("hud.x", Integer.toString(HudLayout.x));
        properties.setProperty("hud.y", Integer.toString(HudLayout.y));
        for (ClientModule module : ModuleManager.all()) {
            String key = module.getName();
            properties.setProperty("module." + key + ".enabled", Boolean.toString(module.isEnabled()));
            properties.setProperty("module." + key + ".key", Integer.toString(module.getKeyCode()));
            for (Setting<?> setting : module.getSettings())
                properties.setProperty("module." + key + ".setting." + setting.getName(), setting.serialize());
        }
        Files.createDirectories(path().getParent());
        try (OutputStream out = Files.newOutputStream(path())) {
            properties.store(out, "EzkaHak 2.0 configuration profile: " + activeProfile);
        }
        persistActiveProfile();
    }

    public static void load() throws IOException {
        if (!profileInitialized) {
            if (Files.exists(ACTIVE_PROFILE_FILE)) {
                String stored = normalizeProfile(Files.readString(ACTIVE_PROFILE_FILE, StandardCharsets.UTF_8));
                if (!stored.isEmpty()) activeProfile = stored;
            }
            profileInitialized = true;
        }
        if (!Files.exists(path())) {
            if (activeProfile.equals("default") && Files.exists(LEGACY_CONFIG)) {
                Files.createDirectories(path().getParent());
                Files.copy(LEGACY_CONFIG, path(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                save();
                return;
            }
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path())) { properties.load(in); }
        try { HudLayout.x = Math.max(4, Integer.parseInt(properties.getProperty("hud.x", "8"))); }
        catch (NumberFormatException ignored) { HudLayout.x = 8; }
        try { HudLayout.y = Math.max(4, Integer.parseInt(properties.getProperty("hud.y", "8"))); }
        catch (NumberFormatException ignored) { HudLayout.y = 8; }
        for (ClientModule module : ModuleManager.all()) {
            String key = module.getName();
            module.setEnabled(Boolean.parseBoolean(properties.getProperty("module." + key + ".enabled", "false")));
            try { module.setKeyCode(Integer.parseInt(properties.getProperty("module." + key + ".key", "-1"))); }
            catch (NumberFormatException ignored) { module.setKeyCode(-1); }
            for (Setting<?> setting : module.getSettings()) {
                String value = properties.getProperty("module." + key + ".setting." + setting.getName());
                if (value != null) setting.deserialize(value);
            }
        }
        persistActiveProfile();
    }
}
