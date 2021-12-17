package io.quarkus.arc.deployment;

import io.quarkus.arc.ContextReferenceFactory;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * An extension can provide a custom {@link ContextReferenceFactory}.
 */
public final class ContextReferenceFactoryBuildItem extends SimpleBuildItem {

    private final RuntimeValue<ContextReferenceFactory> factory;

    public ContextReferenceFactoryBuildItem(RuntimeValue<ContextReferenceFactory> factory) {
        this.factory = factory;
    }

    public RuntimeValue<ContextReferenceFactory> getFactory() {
        return factory;
    }

}
