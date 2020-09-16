package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyLocatorResource;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyMiddleResource;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyParamEntityPrototype;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyParamEntityWithConstructor;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyPathParamResource;
import io.quarkus.rest.test.resource.path.resource.LocatorWithClassHierarchyPathSegmentImpl;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Locator With Class Hierarchy Test")
public class LocatorWithClassHierarchyTest {

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(LocatorWithClassHierarchyPathSegmentImpl.class, LocatorWithClassHierarchyMiddleResource.class,
                    LocatorWithClassHierarchyPathParamResource.class, LocatorWithClassHierarchyParamEntityWithConstructor.class,
                    LocatorWithClassHierarchyParamEntityPrototype.class);
            return TestUtil.finishContainerPrepare(war, null, LocatorWithClassHierarchyLocatorResource.class);
        }
    });

    /**
     * @tpTestDetails Client sends POST request with null entity for the resource Locator, which creates the targeted
     *                resource object.
     * @tpPassCrit Correct response is returned from the server
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Locator With Sub With Path Annotation")
    public void testLocatorWithSubWithPathAnnotation() {
        Response response = client.target(
                PortProviderUtil.generateURL("/resource/locator/ParamEntityWithConstructor/ParamEntityWithConstructor=JAXRS",
                        LocatorWithClassHierarchyTest.class.getSimpleName()))
                .request().post(null);
        Assertions.assertEquals(200, response.getStatus());
        response.close();
    }
}
