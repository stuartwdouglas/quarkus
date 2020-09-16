package io.quarkus.rest.test.providers.jaxb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.plugins.stats.DeleteResourceMethod;
import org.jboss.resteasy.plugins.stats.GetResourceMethod;
import org.jboss.resteasy.plugins.stats.HeadResourceMethod;
import org.jboss.resteasy.plugins.stats.OptionsResourceMethod;
import org.jboss.resteasy.plugins.stats.PostResourceMethod;
import org.jboss.resteasy.plugins.stats.PutResourceMethod;
import org.jboss.resteasy.plugins.stats.RegistryData;
import org.jboss.resteasy.plugins.stats.RegistryEntry;
import org.jboss.resteasy.plugins.stats.RegistryStatsResource;
import org.jboss.resteasy.plugins.stats.ResourceMethodEntry;
import org.jboss.resteasy.plugins.stats.SubresourceLocator;
import org.jboss.resteasy.plugins.stats.TraceResourceMethod;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.StatsProxy;
import io.quarkus.rest.test.providers.jaxb.resource.StatsResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Stats Test")
public class StatsTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(StatsTest.class);
            return TestUtil.finishContainerPrepare(war, null, StatsResource.class, RegistryStatsResource.class,
                    ResourceMethodEntry.class, GetResourceMethod.class, PutResourceMethod.class, DeleteResourceMethod.class,
                    PostResourceMethod.class, OptionsResourceMethod.class, HeadResourceMethod.class, TraceResourceMethod.class,
                    RegistryData.class, RegistryEntry.class, SubresourceLocator.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, StatsTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests resteasy jaxb utility RegistryStatsResource, it is getting information about resources available
     *                to the application
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Registry Stats")
    public void testRegistryStats() throws Exception {
        StatsProxy stats = client.target(generateURL("/")).proxy(StatsProxy.class);
        RegistryData data = stats.get();
        Assertions.assertEquals(4, data.getEntries().size(), "The number of resources doesn't match");
        boolean found = false;
        for (RegistryEntry entry : data.getEntries()) {
            if (entry.getUriTemplate().equals("/entry/{foo:.*}")) {
                Assertions.assertEquals("Some method for resource \"" + entry.getUriTemplate() + "\" is missing ", 2,
                        entry.getMethods().size());
                List<Class> prepareRequiredTypes = prepareRequiredTypes(PostResourceMethod.class, PutResourceMethod.class);
                Assertions.assertTrue(testMethodTypes(entry.getMethods().get(0), prepareRequiredTypes),
                        "Unexpected method type");
                Assertions.assertTrue(testMethodTypes(entry.getMethods().get(1), prepareRequiredTypes),
                        "Unexpected method type");
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found, "Resource not found");
        found = false;
        for (RegistryEntry entry : data.getEntries()) {
            if (entry.getUriTemplate().equals("/resource")) {
                Assertions.assertEquals("Some method for resource \"" + entry.getUriTemplate() + "\" is missing ", 2,
                        entry.getMethods().size());
                List<Class> prepareRequiredTypes = prepareRequiredTypes(HeadResourceMethod.class, DeleteResourceMethod.class);
                Assertions.assertTrue(testMethodTypes(entry.getMethods().get(0), prepareRequiredTypes),
                        "Unexpected method type");
                Assertions.assertTrue(testMethodTypes(entry.getMethods().get(1), prepareRequiredTypes),
                        "Unexpected method type");
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found, "Resource not found");
        found = false;
        for (RegistryEntry entry : data.getEntries()) {
            if (entry.getUriTemplate().equals("/locator")) {
                Assertions.assertNotNull(entry.getLocator());
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found, "Resource not found");
        found = false;
        for (RegistryEntry entry : data.getEntries()) {
            if (entry.getUriTemplate().equals("/resteasy/registry")) {
                Assertions.assertEquals("Some method for resource \"" + entry.getUriTemplate() + "\" is missing ", 1,
                        entry.getMethods().size());
                Assertions.assertTrue(entry.getMethods().get(0) instanceof GetResourceMethod, "Unexpected method type");
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found, "Resource not found");
    }

    private boolean testMethodTypes(ResourceMethodEntry entry, List<Class> types) {
        if (types.contains(entry.getClass())) {
            types.remove(entry.getClass());
            return true;
        } else {
            return false;
        }
    }

    private List<Class> prepareRequiredTypes(Class... types) {
        ArrayList<Class> list = new ArrayList<Class>();
        for (Class type : types) {
            list.add(type);
        }
        return list;
    }
}
