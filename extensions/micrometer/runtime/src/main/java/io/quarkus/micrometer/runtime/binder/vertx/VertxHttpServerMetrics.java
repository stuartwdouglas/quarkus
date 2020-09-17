package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.http.Outcome;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.spi.metrics.HttpServerMetrics;

/**
 * HttpServerMetrics<R, W, S>
 * <ul>
 * <li>R for Request metric -- MetricsContext</li>
 * <li>W for Websocket metric -- LongTaskTimer sample</li>
 * <li>S for Socket metric -- MetricsContext</li>
 * </ul>
 */
public class VertxHttpServerMetrics extends VertxTcpMetrics
        implements HttpServerMetrics<MetricsContext, LongTaskTimer.Sample, Map<String, Object>> {
    static final Logger log = Logger.getLogger(VertxHttpServerMetrics.class);

    final List<Pattern> ignorePatterns;
    final List<Pattern> matchPatterns;

    final String nameWebsocketConnections;
    final String nameHttpServerPush;
    final String nameHttpServerRequests;

    VertxHttpServerMetrics(MeterRegistry registry, VertxConfig config) {
        super(registry, "http.server");
        nameWebsocketConnections = "http.server.websocket.connections";
        nameHttpServerPush = "http.server.push";
        nameHttpServerRequests = "http.server.requests";

        if (config.ignorePatterns.isPresent()) {
            List<String> stringPatterns = config.ignorePatterns.get();
            ignorePatterns = new ArrayList<>(stringPatterns.size());
            for (String s : stringPatterns) {
                ignorePatterns.add(Pattern.compile(s));
            }
        } else {
            ignorePatterns = Collections.emptyList();
        }

        if (config.matchPatterns.isPresent()) {
            List<String> stringPatterns = config.matchPatterns.get();
            matchPatterns = new ArrayList<>(stringPatterns.size());
            for (String s : stringPatterns) {
                matchPatterns.add(Pattern.compile(s));
            }
        } else {
            matchPatterns = Collections.emptyList();
        }
    }

    /**
     * Called when an http server response is pushed.
     *
     * @param socketMetric a MetricsContext object for socket metric context
     * @param method the pushed response method
     * @param uri the pushed response uri
     * @param response the http server response
     * @return a MetricsContext object for request metric context or null
     */
    @Override
    public MetricsContext responsePushed(Map<String, Object> socketMetric, HttpMethod method, String uri,
            HttpServerResponse response) {
        MetricsContext metricsContext = new MetricsContext();
        String path = VertxMetricsTags.parseUriPath(matchPatterns, ignorePatterns, uri);
        if (path != null) {
            registry.counter(nameHttpServerPush,
                    Tags.of(VertxMetricsTags.uri(path, response.getStatusCode()), VertxMetricsTags.method(method),
                            VertxMetricsTags.outcome(response), VertxMetricsTags.status(response.getStatusCode())))
                    .increment();
        }
        return metricsContext;
    }

    /**
     * Called when an http server request begins. Vert.x will invoke
     * {@link #responseEnd} when the response has ended or {@link #requestReset} if
     * the request/response has failed before.
     *
     * @param socketMetric a MetricsContext object for socket metric context
     * @param request the http server request
     * @return a MetricsContext object for request metric context or null
     */
    @Override
    public MetricsContext requestBegin(Map<String, Object> socketMetric, HttpServerRequest request) {
        String path = VertxMetricsTags.parseUriPath(matchPatterns, ignorePatterns, request.uri());
        MetricsContext metricsContext = new MetricsContext();
        if (path != null) {
            // Pre-add the request method tag to the sample
            metricsContext.put(MetricsContext.HTTP_REQUEST_SAMPLE,
                    Timer.start(registry).tags(Tags.of(VertxMetricsTags.method(request.method()))));

            // remember the path to monitor for use later (maybe a 404 or redirect..)
            metricsContext.put(MetricsContext.HTTP_REQUEST_PATH, path);
        }
        FakeMetricsCookie fakeMetricsCookie = new FakeMetricsCookie();
        fakeMetricsCookie.metricsContext = metricsContext;
        request.cookieMap().put(FakeMetricsCookie.NAME, fakeMetricsCookie);
        return metricsContext;
    }

    /**
     * Called when the http server request couldn't complete successfully, for
     * instance the connection was closed before the response was sent.
     *
     * @param requestMetric a MetricsContext object for request metric context or null
     */
    @Override
    public void requestReset(MetricsContext requestMetric) {
        Timer.Sample sample = getRequestSample(requestMetric);
        if (sample != null) {
            String requestPath = getServerRequestPath(requestMetric);
            sample.stop(registry,
                    Timer.builder(nameHttpServerRequests)
                            .tags(Tags.of(VertxMetricsTags.uri(requestPath, 0),
                                    Outcome.CLIENT_ERROR.asTag(), VertxMetricsTags.STATUS_RESET)));
        }
    }

    /**
     * Called when an http server response has ended.
     *
     * @param requestMetric a MetricsContext object for request metric context or null
     * @param response the http server response
     */
    @Override
    public void responseEnd(MetricsContext requestMetric, HttpServerResponse response) {
        Timer.Sample sample = getRequestSample(requestMetric);
        if (sample != null) {
            String requestPath = getServerRequestPath(requestMetric);
            sample.stop(registry, Timer.builder(nameHttpServerRequests)
                    .tags(Tags.of(
                            VertxMetricsTags.uri(requestPath, response.getStatusCode()),
                            VertxMetricsTags.outcome(response),
                            VertxMetricsTags.status(response.getStatusCode()))));
        }
    }

    /**
     * Called when a server web socket connects.
     *
     * @param socketMetric a MetricsContext object for socket metric context or null
     * @param requestMetric a MetricsContext object for request metric context or null
     * @param serverWebSocket the server web socket
     * @return a LongTaskTimer.Sample containing websocket metric context or null
     */
    @Override
    public LongTaskTimer.Sample connected(Map<String, Object> socketMetric, MetricsContext requestMetric,
            ServerWebSocket serverWebSocket) {
        String path = getServerRequestPath(requestMetric);
        if (path != null) {
            return LongTaskTimer.builder(nameWebsocketConnections)
                    .tags(Tags.of(VertxMetricsTags.uri(path, 0)))
                    .register(registry)
                    .start();
        }
        return null;
    }

    /**
     * Called when the server web socket has disconnected.
     *
     * @param websocketMetric a LongTaskTimer.Sample containing websocket metric context or null
     */
    @Override
    public void disconnected(LongTaskTimer.Sample websocketMetric) {
        if (websocketMetric != null) {
            websocketMetric.stop();
        }
    }

    private Timer.Sample getRequestSample(MetricsContext requestMetric) {
        if (requestMetric == null) {
            return null;
        }
        return requestMetric.getValue(MetricsContext.HTTP_REQUEST_SAMPLE);
    }

    private String getServerRequestPath(MetricsContext source) {
        if (source == null) {
            return null;
        }

        String path = source.getFromRoutingContext(MetricsContext.HTTP_REQUEST_PATH);
        if (path != null) {
            log.debugf("Using path from routing context %s", path);
            return path;
        }
        return source.getValue(MetricsContext.HTTP_REQUEST_PATH);
    }
}
