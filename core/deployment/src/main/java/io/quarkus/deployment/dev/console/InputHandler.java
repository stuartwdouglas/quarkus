package io.quarkus.deployment.dev.console;

import java.util.function.Consumer;

public interface InputHandler {

    void handleInput(int[] keys);

    void promptHandler(Consumer<String> promptHandler);
}
