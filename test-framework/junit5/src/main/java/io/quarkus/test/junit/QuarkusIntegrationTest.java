package io.quarkus.test.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation that indicates that this test should be run the result of the Quarkus build.
 * That means that if a jar was created, that jar is launched using {@code java -jar ...}
 * (and thus runs in a separate JVM than the test).
 * If instead a native image was created, the that image is launched.
 * Finally, if a container image was created during the build, then a new container is created and run.
 *
 * The standard usage pattern is expected to be a base test class that runs the
 * tests using the JVM version of Quarkus, with a subclass that extends the base
 * test and is annotated with this annotation to perform the same checks against
 * the native image.
 *
 * Note that it is not possible to mix {@code @QuarkusTest} and {@code QuarkusIntegrationTest} in the same test
 * run, it is expected that the {@code @QuarkusTest} tests will be standard unit tests that are
 * executed by surefire, while the {@code QuarkusIntegrationTest} tests will be integration tests
 * executed by failsafe.
 * This also means that injection of beans into a test class using {@code @Inject} is not supported
 * in {@code QuarkusIntegrationTest}. Such injection is only possible in tests injected with
 * {@link @QuarkusTest} so the test class structure must take this into account.
 */
@Target(ElementType.TYPE)
@ExtendWith({ DisabledOnIntegrationTestCondition.class, QuarkusTestExtension.class, QuarkusIntegrationTestExtension.class })
@Retention(RetentionPolicy.RUNTIME)
public @interface QuarkusIntegrationTest {

    /**
     * If quarkus should automatically start the application. If this is set to false then you need to use
     * @return
     */
    boolean start() default true;

    /**
     * If used as a field of class annotated with {@link QuarkusIntegrationTest}, the field is populated
     * with an implementation that allows accessing contextual test information
     */
    interface Context {

        /**
         * Returns a map containing all the properties creates by potentially launched dev services.
         * If no dev services where launched, the map will be empty.
         */
        Map<String, String> devServicesProperties();

        /**
         * Returns a launcher that can be used to launch the application. This can only be used if {@link #start()} is false.
         *
         * @return A new launch builder
         */
        LaunchBuilder newBuilder();
    }

    interface LaunchBuilder {

        LaunchBuilder systemProperty(String name, String value);
        LaunchBuilder jvmArg(String arg);
        LaunchBuilder environmentVariable(String name, String value);

        LaunchResult launchToCompletion(String... args);

    }

    class LaunchResult {

        final int statusCode;
        final String output;

        public LaunchResult(int statusCode, String output) {
            this.statusCode = statusCode;
            this.output = output;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getOutput() {
            return output;
        }
    }
}
