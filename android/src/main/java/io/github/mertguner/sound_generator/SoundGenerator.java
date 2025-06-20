package io.github.mertguner.sound_generator;

import android.os.Handler;
import android.os.Looper;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import io.github.mertguner.sound_generator.generators.sawtoothGenerator;
import io.github.mertguner.sound_generator.generators.signalDataGenerator;
import io.github.mertguner.sound_generator.generators.sinusoidalGenerator;
import io.github.mertguner.sound_generator.generators.squareWaveGenerator;
import io.github.mertguner.sound_generator.generators.triangleGenerator;
import io.github.mertguner.sound_generator.handlers.isPlayingStreamHandler;
import io.github.mertguner.sound_generator.models.WaveTypes;

public class SoundGenerator {
    private Thread bufferThread;
    private AudioTrack audioTrack;
    private signalDataGenerator generator;

    private volatile boolean isPlaying = false;
    private volatile boolean isStopping = false;

    private int minSamplesSize;
    private WaveTypes waveType = WaveTypes.SINUSOIDAL;
    private float rightVolume = 1, leftVolume = 1, volume = 1, dB = -20;
    private boolean cleanStart = false;
    private float amplitude = 1.0f;
    private int fadeDurationMs = 20;

    public boolean init(int sampleRate) {
        try {
            minSamplesSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

            generator = new signalDataGenerator(minSamplesSize, sampleRate);
            generator.setAmplitude(amplitude);

            generator.start();

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minSamplesSize, AudioTrack.MODE_STREAM);

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void startPlayback() {
        if (bufferThread != null || audioTrack == null) return;

        if (cleanStart) {
            generator.resetFrequency();
        }

        isPlaying = true;
        isStopping = false;

        bufferThread = new Thread(() -> {
            audioTrack.flush();
            audioTrack.setPlaybackHeadPosition(0);
            audioTrack.play();

            float envelope = 0.0f;
            final float envelopeStep = (fadeDurationMs > 0) ? (1.0f / (getSampleRate() * (fadeDurationMs / 1000.0f))) : 1.0f;

            while (isPlaying) {
                try {
                    short[] originalSamples = generator.getData();

                    for (int i = 0; i < originalSamples.length; i++) {
                        if (isStopping) {
                            envelope -= envelopeStep;
                        } else {
                            envelope += envelopeStep;
                        }
                        envelope = Math.max(0.0f, Math.min(1.0f, envelope));

                        originalSamples[i] = (short) (originalSamples[i] * envelope);
                    }

                    audioTrack.write(originalSamples, 0, originalSamples.length);

                    if (isStopping && envelope <= 0.0f) {
                        isPlaying = false;
                    }

                } catch (InterruptedException e) {
                    isPlaying = false;
                    Thread.currentThread().interrupt();
                }
            }
            audioTrack.stop();
        });

        isPlayingStreamHandler.change(true);
        bufferThread.start();
    }

    public void stopPlayback() {
        if (bufferThread == null || isStopping) return;

        isStopping = true;

        new Thread(() -> {
            try {
                if (bufferThread != null) {
                    bufferThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                new Handler(Looper.getMainLooper()).post(() -> {
                    isPlayingStreamHandler.change(false);
                    bufferThread = null;
                });
            }
        }).start();
    }

    public void release() {
        if (generator != null) generator.stop();
        if (isPlaying()) stopPlayback();
        if (audioTrack != null) audioTrack.release();
    }

    public void setFadeDuration(int duration) {
        this.fadeDurationMs = Math.max(0, duration);
    }

    public void setCleanStart(boolean cleanStart) {
        this.cleanStart = cleanStart;
    }

    public void setAutoUpdateOneCycleSample(boolean autoUpdateOneCycleSample) {
        if (generator != null) generator.setAutoUpdateOneCycleSample(autoUpdateOneCycleSample);
    }

    public int getSampleRate() {
        if (generator != null) return generator.getSampleRate();
        return 0;
    }

    public void setSampleRate(int sampleRate) {
        if (generator != null) generator.setSampleRate(sampleRate);
    }

    public void refreshOneCycleData() {
        if (generator != null) generator.createOneCycleData(true);
    }

    public void setFrequency(float v) {
        if (generator != null) generator.setFrequency(v);
    }

    public float getFrequency() {
        if (generator != null) return generator.getFrequency();
        return 0;
    }

    public void setBalance(float balance) {
        balance = Math.max(-1, Math.min(1, balance));
        rightVolume = (balance >= 0) ? 1 : (balance == -1) ? 0 : (1 + balance);
        leftVolume = (balance <= 0) ? 1 : (balance == 1) ? 0 : (1 - balance);
        if (audioTrack != null) {
            audioTrack.setStereoVolume(leftVolume, rightVolume);
        }
    }

    public void setVolume(float volume, boolean recalculateDecibel) {
        volume = Math.max(0, Math.min(1, volume));
        this.volume = volume;
        if (recalculateDecibel) {
            if (volume >= 0.000001f) {
                this.dB = 20f * (float) Math.log10(volume);
            } else {
                this.dB = -120f;
            }
        }
        if (audioTrack != null) {
            audioTrack.setStereoVolume(leftVolume * volume, rightVolume * volume);
        }
    }

    public float getVolume() {
        return volume;
    }

    public void setDecibel(float dB) {
        this.dB = dB;
        float lineerVolume = (float) Math.pow(10f, (dB / 20f));
        if (lineerVolume < 0.000001f) {
            lineerVolume = 0f;
        }
        setVolume(lineerVolume, false);
    }

    public float getDecibel() {
        return dB;
    }

    public void setAmplitude(float amplitude) {
        this.amplitude = Math.max(0, Math.min(1, amplitude));
        if (generator != null) {
            generator.updateOnce();
            refreshOneCycleData();
        }
    }

    public float getAmplitude() {
        return amplitude;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setWaveform(WaveTypes waveType) {
        if (this.waveType.equals(waveType) || (generator == null)) return;
        this.waveType = waveType;
        if (waveType.equals(WaveTypes.SINUSOIDAL)) generator.setGenerator(new sinusoidalGenerator());
        else if (waveType.equals(WaveTypes.TRIANGLE)) generator.setGenerator(new triangleGenerator());
        else if (waveType.equals(WaveTypes.SQUAREWAVE)) generator.setGenerator(new squareWaveGenerator());
        else if (waveType.equals(WaveTypes.SAWTOOTH)) generator.setGenerator(new sawtoothGenerator());
    }
}