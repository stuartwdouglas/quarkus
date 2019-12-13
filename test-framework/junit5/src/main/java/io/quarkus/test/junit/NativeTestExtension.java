package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;

import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;

public class NativeTestExtension
        implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback {

    private static boolean failedBoot;

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (!failedBoot) {
            RestAssuredURLManager.clearURL();
            TestScopeManager.tearDown(true);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (!failedBoot) {
            RestAssuredURLManager.setURL(false);
            TestScopeManager.setup(true);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        ExtensionContext root = extensionContext.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        ExtensionState state = store.get(ExtensionState.class.getName(), ExtensionState.class);
        PropertyTestUtil.setLogFileProperty();
        if (state == null) {
            TestResourceManager testResourceManager = new TestResourceManager(extensionContext.getRequiredTestClass());
            try {
                Map<String, String> systemProps = testResourceManager.start();
                NativeImageLauncher launcher = new NativeImageLauncher(extensionContext.getRequiredTestClass());
                launcher.addSystemProperties(systemProps);
                try {
                    launcher.start();
                } catch (IOException e) {
                    try {
                        launcher.close();
                    } catch (Throwable t) {
                    }
                    throw e;
                }
                state = new ExtensionState(testResourceManager, launcher, true);
                store.put(ExtensionState.class.getName(), state);
            } catch (Exception e) {

                failedBoot = true;
                throw new JUnitException("Quarkus native image start failed, original cause: " + e);
            }
        }
    }

    public class ExtensionState implements ExtensionContext.Store.CloseableResource {

        private final TestResourceManager testResourceManager;
        private final Closeable resource;

        ExtensionState(TestResourceManager testResourceManager, Closeable resource, boolean nativeImage) {
            this.testResourceManager = testResourceManager;
            this.resource = resource;
        }

        @Override
        public void close() throws Throwable {
            testResourceManager.stop();
            resource.close();
        }
    }
}
