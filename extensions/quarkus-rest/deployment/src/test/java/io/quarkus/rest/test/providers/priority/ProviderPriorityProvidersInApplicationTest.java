package io.quarkus.rest.test.providers.priority;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityExceptionMapperAAA;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityExceptionMapperBBB;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityExceptionMapperCCC;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFoo;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFooParamConverter;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFooParamConverterProviderAAA;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFooParamConverterProviderBBB;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityFooParamConverterProviderCCC;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityResource;
import io.quarkus.rest.test.providers.priority.resource.ProviderPriorityTestException;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter ExceptionMappers and ParamConverterProviders
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0
 */
@DisplayName("Provider Priority Providers In Application Test")
public class ProviderPriorityProvidersInApplicationTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(ProviderPriorityFoo.class, ProviderPriorityFooParamConverter.class,
                    ProviderPriorityTestException.class);
            List<Class<?>> singletons = new ArrayList<Class<?>>();
            singletons.add(ProviderPriorityExceptionMapperCCC.class);
            singletons.add(ProviderPriorityFooParamConverterProviderCCC.class);
            return TestUtil.finishContainerPrepare(war, null, singletons, ProviderPriorityResource.class,
                    ProviderPriorityExceptionMapperAAA.class, ProviderPriorityExceptionMapperBBB.class,
                    ProviderPriorityExceptionMapperCCC.class, ProviderPriorityFooParamConverterProviderAAA.class,
                    ProviderPriorityFooParamConverterProviderBBB.class, ProviderPriorityFooParamConverterProviderCCC.class);
        }
    });

    private ResteasyProviderFactory factory;

    @BeforeEach
    public void init() {
        factory = ResteasyProviderFactory.newInstance();
        RegisterBuiltin.register(factory);
        ResteasyProviderFactory.setInstance(factory);
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
        // Clear the singleton
        ResteasyProviderFactory.clearInstanceIfEqual(factory);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProviderPriorityProvidersInApplicationTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests that ExceptionMappers are sorted by priority
     * @tpSince RESTEasy 4.0.0
     */
    // @Test
    public void testExceptionMapper() throws Exception {
        WebTarget base = client.target(generateURL("/exception"));
        Response response = base.request().get();
        assertEquals(444, response.getStatus());
        assertEquals(response.readEntity(String.class), "CCC");
    }

    /**
     * @tpTestDetails Tests that ParamConverterProviders are sorted by priority
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    @DisplayName("Test Param Converter Provider")
    public void testParamConverterProvider() throws Exception {
        WebTarget base = client.target(generateURL("/paramconverter/dummy"));
        Response response = base.request().get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "CCC");
    }
}
