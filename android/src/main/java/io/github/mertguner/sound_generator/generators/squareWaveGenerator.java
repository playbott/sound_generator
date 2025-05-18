package io.github.mertguner.sound_generator.generators;

public class squareWaveGenerator extends baseGenerator {
    public short getValue(double phase, double period, float amplitude) {
        if (phase <= (period / 2)) {
            return (short) (amplitude * Short.MAX_VALUE);
        } else {
            return (short) (amplitude * -Short.MAX_VALUE);
        }
    }
}
