package io.github.mertguner.sound_generator.handlers;

import java.util.List;
import java.util.function.Consumer;

import io.flutter.plugin.common.EventChannel;

public class getOneCycleDataHandler implements EventChannel.StreamHandler {
    public static final String NATIVE_CHANNEL_EVENT = "io.github.mertguner.sound_generator/onOneCycleDataHandler";
    
    private final Consumer<EventChannel.EventSink> onListenConsumer;

    public getOneCycleDataHandler(Consumer<EventChannel.EventSink> onListenConsumer) {
        this.onListenConsumer = onListenConsumer;
    }

    @Override
    public void onListen(Object o, EventChannel.EventSink eventSink) {
        onListenConsumer.accept(eventSink);
    }

    @Override
    public void onCancel(Object o) {
        onListenConsumer.accept(null);
    }
}
