package org.jboss.resteasy.reactive.server.processor;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;

public interface ResourceMethodCustomizer {

    void customize();

    /**
     * The priority of the customizer.
     * All customizers are sorted by priority - in keeping with how JAX-RS handles priorities, lower priority
     * customizers are executed first
     */
    default int priority() {
        return 0;
    }

    interface Context {

        ServerResourceMethod resourceMethod();

        ResourceClass resourceClass();

        String deploymentPath();
    }
}
