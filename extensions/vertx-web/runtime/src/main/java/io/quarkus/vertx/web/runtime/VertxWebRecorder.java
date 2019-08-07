package io.quarkus.vertx.web.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.event.Event;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.Timing;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import io.quarkus.runtime.configuration.ssl.ServerSslConfig;
import io.quarkus.vertx.runtime.VertxConfiguration;
import io.quarkus.vertx.runtime.VertxRecorder;
import io.quarkus.vertx.web.Route;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Recorder
public class VertxWebRecorder {

    public static void setHotReplacement(Handler<RoutingContext> handler) {
        hotReplacementHandler = handler;
    }

    private static final Logger LOGGER = Logger.getLogger(VertxWebRecorder.class.getName());

    private static volatile Handler<RoutingContext> hotReplacementHandler;

    private static volatile Router router;
    private static volatile HttpServer server;
    private static volatile HttpServer sslServer;

    public static void startServerAfterFailedStart() {
        VertxConfiguration vertxConfiguration = new VertxConfiguration();
        ConfigInstantiator.handleObject(vertxConfiguration);
        VertxRecorder.initialize(vertxConfiguration);

        try {
            HttpConfiguration config = new HttpConfiguration();
            ConfigInstantiator.handleObject(config);

            router = Router.router(VertxRecorder.getVertx());
            if (hotReplacementHandler != null) {
                router.route().blockingHandler(hotReplacementHandler);
            }

            //we can't really do
            doServerStart(VertxRecorder.getVertx(), config, LaunchMode.DEVELOPMENT);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void configureRouter(RuntimeValue<Vertx> vertx, BeanContainer container, Map<String, List<Route>> routeHandlers,
            List<Handler<RoutingContext>> filters,
            HttpConfiguration httpConfiguration, LaunchMode launchMode, ShutdownContext shutdown,
            Handler<HttpServerRequest> defaultRoute) throws IOException {

        List<io.vertx.ext.web.Route> appRoutes = initialize(vertx.getValue(), httpConfiguration, routeHandlers, filters,
                launchMode, defaultRoute);
        container.instance(RouterProducer.class).initialize(router);

        if (launchMode == LaunchMode.DEVELOPMENT) {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    for (io.vertx.ext.web.Route route : appRoutes) {
                        route.remove();
                    }
                }
            });
        } else {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    server.close();
                    router = null;
                    server = null;
                    if (sslServer != null) {
                        sslServer.close();
                        sslServer = null;
                    }
                }
            });
        }
    }

    List<io.vertx.ext.web.Route> initialize(Vertx vertx, HttpConfiguration httpConfiguration,
            Map<String, List<Route>> routeHandlers,
            List<Handler<RoutingContext>> filters,
            LaunchMode launchMode,
            Handler<HttpServerRequest> defaultRoute) throws IOException {
        List<io.vertx.ext.web.Route> routes = new ArrayList<>();
        if (router == null) {
            router = Router.router(vertx);
            if (hotReplacementHandler != null) {
                router.route().blockingHandler(hotReplacementHandler);
            }
        }
        for (Entry<String, List<Route>> entry : routeHandlers.entrySet()) {
            Handler<RoutingContext> handler = createHandler(entry.getKey());
            for (Route route : entry.getValue()) {
                routes.add(addRoute(router, handler, route, filters));
            }
        }
        // Make it also possible to register the route handlers programmatically
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.select(Router.class).fire(router);

        for (Handler<RoutingContext> i : filters) {
            if (i != null) {
                router.route().handler(i);
            }
        }
        if (defaultRoute != null) {
            //TODO: can we skip the router if no other routes?
            router.route().handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    defaultRoute.handle(event.request());
                }
            });
        }

        // Start the server
        if (server == null) {
            doServerStart(vertx, httpConfiguration, launchMode);
        }
        return routes;
    }

    private static void doServerStart(Vertx vertx, HttpConfiguration httpConfiguration, LaunchMode launchMode)
            throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        // Http server configuration
        HttpServerOptions httpServerOptions = createHttpServerOptions(httpConfiguration, launchMode);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        server = vertx.createHttpServer(httpServerOptions).requestHandler(router)
                .listen(ar -> {
                    if (ar.succeeded()) {
                        // TODO log proper message
                        Timing.setHttpServer(String.format(
                                "Listening on: http://%s:%s", httpServerOptions.getHost(), httpServerOptions.getPort()));

                    } else {
                        // We can't throw an exception from here as we are on the event loop.
                        // We store the failure in a reference.
                        // The reference will be checked in the main thread, and the failure re-thrown.
                        failure.set(ar.cause());
                    }
                    latch.countDown();
                });

        try {
            latch.await();
            if (failure.get() != null) {
                throw new IllegalStateException("Unable to start the HTTP server", failure.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to start the HTTP server", e);
        }

        CountDownLatch sslLatch = new CountDownLatch(1);
        HttpServerOptions sslConfig = createSslOptions(httpConfiguration, launchMode);
        if (sslConfig != null) {
            sslServer = vertx.createHttpServer(sslConfig).requestHandler(router)
                    .listen(ar -> {
                        if (ar.succeeded()) {
                            // TODO log proper message
                            Timing.setHttpServer(String.format(
                                    "Listening on: https://%s:%s", httpServerOptions.getHost(), httpServerOptions.getPort()));

                        } else {
                            // We can't throw an exception from here as we are on the event loop.
                            // We store the failure in a reference.
                            // The reference will be checked in the main thread, and the failure re-thrown.
                            failure.set(ar.cause());
                        }
                        sslLatch.countDown();
                    });
            try {
                latch.await();
                if (failure.get() != null) {
                    throw new IllegalStateException("Unable to start the HTTP server", failure.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Unable to start the HTTP server", e);
            }
        }
    }

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled
     *
     */
    private static HttpServerOptions createSslOptions(HttpConfiguration httpConfiguration, LaunchMode launchMode)
            throws IOException {
        ServerSslConfig sslConfig = httpConfiguration.ssl;
        //TODO: static fields break config
        Logger log = Logger.getLogger("io.quarkus.configuration.ssl");
        final Optional<Path> certFile = sslConfig.certificate.file;
        final Optional<Path> keyFile = sslConfig.certificate.keyFile;
        final Optional<Path> keyStoreFile = sslConfig.certificate.keyStoreFile;
        final String keystorePassword = sslConfig.certificate.keyStorePassword;
        final HttpServerOptions serverOptions = new HttpServerOptions();
        if (certFile.isPresent() && keyFile.isPresent()) {
            PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                    .setCertPath(certFile.get().toAbsolutePath().toString())
                    .setKeyPath(keyFile.get().toAbsolutePath().toString());
            serverOptions.setPemKeyCertOptions(pemKeyCertOptions);
        } else if (keyStoreFile.isPresent()) {
            final Path keyStorePath = keyStoreFile.get();
            final Optional<String> keyStoreFileType = sslConfig.certificate.keyStoreFileType;
            final String type;
            if (keyStoreFileType.isPresent()) {
                type = keyStoreFileType.get().toLowerCase();
            } else {
                final String pathName = keyStorePath.toString();
                if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
                    type = "pkcs12";
                } else {
                    // assume jks
                    type = "jks";
                }
            }

            //load the data
            byte[] data;
            final InputStream keystoreAsResource = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(keyStorePath.toString());

            if (keystoreAsResource != null) {
                try (InputStream is = keystoreAsResource) {
                    data = doRead(is);
                }
            } else {
                try (InputStream is = Files.newInputStream(keyStorePath)) {
                    data = doRead(is);
                }
            }

            switch (type) {
                case "pkcs12": {
                    PfxOptions options = new PfxOptions()
                            .setPassword(keystorePassword)
                            .setValue(Buffer.buffer(data));
                    serverOptions.setPfxKeyCertOptions(options);
                    break;
                }
                case "jks": {
                    JksOptions options = new JksOptions()
                            .setPassword(keystorePassword)
                            .setValue(Buffer.buffer(data));
                    serverOptions.setKeyStoreOptions(options);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown keystore type: " + type + " valid types are jks or pkcs12");
            }

        } else {
            return null;
        }

        for (String i : sslConfig.cipherSuites) {
            if (!i.isEmpty()) {
                serverOptions.addEnabledCipherSuite(i);
            }
        }

        for (String i : sslConfig.protocols) {
            if (!i.isEmpty()) {
                serverOptions.addEnabledSecureTransportProtocol(i);
            }
        }
        serverOptions.setSsl(true);
        serverOptions.setHost(httpConfiguration.host);
        serverOptions.setPort(httpConfiguration.determineSslPort(launchMode));
        return serverOptions;
    }

    private static byte[] doRead(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    private static HttpServerOptions createHttpServerOptions(HttpConfiguration httpConfiguration, LaunchMode launchMode) {
        // TODO other config properties
        HttpServerOptions options = new HttpServerOptions();
        options.setHost(httpConfiguration.host);
        options.setPort(httpConfiguration.determinePort(launchMode));
        return options;
    }

    private io.vertx.ext.web.Route addRoute(Router router, Handler<RoutingContext> handler, Route routeAnnotation,
            List<Handler<RoutingContext>> filters) {
        io.vertx.ext.web.Route route;
        if (!routeAnnotation.regex().isEmpty()) {
            route = router.routeWithRegex(routeAnnotation.regex());
        } else if (!routeAnnotation.path().isEmpty()) {
            route = router.route(routeAnnotation.path());
        } else {
            route = router.route();
        }
        if (routeAnnotation.methods().length > 0) {
            for (HttpMethod method : routeAnnotation.methods()) {
                route.method(method);
            }
        }
        if (routeAnnotation.order() != Integer.MIN_VALUE) {
            route.order(routeAnnotation.order());
        }
        if (routeAnnotation.produces().length > 0) {
            for (String produces : routeAnnotation.produces()) {
                route.produces(produces);
            }
        }
        if (routeAnnotation.consumes().length > 0) {
            for (String consumes : routeAnnotation.consumes()) {
                route.consumes(consumes);
            }
        }

        for (Handler<RoutingContext> i : filters) {
            if (i != null) {
                route.handler(i);
            }
        }
        route.handler(BodyHandler.create());
        switch (routeAnnotation.type()) {
            case NORMAL:
                route.handler(handler);
                break;
            case BLOCKING:
                // We don't mind if blocking handlers are executed in parallel
                route.blockingHandler(handler, false);
                break;
            case FAILURE:
                route.failureHandler(handler);
                break;
            default:
                throw new IllegalStateException("Unsupported handler type: " + routeAnnotation.type());
        }
        LOGGER.debugf("Route registered for %s", routeAnnotation);
        return route;
    }

    @SuppressWarnings("unchecked")
    private Handler<RoutingContext> createHandler(String handlerClassName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = RouterProducer.class.getClassLoader();
            }
            Class<? extends Handler<RoutingContext>> handlerClazz = (Class<? extends Handler<RoutingContext>>) cl
                    .loadClass(handlerClassName);
            return handlerClazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                | InvocationTargetException e) {
            throw new IllegalStateException("Unable to create invoker: " + handlerClassName, e);
        }
    }

}
