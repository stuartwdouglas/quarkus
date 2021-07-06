package io.quarkus.deployment.dev;

import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;

public class FailedStartConsumer implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        for (var i : buildResult.consumeMulti(FailedStartBuildItem.class)) {
            if (i.getTask() != null) {
                i.getTask().run();
            }
        }
    }
}
