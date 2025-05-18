package io.github.mertguner.sound_generator.generators;

public class sawtoothGenerator extends baseGenerator {
    public short getValue(double phase, double period, float amplitude) {
        if (phase < (period / 2))
            return (short) (amplitude * Short.MAX_VALUE * (((2. * phase) / Math.PI) - 1));
        else
            return (short) (amplitude * Short.MAX_VALUE * (((2. * phase) / Math.PI) - 3));
    }
}
