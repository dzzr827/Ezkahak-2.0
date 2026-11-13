package dev.ezkahak.client;

import dev.ezkahak.client.config.ConfigManager;
import dev.ezkahak.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EzkaHakClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static KeyBinding hudEditorKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.initialize();
        try { ConfigManager.load(); }
        catch (IOException exception) { System.err.println("[EzkaHak] Could not load config: " + exception.getMessage()); }

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ezkahak.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.MISC));
        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ezkahak.hud_editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, KeyBinding.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new EzkaHakClickGui());
            }
            while (hudEditorKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new HudEditorScreen());
            }
            ModuleManager.tick(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> { MinecraftClient mc = MinecraftClient.getInstance(); renderHud(mc, drawContext); NotificationManager.render(mc, drawContext); });
    }

    private static void renderHud(MinecraftClient client, DrawContext context) {
        if (client.player == null || client.options.hudHidden) return;
        int x = HudLayout.x, y = HudLayout.y;
        int cyan = 0xFF22B8FF;
        var effects = ModuleManager.find("Visual Effects");
        if (effects != null && effects.isEnabled()) {
            boolean animated = effects.getSettings().stream().filter(s -> s instanceof dev.ezkahak.client.setting.BooleanSetting).map(s -> (dev.ezkahak.client.setting.BooleanSetting)s).filter(s -> s.getName().equals("Animated accent")).findFirst().map(dev.ezkahak.client.setting.BooleanSetting::get).orElse(true);
            if (animated) { float phase = (System.currentTimeMillis() % 2400L) / 2400.0f; int blue = (int)(150 + 90 * (0.5 + 0.5 * Math.sin(phase * Math.PI * 2))); cyan = 0xFF000000 | (34 << 16) | (blue << 8) | 255; }
        }
        var hud = ModuleManager.find("HUD");
        if (hud != null && hud.isEnabled()) {
            boolean showBrand = true, showFps = true, showCoords = true, showPing = true, showArmor = true;
            String accent = "Cyan";
            for (var setting : hud.getSettings()) {
                if (setting instanceof dev.ezkahak.client.setting.BooleanSetting b) {
                    if (b.getName().equals("FPS")) showFps = b.get();
                    if (b.getName().equals("Coordinates")) showCoords = b.get();
                    if (b.getName().equals("Client name")) showBrand = b.get();
                    if (b.getName().equals("Ping")) showPing = b.get();
                    if (b.getName().equals("Armor status")) showArmor = b.get();
                }
                if (setting instanceof dev.ezkahak.client.setting.ModeSetting m && m.getName().equals("Accent")) accent = m.get();
            }
            if ("Blue".equals(accent)) cyan = 0xFF4F80FF;
            if ("Purple".equals(accent)) cyan = 0xFFB56BFF;
            if (showBrand) { drawHudLine(context, "EZKAHAK 2.0", x, y, cyan); y += 12; }
            if (showFps) { drawHudLine(context, "FPS: " + client.getCurrentFps(), x, y, 0xFFEAF5FF); y += 12; }
            if (showCoords) {
                var pos = client.player.getBlockPos();
                drawHudLine(context, "XYZ: " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), x, y, 0xFFEAF5FF);
                y += 12;
            }
            if (showPing && client.getNetworkHandler() != null) {
                var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) drawHudLine(context, "PING: " + entry.getLatency() + " ms", x, y, 0xFFEAF5FF);
                y += 12;
            }
            if (showArmor) {
                int total = 0, pieces = 0;
                for (var stack : client.player.getArmorItems()) {
                    if (!stack.isEmpty() && stack.isDamageable()) {
                        int percent = Math.max(0, (stack.getMaxDamage() - stack.getDamage()) * 100 / stack.getMaxDamage());
                        total += percent;
                        pieces++;
                    }
                }
                if (pieces > 0) drawHudLine(context, "ARMOR: " + (total / pieces) + "%", x, y, 0xFF7BDFFF);
            }
        }
        var array = ModuleManager.find("Array List");
        if (array != null && array.isEnabled()) {
            List<String> enabled = new ArrayList<>();
            for (var module : ModuleManager.all()) {
                if (module.isEnabled() && !module.getName().equals("Array List") && !module.getName().equals("HUD"))
                    enabled.add(module.getName());
            }
            enabled.sort(Comparator.comparingInt((String name) -> client.textRenderer.getWidth(name)).reversed());
            int right = client.getWindow().getScaledWidth() - 8;
            int top = 8;
            for (String name : enabled) {
                int textWidth = client.textRenderer.getWidth(name);
                context.fill(right - textWidth - 8, top - 2, right + 2, top + 10, 0xAA0A101A);
                context.fill(right - textWidth - 8, top - 2, right - textWidth - 6, top + 10, cyan);
                context.drawText(client.textRenderer, Text.literal(name), right - textWidth - 4, top, 0xFFEAF5FF, true);
                top += 13;
            }
        }
    }

    private static void drawHudLine(DrawContext context, String text, int x, int y, int color) {
        context.fill(x - 3, y - 2, x + net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(text) + 4, y + 10, 0xAA0A101A);
        context.fill(x - 3, y - 2, x - 1, y + 10, color);
        context.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer, Text.literal(text), x, y, color, true);
    }
}
