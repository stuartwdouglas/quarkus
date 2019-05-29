package io.quarkus.netty.runtime;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLContext;

import io.quarkus.router.RouterFilter;
import io.quarkus.runtime.ExecutorTemplate;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.annotations.Template;
import io.quarkus.runtime.configuration.ConfigInstantiator;

@Template
public class NettyHTTPTemplate {
    private static final List<RouterFilter> hotDeploymentWrappers = new CopyOnWriteArrayList<>();

    public static void startServerAfterFailedStart() {
        try {
            HttpConfig config = new HttpConfig();
            ConfigInstantiator.handleObject(config);

            ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig();
            ConfigInstantiator.handleObject(threadPoolConfig);

            ExecutorService service = ExecutorTemplate.createDevModeExecutorForFailedStart(threadPoolConfig);
            //we can't really do
            doServerStart(config, LaunchMode.DEVELOPMENT, config.ssl.toSSLContext(), service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void addHotDeploymentWrapper(HandlerWrapper handlerWrapper) {
        hotDeploymentWrappers.add(handlerWrapper);
    }

    /**
     * Used for quarkus:run, where we want undertow to start very early in the process.
     * <p>
     * This enables recovery from errors on boot. In a normal boot undertow is one of the last things start, so there would
     * be no chance to use hot deployment to fix the error. In development mode we start Undertow early, so any error
     * on boot can be corrected via the hot deployment handler
     */
    private static void doServerStart(HttpConfig config, LaunchMode launchMode, SSLContext sslContext, ExecutorService executor) {
        if (undertow == null) {
            int port = config.determinePort(launchMode);
            int sslPort = config.determineSslPort(launchMode);
            log.debugf("Starting Undertow on port %d", port);
            HttpHandler rootHandler = new CanonicalPathHandler(ROOT_HANDLER);
            for (HandlerWrapper i : hotDeploymentWrappers) {
                rootHandler = i.wrap(rootHandler);
            }

            Undertow.Builder builder = Undertow.builder()
                    .addHttpListener(port, config.host)
                    .setWorker(executor)
                    .setHandler(rootHandler);
            if (config.ioThreads.isPresent()) {
                builder.setIoThreads(config.ioThreads.getAsInt());
            } else if (launchMode.isDevOrTest()) {
                //we limit the number of IO and worker threads in development and testing mode
                builder.setIoThreads(2);
            } else {
                builder.setIoThreads(Runtime.getRuntime().availableProcessors() * 2);
            }
            if (sslContext != null) {
                log.debugf("Starting Undertow HTTPS listener on port %d", sslPort);
                builder.addHttpsListener(sslPort, config.host, sslContext);
            }
            undertow = builder
                    .build();
            undertow.start();
        }
    }

}
