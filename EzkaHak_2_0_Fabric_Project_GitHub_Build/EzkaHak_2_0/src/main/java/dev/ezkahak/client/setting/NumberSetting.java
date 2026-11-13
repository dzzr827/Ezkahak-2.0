package dev.ezkahak.client.setting;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        set(defaultValue);
    }
    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }
    public void increase() { set(get() + step); }
    public void decrease() { set(get() - step); }
    @Override protected Double normalize(Double value) {
        if (value == null || !Double.isFinite(value)) return min;
        double clamped = Math.max(min, Math.min(max, value));
        return Math.round(clamped / step) * step;
    }
    @Override public String serialize() { return Double.toString(get()); }
    @Override public void deserialize(String value) {
        try { set(Double.parseDouble(value)); } catch (NumberFormatException ignored) { }
    }
}
