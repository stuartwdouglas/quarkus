package io.quarkus.rest.server.runtime.spi;

import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;

public interface QuarkusRestContext {
    public void registerCompletionCallback(CompletionCallback callback);

    public void registerConnectionCallback(ConnectionCallback callback);
}
