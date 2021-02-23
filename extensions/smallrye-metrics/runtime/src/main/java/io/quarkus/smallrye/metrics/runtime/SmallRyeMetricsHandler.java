package io.quarkus.smallrye.metrics.runtime;

import io.smallrye.metrics.MetricsRequestHandler;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.jboss.logging.Logger;

import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

public class SmallRyeMetricsHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = Logger.getLogger(SmallRyeMetricsHandler.class.getName());


    @Override
    public void handle(RoutingContext routingContext) {
        MetricsRequestHandler internalHandler = CDI.current().select(MetricsRequestHandler.class).get();
        HttpServerResponse response = routingContext.response();
        HttpServerRequest request = routingContext.request();
        Stream<String> acceptHeaders = request.headers().getAll("Accept").stream();

        try {
            internalHandler.handleRequest(request.path(), routingContext.mountPoint() == null ? routingContext.currentRoute().getPath() : routingContext.mountPoint() + routingContext.currentRoute().getPath(), request.rawMethod(), acceptHeaders,
                    new MetricsRequestHandler.Responder() {
                        @Override
                        public void respondWith(int status, String message, Map<String, String> headers) throws IOException {
                            response.setStatusCode(status);
                            for (Map.Entry<String, String> entry : headers.entrySet()) {
                                response.putHeader(entry.getKey(), entry.getValue());
                            }
                            response.end(Buffer.buffer(message));
                        }
                    });
        } catch (IOException e) {
            response.setStatusCode(503);
            response.end();
            LOGGER.error(e);
        }
    }
}
