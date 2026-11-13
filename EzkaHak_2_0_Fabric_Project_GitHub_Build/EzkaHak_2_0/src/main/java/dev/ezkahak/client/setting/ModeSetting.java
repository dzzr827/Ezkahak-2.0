package dev.ezkahak.client.setting;

import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> modes;
    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = List.of(modes);
        if (!this.modes.contains(defaultValue) && !this.modes.isEmpty()) set(this.modes.get(0));
    }
    public List<String> modes() { return modes; }
    public void cycle() {
        if (modes.isEmpty()) return;
        int index = modes.indexOf(get());
        set(modes.get((index + 1) % modes.size()));
    }
    @Override protected String normalize(String value) {
        return modes.contains(value) ? value : getDefaultValue();
    }
    @Override public String serialize() { return get(); }
    @Override public void deserialize(String value) { set(value); }
}
