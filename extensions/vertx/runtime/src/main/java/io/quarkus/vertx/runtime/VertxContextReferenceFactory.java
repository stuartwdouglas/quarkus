package io.quarkus.vertx.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.ContextReference;
import io.quarkus.arc.ContextReferenceFactory;
import io.quarkus.arc.InjectableContext;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class VertxContextReferenceFactory implements ContextReferenceFactory {

    @Override
    public <T extends InjectableContext.ContextState> ContextReference<T> create() {
        ThreadLocal<T> fallback = new ThreadLocal<>();
        return new ContextReference<T>() {
            @Override
            public T get() {
                Supplier<Vertx> vertx = VertxCoreRecorder.getVertx();
                if (vertx != null) {
                    Context context = vertx.get().getOrCreateContext();
                    return context.get(this);
                } else {
                    return fallback.get();
                }
            }

            @Override
            public void set(T state) {
                Supplier<Vertx> vertx = VertxCoreRecorder.getVertx();
                if (vertx != null) {
                    Context context = vertx.get().getOrCreateContext();
                    context.put(this, state);
                } else {
                    fallback.set(state);
                }
            }

            @Override
            public void remove() {
                Supplier<Vertx> vertx = VertxCoreRecorder.getVertx();
                if (vertx != null) {
                    Context context = vertx.get().getOrCreateContext();
                    //context.remove(this);
                } else {
                    fallback.remove();
                }
            }
        };
    }
}
