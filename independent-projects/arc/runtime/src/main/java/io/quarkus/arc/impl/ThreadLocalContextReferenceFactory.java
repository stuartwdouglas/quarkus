package io.quarkus.arc.impl;

import io.quarkus.arc.ContextReference;
import io.quarkus.arc.ContextReferenceFactory;
import io.quarkus.arc.InjectableContext.ContextState;

/**
 * The default implementation makes use of {@link ThreadLocal} variables.
 * 
 * @see ThreadLocalContextReference
 */
final class ThreadLocalContextReferenceFactory implements ContextReferenceFactory {

    @Override
    public <T extends ContextState> ContextReference<T> create() {
        return new ThreadLocalContextReference<>();
    }

}
