package io.quarkus.deployment.dev;

import io.quarkus.builder.item.MultiBuildItem;

public final class FailedStartBuildItem extends MultiBuildItem {

    final Runnable task;

    public FailedStartBuildItem(Runnable task) {
        this.task = task;
    }

    public Runnable getTask() {
        return task;
    }
}
