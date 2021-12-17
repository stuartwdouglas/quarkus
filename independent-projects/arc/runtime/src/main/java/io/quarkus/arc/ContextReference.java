package io.quarkus.arc;

import io.quarkus.arc.InjectableContext.ContextState;

/**
 * Represents the current context of a normal scope.
 *
 * @param <T>
 * @see ContextReferenceFactory
 */
public interface ContextReference<T extends ContextState> {

    /**
     * 
     * @return the current state
     */
    T get();

    /**
     * Sets the current state.
     * 
     * @param state
     */
    void set(T state);

    /**
     * Removes the current state.
     */
    void remove();

}
