package io.quarkus.dev.testing;

import java.util.function.Consumer;

public class TracingHandler {

    private static volatile Consumer<String> tracingHandler;

    public static void trace(String className) {
        Consumer<String> t = tracingHandler;
        if (t != null) {
            t.accept(className);
        }
    }

    public static void setTracingHandler(Consumer<String> tracingHandler) {
        TracingHandler.tracingHandler = tracingHandler;
    }
}
