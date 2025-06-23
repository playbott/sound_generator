package io.github.mertguner.sound_generator.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.github.mertguner.sound_generator.handlers.getOneCycleDataHandler;

public class signalDataGenerator {

    private final float _2Pi = 2.0f * (float) Math.PI;

    private int sampleRate = 48000;
    private float phCoefficient = _2Pi / (float) sampleRate;
    private float smoothStep = 1f / (float) sampleRate * 20f;

    private float frequency = 50;
    private baseGenerator generator = new sinusoidalGenerator();

    private final int bufferSamplesSize;
    private float ph = 0;
    private float oldFrequency = 50;
    private boolean autoUpdateOneCycleSample = false;
    private float amplitude = 1.0f;

    private final BlockingQueue<short[]> bufferQueue;
    private Thread producerThread;
    private volatile boolean isRunning = false;

    public signalDataGenerator(int bufferSamplesSize, int sampleRate) {
        this.bufferSamplesSize = bufferSamplesSize;
        setSampleRate(sampleRate);

        this.bufferQueue = new ArrayBlockingQueue<>(2);
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        producerThread = new Thread(() -> {
            while (isRunning) {
                try {
                    short[] buffer = new short[bufferSamplesSize];
                    generateBuffer(buffer);
                    bufferQueue.put(buffer);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        producerThread.setDaemon(true);
        producerThread.start();
    }

    public void stop() {
        isRunning = false;
        if (producerThread != null) {
            producerThread.interrupt();
        }
        bufferQueue.clear();
    }

    private void generateBuffer(short[] buffer) {
        for (int i = 0; i < bufferSamplesSize; i++) {
            oldFrequency += ((frequency - oldFrequency) * smoothStep);
            buffer[i] = generator.getValue(ph, _2Pi, amplitude);
            ph += (oldFrequency * phCoefficient);

            if (ph > _2Pi) {
                ph -= _2Pi;
            }
        }
    }

    public short[] getData() throws InterruptedException {
        return bufferQueue.take();
    }

    public void updateOnce() {
        createOneCycleData();
    }

    public boolean isAutoUpdateOneCycleSample() {
        return autoUpdateOneCycleSample;
    }

    public void setAutoUpdateOneCycleSample(boolean autoUpdateOneCycleSample) {
        this.autoUpdateOneCycleSample = autoUpdateOneCycleSample;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        phCoefficient = _2Pi / (float) sampleRate;
        smoothStep = 1f / (float) sampleRate * 20f;
    }

    public baseGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(baseGenerator generator) {
        this.generator = generator;
        createOneCycleData();
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
        createOneCycleData();
    }

    public void setAmplitude(float amplitude) {
        this.amplitude = amplitude;
        updateOnce();
    }

    public float getAmplitude() {
        return amplitude;
    }

    public void resetFrequency() {
        oldFrequency = frequency;
    }

    public void createOneCycleData() {
        createOneCycleData(false);
    }

    public void createOneCycleData(boolean force) {
        if (generator == null || (!autoUpdateOneCycleSample && !force)) return;
        int size = Math.round(_2Pi / (frequency * phCoefficient));
        List<Integer> oneCycleBuffer = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            oneCycleBuffer.add((int) generator.getValue((frequency * phCoefficient) * (float) i, _2Pi, amplitude));
        }
        oneCycleBuffer.add((int) generator.getValue(0, _2Pi, amplitude));
        getOneCycleDataHandler.setData(oneCycleBuffer);
    }
}