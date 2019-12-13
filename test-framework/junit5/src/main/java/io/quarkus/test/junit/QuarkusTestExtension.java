package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.deployment.proxy.ProxyConfiguration;
import io.quarkus.deployment.proxy.ProxyFactory;
import io.quarkus.runner.bootstrap.AugmentAction;
import io.quarkus.runner.bootstrap.RunningQuarkusApplication;
import io.quarkus.test.common.DefineClassVisibleClassLoader;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.common.http.TestHTTPResourceManager;

//todo: share common core with QuarkusUnitTest
public class QuarkusTestExtension
        implements BeforeEachCallback, AfterEachCallback, TestInstanceFactory, BeforeAllCallback, InvocationInterceptor {

    private static boolean failedBoot;

    private static Class<?> actualTestClass;
    private static Object actualTestInstance;
    private static ClassLoader originalCl;
    private RunningQuarkusApplication runningQuarkusApplication;

    private ExtensionState doJavaStart(ExtensionContext context, TestResourceManager testResourceManager) {

        try {
            final LinkedBlockingDeque<Runnable> shutdownTasks = new LinkedBlockingDeque<>();

            Path appClassLocation = getAppClassLocation(context.getRequiredTestClass());

            QuarkusBootstrap.builder(appClassLocation);

            final ClassLoader testClassLoader = context.getRequiredTestClass().getClassLoader();
            final QuarkusBootstrap.Builder runnerBuilder = QuarkusBootstrap.builder(appClassLocation);

            final Path testClassLocation = getTestClassesLocation(context.getRequiredTestClass());

            if (!appClassLocation.equals(testClassLocation)) {
                runnerBuilder.addAdditionalApplicationArchive(new AdditionalDependency(testClassLocation, false, true));
            }
            CuratedApplication curatedApplication = runnerBuilder.setTest(true).build().bootstrap();
            AugmentAction augmentAction = new AugmentAction(curatedApplication,
                    Collections.singletonList(new Consumer<BuildChainBuilder>() {
                        @Override
                        public void accept(BuildChainBuilder buildChainBuilder) {
                            buildChainBuilder.addBuildStep(new BuildStep() {
                                @Override
                                public void execute(BuildContext context) {
                                    context.produce(new TestClassPredicateBuildItem(new Predicate<String>() {
                                        @Override
                                        public boolean test(String className) {
                                            return PathTestHelper.isTestClass(className, testClassLoader);
                                        }
                                    }));
                                }
                            }).produces(TestClassPredicateBuildItem.class)
                                    .build();

                            buildChainBuilder.addBuildStep(new BuildStep() {
                                @Override
                                public void execute(BuildContext context) {
                                    context.produce(new TestAnnotationBuildItem(QuarkusTest.class.getName()));
                                }
                            }).produces(TestAnnotationBuildItem.class)
                                    .build();
                        }
                    }));
            runningQuarkusApplication = augmentAction.createInitialRuntimeApplication().run();

            System.setProperty("test.url", TestHTTPResourceManager.getUri(runningQuarkusApplication));

            Closeable shutdownTask = new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        runningQuarkusApplication.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        while (!shutdownTasks.isEmpty()) {
                            shutdownTasks.pop().run();
                        }
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        shutdownTask.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, "Quarkus Test Cleanup Shutdown task"));
            return new ExtensionState(testResourceManager, shutdownTask, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (isNativeTest(context)) {
            return;
        }
        if (!failedBoot) {
            boolean nativeImageTest = context.getRequiredTestClass().isAnnotationPresent(SubstrateTest.class)
                    || isNativeTest(context);
            RestAssuredURLManager.clearURL();
            TestScopeManager.tearDown(nativeImageTest);
        }
    }

    private boolean isNativeTest(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(NativeImageTest.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (isNativeTest(context)) {
            return;
        }
        if (!failedBoot) {
            boolean nativeImageTest = context.getRequiredTestClass().isAnnotationPresent(SubstrateTest.class)
                    || isNativeTest(context);
            RestAssuredURLManager.setURL(false);
            TestScopeManager.setup(nativeImageTest);
        }
    }

    @Override
    public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
            throws TestInstantiationException {
        if (isNativeTest(extensionContext)) {
            try {
                return extensionContext.getRequiredTestClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new TestInstantiationException("Failed to create test", e);
            }
        }
        if (failedBoot) {
            try {
                return extensionContext.getRequiredTestClass().newInstance();
            } catch (Exception e) {
                throw new TestInstantiationException("Boot failed", e);
            }
        }
        ExtensionContext root = extensionContext.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        ExtensionState state = store.get(ExtensionState.class.getName(), ExtensionState.class);
        if (state == null) {
            PropertyTestUtil.setLogFileProperty();
            TestResourceManager testResourceManager = new TestResourceManager(extensionContext.getRequiredTestClass());
            try {
                testResourceManager.start();
                state = doJavaStart(extensionContext, testResourceManager);
                store.put(ExtensionState.class.getName(), state);

            } catch (Throwable e) {
                try {
                    testResourceManager.stop();
                } catch (Exception ex) {
                    e.addSuppressed(ex);
                }
                failedBoot = true;
                throw e;
            }
        }

        // non-static inner classes are not supported
        Class<?> testClass = factoryContext.getTestClass();
        if (testClass.getEnclosingClass() != null && !Modifier.isStatic(testClass.getModifiers())) {
            throw new IllegalStateException("Test class " + testClass + " cannot be a non-static inner class.");
        }
        try {
            actualTestClass = Class.forName(testClass.getName(), true,
                    Thread.currentThread().getContextClassLoader());
            Class<?> cdi = Thread.currentThread().getContextClassLoader().loadClass("javax.enterprise.inject.spi.CDI");
            Object instance = cdi.getMethod("current").invoke(null);
            Method selectMethod = cdi.getMethod("select", Class.class, Annotation[].class);
            Object cdiInstance = selectMethod.invoke(instance, actualTestClass, new Annotation[0]);
            actualTestInstance = selectMethod.getReturnType().getMethod("get").invoke(cdiInstance);
        } catch (Exception e) {
            throw new TestInstantiationException("Failed to create test instance", e);
        }
        ProxyFactory<?> factory = new ProxyFactory<>(new ProxyConfiguration<>()
                .setAnchorClass(testClass)
                .setProxyNameSuffix("$$QuarkusUnitTestProxy")
                .setClassLoader(new DefineClassVisibleClassLoader(testClass.getClassLoader()))
                .setSuperClass((Class<Object>) testClass));

        //verify that we can proxy the relevant methods
        Class c = testClass;
        while (c != Object.class) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.getAnnotation(Test.class) != null) {
                    if (Modifier.isFinal(method.getModifiers())) {
                        throw new RuntimeException("Test method " + method + " cannot be final");
                    }
                    if (!Modifier.isPublic(method.getModifiers()) && !Modifier.isProtected(method.getModifiers())) {
                        throw new RuntimeException("Test method " + method + " must be public or protected");
                    }
                }

            }
            c = c.getSuperclass();
        }

        try {
            return factory.newInstance(new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Method realMethod = actualTestInstance.getClass().getMethod(method.getName(), method.getParameterTypes());
                    return realMethod.invoke(actualTestInstance, args);
                }
            });
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassLoader setCCL(ClassLoader cl) {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(cl);
        return original;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (isNativeTest(context)) {
            return;
        }
        if (failedBoot) {
            throw new TestAbortedException("Not running test as boot failed");
        }
    }

    @Override
    public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext);
        invocation.proceed();
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext);
        invocation.proceed();
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext);
        invocation.proceed();
    }

    @Override
    public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext);
        invocation.proceed();
    }

    private void runExtensionMethod(ReflectiveInvocationContext<Method> invocationContext) {
        Method newMethod = null;
        Class<?> c = actualTestClass;
        while (c != Object.class) {
            try {
                newMethod = c.getDeclaredMethod(invocationContext.getExecutable().getName(),
                        invocationContext.getExecutable().getParameterTypes());
                break;
            } catch (NoSuchMethodException e) {
                //ignore
            }
            c = c.getSuperclass();
        }
        if (newMethod == null) {
            throw new RuntimeException("Could not find method " + invocationContext.getExecutable() + " on test class");
        }
        try {
            newMethod.setAccessible(true);
            newMethod.invoke(actualTestInstance, invocationContext.getArguments().toArray());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    class ExtensionState implements ExtensionContext.Store.CloseableResource {

        private final TestResourceManager testResourceManager;
        private final Closeable resource;

        ExtensionState(TestResourceManager testResourceManager, Closeable resource, boolean nativeImage) {
            this.testResourceManager = testResourceManager;
            this.resource = resource;
        }

        @Override
        public void close() throws Throwable {
            testResourceManager.stop();
            try {
                resource.close();
            } finally {
                if (QuarkusTestExtension.this.originalCl != null) {
                    setCCL(QuarkusTestExtension.this.originalCl);
                }
            }
        }
    }

    static class DeleteRunnable implements Runnable {
        final Path path;

        DeleteRunnable(Path path) {
            this.path = path;
        }

        @Override
        public void run() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
