package io.quarkus.rest.test.providers.atom;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.providers.atom.Entry;
import org.jboss.resteasy.plugins.providers.atom.Feed;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.atom.resource.JAXBContextFinderAtomServer;
import io.quarkus.rest.test.providers.atom.resource.JAXBContextFinderCustomerAtom;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Atom provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test integration of atom provider and JAXB Context finder
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Jaxb Context Finder Test")
public class JAXBContextFinderTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(JAXBContextFinderCustomerAtom.class);
            return TestUtil.finishContainerPrepare(war, null, JAXBContextFinderAtomServer.class);
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
        return PortProviderUtil.generateURL(path, JAXBContextFinderTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test new client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Atom Feed New Client")
    public void testAtomFeedNewClient() throws Exception {
        Response response = client.target(generateURL("/atom/feed")).request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Feed feed = response.readEntity(Feed.class);
        Iterator<Entry> it = feed.getEntries().iterator();
        Entry entry1 = it.next();
        Entry entry2 = it.next();
        Field field = Entry.class.getDeclaredField("finder");
        field.setAccessible(true);
        Assertions.assertNotNull(field.get(entry1), "First feet is not correct");
        Assertions.assertEquals(field.get(entry1), field.get(entry2), "Second feet is not correct");
        response.close();
    }
}
