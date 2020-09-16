package io.quarkus.deployment.builditem;

import java.util.concurrent.Future;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A part of runtime startup that is being processed by another thread.
 *
 * The main method will wait for this to complete before printing the startup complete message.
 *
 */
public final class AsyncRuntimeStartupTaskBuildItem extends MultiBuildItem {

    final Future<?> task;

    public AsyncRuntimeStartupTaskBuildItem(Future<?> task) {
        this.task = task;
    }

    public Future<?> getTask() {
        return task;
    }
}
