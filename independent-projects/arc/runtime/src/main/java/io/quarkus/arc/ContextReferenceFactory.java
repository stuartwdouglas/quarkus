package io.quarkus.arc;

import io.quarkus.arc.InjectableContext.ContextState;

/**
 * This factory is used to create a new {@link ContextReference} for a non-shared context of a normal scope, e.g. the request
 * context.
 *
 * @param <T>
 */
public interface ContextReferenceFactory {

    <T extends ContextState> ContextReference<T> create();

}
