package dev.ezkahak.client.setting;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean defaultValue) { super(name, defaultValue); }
    public void toggle() { set(!get()); }
    @Override public String serialize() { return Boolean.toString(get()); }
    @Override public void deserialize(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) set(Boolean.parseBoolean(value));
    }
}
