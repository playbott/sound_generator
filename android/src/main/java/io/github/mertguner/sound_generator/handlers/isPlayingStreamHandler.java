package io.github.mertguner.sound_generator.handlers;

import java.util.function.Consumer;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;

public class isPlayingStreamHandler implements StreamHandler {
    public static final String NATIVE_CHANNEL_EVENT = "io.github.mertguner.sound_generator/onChangeIsPlaying";
    
    private final Consumer<EventSink> onListenConsumer;

    public isPlayingStreamHandler(Consumer<EventSink> onListenConsumer) {
        this.onListenConsumer = onListenConsumer;
    }

    @Override
    public void onListen(Object o, EventSink eventSink) {
        onListenConsumer.accept(eventSink);
    }

    @Override
    public void onCancel(Object o) {
        onListenConsumer.accept(null);
    }
}