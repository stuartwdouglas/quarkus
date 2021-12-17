package io.quarkus.arc.impl;

import io.quarkus.arc.ContextReference;
import io.quarkus.arc.InjectableContext.ContextState;

/**
 * {@link ThreadLocal} implementation of {@link ContextReference}.
 *
 * @param <T>
 */
final class ThreadLocalContextReference<T extends ContextState> implements ContextReference<T> {

    private final ThreadLocal<T> currentContext = new ThreadLocal<>();

    @Override
    public T get() {
        return currentContext.get();
    }

    @Override
    public void set(T state) {
        currentContext.set(state);
    }

    @Override
    public void remove() {
        currentContext.remove();
    }

}
