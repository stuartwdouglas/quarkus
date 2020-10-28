package io.quarkus.rest.server.runtime.jaxrs;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;

public class QuarkusRestSse implements Sse {

    public static final QuarkusRestSse INSTANCE = new QuarkusRestSse();

    @Override
    public OutboundSseEvent.Builder newEventBuilder() {
        return new QuarkusRestOutboundSseEvent.BuilderImpl();
    }

    @Override
    public SseBroadcaster newBroadcaster() {
        return new QuarkusRestSseBroadcasterImpl();
    }
}
