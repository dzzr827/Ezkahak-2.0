package dev.ezkahak.client.setting;

public abstract class Setting<T> {
    private final String name;
    private final T defaultValue;
    private T value;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() { return name; }
    public T get() { return value; }
    public T getDefaultValue() { return defaultValue; }
    public void set(T value) { this.value = normalize(value); }
    protected T normalize(T value) { return value; }
    public void reset() { value = defaultValue; }
    public abstract String serialize();
    public abstract void deserialize(String value);
}
