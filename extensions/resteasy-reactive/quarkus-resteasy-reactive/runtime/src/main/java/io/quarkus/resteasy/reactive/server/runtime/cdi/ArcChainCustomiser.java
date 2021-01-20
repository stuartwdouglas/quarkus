package io.quarkus.resteasy.reactive.server.runtime.cdi;

import java.util.Collections;
import java.util.List;

import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 *
 */
public class ArcChainCustomiser implements HandlerChainCustomizer {
    @Override
    public List<ServerRestHandler> handlers(Phase phase) {
        return Collections.emptyList();
    }
}
