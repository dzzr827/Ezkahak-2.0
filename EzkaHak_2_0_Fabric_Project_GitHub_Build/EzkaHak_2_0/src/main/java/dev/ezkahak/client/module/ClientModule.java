package dev.ezkahak.client.module;

import dev.ezkahak.client.setting.Setting;
import dev.ezkahak.client.NotificationManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ClientModule {
    private final String name;
    private final String category;
    private final String description;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int keyCode = -1;

    protected ClientModule(String name, String category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public final String getName() { return name; }
    public final String getCategory() { return category; }
    public final String getDescription() { return description; }
    public final boolean isEnabled() { return enabled; }
    public final int getKeyCode() { return keyCode; }
    public final void setKeyCode(int keyCode) { this.keyCode = keyCode < 0 ? -1 : keyCode; }
    public final List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }

    protected final <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public final void toggle() { setEnabled(!enabled); }
    public final void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (enabled) onEnable(); else onDisable();
        ClientModule notifications = ModuleManager.find("Notifications");
        if (!name.equals("Notifications") && notifications != null && notifications.isEnabled()) NotificationManager.push(name + (enabled ? " enabled" : " disabled"));
    }
    public final void resetSettings() { settings.forEach(Setting::reset); }

    protected void onEnable() { }
    protected void onDisable() { }
    public void onClientTick() { }
}
