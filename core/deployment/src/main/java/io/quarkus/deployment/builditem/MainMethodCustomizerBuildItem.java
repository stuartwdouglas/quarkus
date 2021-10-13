package io.quarkus.deployment.builditem;

import java.util.function.Consumer;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gizmo.BytecodeCreator;

/**
 * Build item that allows you to inject code at the start of the main method, before any recoder code has been run
 */
public final class MainMethodCustomizerBuildItem extends MultiBuildItem implements Comparable<MainMethodCustomizerBuildItem> {

    private final int priority;
    private final Consumer<BytecodeCreator> consumer;

    public MainMethodCustomizerBuildItem(int priority, Consumer<BytecodeCreator> consumer) {
        this.priority = priority;
        this.consumer = consumer;
    }

    public int getPriority() {
        return priority;
    }

    public Consumer<BytecodeCreator> getConsumer() {
        return consumer;
    }

    @Override
    public int compareTo(MainMethodCustomizerBuildItem o) {
        return Integer.compare(priority, o.priority);
    }
}
