package io.quarkus.deployment.builditem;

import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents the raw command line arguments as they were passed to the application.
 *
 * No filtering is done on these parameters.
 */
public final class RawProgramArgumentsBuildItem extends SimpleBuildItem {

    private final Supplier<String[]> args;

    public RawProgramArgumentsBuildItem(Supplier<String[]> args) {
        this.args = args;
    }

    public Supplier<String[]> getArgs() {
        return args;
    }
}
