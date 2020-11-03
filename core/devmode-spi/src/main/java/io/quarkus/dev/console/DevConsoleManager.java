package io.quarkus.dev.console;

import java.util.Collections;
import java.util.function.Consumer;

public class DevConsoleManager {

    private static volatile Consumer<DevConsoleRequest> handler;

    public static void registerHandler(Consumer<DevConsoleRequest> requestHandler) {
        handler = requestHandler;
    }

    public static void sentRequest(DevConsoleRequest request) {
        Consumer<DevConsoleRequest> handler = DevConsoleManager.handler;
        if (handler == null) {
            request.getResponse().complete(new DevConsoleResponse(503, Collections.emptyMap(), new byte[0])); //service unavailable
        } else {
            handler.accept(request);
        }
    }

}
