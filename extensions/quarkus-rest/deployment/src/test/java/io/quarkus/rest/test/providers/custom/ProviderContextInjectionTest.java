package io.quarkus.rest.test.providers.custom;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.custom.resource.ProviderContextInjectionAnyExceptionExceptionMapper;
import io.quarkus.rest.test.providers.custom.resource.ProviderContextInjectionEnumContextResolver;
import io.quarkus.rest.test.providers.custom.resource.ProviderContextInjectionEnumProvider;
import io.quarkus.rest.test.providers.custom.resource.ProviderContextInjectionIOExceptionExceptionMapper;
import io.quarkus.rest.test.providers.custom.resource.ProviderContextInjectionResource;
import io.quarkus.rest.test.providers.custom.resource.ProviderContextInjectionTextPlainEnumContextResolver;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ProviderContextInjectionTest {

    static Client client;

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ProviderContextInjectionEnumProvider.class);
                    return TestUtil.finishContainerPrepare(war, null, ProviderContextInjectionResource.class,
                            ProviderContextInjectionAnyExceptionExceptionMapper.class,
                            ProviderContextInjectionIOExceptionExceptionMapper.class,
                            ProviderContextInjectionEnumContextResolver.class,
                            ProviderContextInjectionTextPlainEnumContextResolver.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ProviderContextInjectionTest.class.getSimpleName());
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Providers are injected into Resource with @Context injection. The resource gets ContextResolver
     *                provider for user defined enum type EnumProvider and verifies that correct application provider was
     *                chosen.
     * @tpPassCrit Correct application provider was chosen
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void isRegisteredWildCardContextResolverTest() {
        Response response = client.target(generateURL("/resource/isRegisteredContextResolver")).request().get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Providers are injected into Resource with @Context injection. The resource gets ExceptionMapper
     *                provider for RuntimeException and verifies that the correct application provider was chosen.
     * @tpPassCrit Correct application provider was chosen
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testExceptionMapped() {
        Response response = client.target(generateURL("/resource/isRegisteredRuntimeExceptionMapper")).request().get();
        Assert.assertEquals(200, response.getStatus());
        response.close();
    }

}
