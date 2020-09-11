package io.quarkus.rest.test.rx.rxjava2.resource;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.annotations.SseElementType;
import io.quarkus.rest.test.rx.resource.Thing;

import io.reactivex.Observable;

@Path("")
public interface Rx2ObservableSSECompatibilityResource {

   @GET
   @Path("eventStream/thing")
   @Produces("text/event-stream")
   @SseElementType("application/json")
   void eventStreamThing(@Context SseEventSink eventSink, @Context Sse sse);

   @GET
   @Path("observable/thing")
   @Produces("text/event-stream")
   @SseElementType("application/json")
   Observable<Thing> observableSSE();
}
