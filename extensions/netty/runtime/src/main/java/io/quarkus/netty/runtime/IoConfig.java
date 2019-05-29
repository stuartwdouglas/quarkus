package io.quarkus.netty.runtime;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The base Netty IO config, used to configure the number of threads used for IO operations
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class IoConfig {

    /**
     * The number if IO threads used to perform IO. This will be automatically set to a reasonable value based on
     * the number of CPU cores if it is not provided
     */
    @ConfigItem
    public OptionalInt ioThreads;
}
