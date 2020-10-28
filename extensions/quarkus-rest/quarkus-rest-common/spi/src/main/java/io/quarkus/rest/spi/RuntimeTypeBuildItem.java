package io.quarkus.rest.spi;

import javax.ws.rs.RuntimeType;

public interface RuntimeTypeBuildItem {

    /**
     * Returns the runtime type for this build item. If the value is null
     * then it applies to both server and client.
     */
    RuntimeType getRuntimeType();

}
