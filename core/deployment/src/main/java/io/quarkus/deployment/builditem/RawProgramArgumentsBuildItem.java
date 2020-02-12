package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.RawCommandLineArguments;

/**
 * A build item that represents the raw command line arguments as they were passed to the application.
 *
 * No filtering is done on these parameters.
 */
public final class RawProgramArgumentsBuildItem extends SimpleBuildItem
        implements BytecodeRecorderImpl.ReturnedProxy, RawCommandLineArguments {

    @Override
    public String __returned$proxy$key() {
        return RawCommandLineArguments.class.getName();
    }

    @Override
    public boolean __static$$init() {
        return false;
    }

    @Override
    public String[] getArguments() {
        throw new IllegalStateException("Can only be called at runtime");
    }
}
