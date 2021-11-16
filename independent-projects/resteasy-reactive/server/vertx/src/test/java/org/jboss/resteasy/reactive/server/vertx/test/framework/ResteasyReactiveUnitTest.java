package org.jboss.resteasy.reactive.server.vertx.test.framework;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.common.processor.JandexUtil;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.reflection.ReflectiveContextInjectedBeanFactory;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.processor.scanning.AsyncReturnTypeScanner;
import org.jboss.resteasy.reactive.server.spi.DefaultRuntimeConfiguration;
import org.jboss.resteasy.reactive.server.vertx.BlockingInputHandler;
import org.jboss.resteasy.reactive.server.vertx.ResteasyReactiveVertxHandler;
import org.jboss.resteasy.reactive.server.vertx.VertxRequestContextFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ResteasyReactiveUnitTest implements BeforeAllCallback, AfterAllCallback {

    private static final Logger rootLogger;
    private Handler[] originalHandlers;

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        rootLogger = LogManager.getLogManager().getLogger("");
        BlockingOperationSupport.setIoThreadDetector(new BlockingOperationSupport.IOThreadDetector() {
            @Override
            public boolean isBlockingAllowed() {
                return !Context.isOnEventLoopThread();
            }
        });
    }

    private Path deploymentDir;
    private Consumer<Throwable> assertException;
    private Supplier<JavaArchive> archiveProducer;

    private Consumer<List<LogRecord>> assertLogRecords;

    private Timer timeoutTimer;
    private volatile TimerTask timeoutTask;
    private InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler((r) -> false);

    static Vertx vertx;
    static HttpServer httpServer;
    static Router router;
    static Route route;
    static Executor executor = Executors.newFixedThreadPool(10);

    List<Closeable> closeTasks = new ArrayList<>();

    public ResteasyReactiveUnitTest setExpectedException(Class<? extends Throwable> expectedException) {
        return assertException(t -> {
            Throwable i = t;
            boolean found = false;
            while (i != null) {
                if (i.getClass().getName().equals(expectedException.getName())) {
                    found = true;
                    break;
                }
                i = i.getCause();
            }

            assertTrue(found, "Build failed with wrong exception, expected " + expectedException + " but got " + t);
        });
    }

    public ResteasyReactiveUnitTest() {
    }

    public ResteasyReactiveUnitTest assertException(Consumer<Throwable> assertException) {
        if (this.assertException != null) {
            throw new IllegalStateException("Don't set the asserted or excepted exception twice"
                    + " to avoid shadowing out the first call.");
        }
        this.assertException = assertException;
        return this;
    }

    public Supplier<JavaArchive> getArchiveProducer() {
        return archiveProducer;
    }

    public ResteasyReactiveUnitTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        Objects.requireNonNull(archiveProducer);
        this.archiveProducer = archiveProducer;
        return this;
    }

    public ResteasyReactiveUnitTest assertLogRecords(Consumer<List<LogRecord>> assertLogRecords) {
        if (this.assertLogRecords != null) {
            throw new IllegalStateException("Don't set the a log record assertion twice"
                    + " to avoid shadowing out the first call.");
        }
        this.assertLogRecords = assertLogRecords;
        return this;
    }

    private void exportArchive(Path deploymentDir, Class<?> testClass) {
        try {
            JavaArchive archive = getArchiveProducerOrDefault();
            Class<?> c = testClass;
            archive.addClasses(c.getClasses());
            while (c != Object.class) {
                archive.addClass(c);
                c = c.getSuperclass();
            }
            archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.toFile());

        } catch (Exception e) {
            throw new RuntimeException("Unable to create the archive", e);
        }
    }

    private JavaArchive getArchiveProducerOrDefault() {
        if (archiveProducer == null) {
            return ShrinkWrap.create(JavaArchive.class);
        } else {
            return archiveProducer.get();
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        originalHandlers = rootLogger.getHandlers();

        timeoutTask = new TimerTask() {
            @Override
            public void run() {
                System.err.println("Test has been running for more than 5 minutes, thread dump is:");
                for (Map.Entry<Thread, StackTraceElement[]> i : Thread.getAllStackTraces().entrySet()) {
                    System.err.println("\n");
                    System.err.println(i.toString());
                    System.err.println("\n");
                    for (StackTraceElement j : i.getValue()) {
                        System.err.println(j);
                    }
                }
            }
        };
        timeoutTimer = new Timer("Test thread dump timer");
        timeoutTimer.schedule(timeoutTask, 1000 * 60 * 5);
        ExtensionContext.Store store = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);

        Class<?> testClass = extensionContext.getRequiredTestClass();

        deploymentDir = Files.createTempDirectory("quarkus-unit-test");

        exportArchive(deploymentDir, testClass);

        if (vertx == null) {
            vertx = Vertx.vertx();
            HttpServer server = vertx.createHttpServer();
            router = Router.router(vertx);
            server.requestHandler(router).listen(8080).toCompletionStage().toCompletableFuture().get();
            store.put(ResteasyReactiveUnitTest.class.getName(), new ExtensionContext.Store.CloseableResource() {
                @Override
                public void close() throws Throwable {
                    server.close();
                    vertx.close();
                }
            });
        }

        IndexView nonCalcIndex = JandexUtil.createIndex(deploymentDir);
        ResteasyReactiveDeploymentManager.ScanStep scanStep = ResteasyReactiveDeploymentManager.start(nonCalcIndex);
        scanStep.addMethodScanner(new AsyncReturnTypeScanner());
        ResteasyReactiveDeploymentManager.ScannedApplication scanned = scanStep.scan();
        ResteasyReactiveDeploymentManager.PreparedApplication prepared = scanned
                .prepare(Thread.currentThread().getContextClassLoader(), ReflectiveContextInjectedBeanFactory.STRING_FACTORY);
        prepared.addScannedSerializers();
        prepared.addBuiltinSerializers();
        DefaultRuntimeConfiguration runtimeConfiguration = new DefaultRuntimeConfiguration(Duration.ofMinutes(1), true,
                System.getProperty("java.io.tmpdir"), StandardCharsets.UTF_8, Optional.empty(), 100000);
        ResteasyReactiveDeploymentManager.RunnableApplication application = prepared.createApplication(runtimeConfiguration,
                new VertxRequestContextFactory(), executor, BlockingInputHandler::new);

        ResteasyReactiveVertxHandler handler = new ResteasyReactiveVertxHandler(application.getInitialHandler());
        String path = application.getPath();
        router.route(path).handler(handler);
        if (path.endsWith("/")) {
            router.route(path + "*").handler(handler);
        } else {
            router.route(path + "/*").handler(handler);
        }

    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (assertLogRecords != null) {
            assertLogRecords.accept(inMemoryLogHandler.records);
        }
        //rootLogger.setHandlers(originalHandlers);
        inMemoryLogHandler.clearRecords();

        System.clearProperty("test.url");
        timeoutTask.cancel();
        timeoutTask = null;
        timeoutTimer = null;
        if (deploymentDir != null) {
            deleteDirectory(deploymentDir);
        }
        router.clear();

    }

    public static void deleteDirectory(final Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    // ignored
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    // ignored
                }
                return FileVisitResult.CONTINUE;
            }

        });
    }

}
