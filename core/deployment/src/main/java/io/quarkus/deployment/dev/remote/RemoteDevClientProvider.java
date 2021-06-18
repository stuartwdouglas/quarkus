package io.quarkus.deployment.dev.remote;

import java.util.Optional;

import io.quarkus.runtime.LiveReloadConfig;

public interface RemoteDevClientProvider {

    Optional<RemoteDevClient> getClient(LiveReloadConfig liveReloadConfig);
}
