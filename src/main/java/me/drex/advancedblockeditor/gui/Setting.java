package me.drex.advancedblockeditor.gui;

import net.minecraft.util.Mth;

import java.util.function.Function;

public final class Setting {

    private int value;
    private final int min;
    private final int max;
    private final Function<Integer, Double> calculateResult;

    public Setting(int defaultValue, int min, int max, Function<Integer, Double> calculateResult) {
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.calculateResult = calculateResult;
    }

    public void updateValue(int delta) {
        value = Mth.clamp(value + delta, min, max);
    }

    public double getResult() {
        return calculateResult.apply(value);
    }

}
