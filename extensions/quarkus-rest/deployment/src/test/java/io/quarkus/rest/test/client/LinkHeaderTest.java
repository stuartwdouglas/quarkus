package io.quarkus.rest.test.client;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.resource.LinkHeaderService;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for new Link provided by jax-rs 2.0 spec
 * @tpSince RESTEasy 3.0.16
 */
public class LinkHeaderTest extends ClientTestBase {

    protected static QuarkusRestClient client;

    @BeforeEach
    public void setup() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, LinkHeaderService.class);
                }
            });

    @AfterEach
    public void shutdown() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Test new client without API
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testLinkheader() throws Exception {

        // new client testing
        {
            Response response = client.target(generateURL("/linkheader/str")).request().header("Link",
                    "<http://example.com/TheBook/chapter2>; rel=\"previous\"; title=\"previous chapter\"")
                    .post(Entity.text(new String()));
            javax.ws.rs.core.Link link = response.getLink("previous");
            Assert.assertNotNull(link);
            Assert.assertEquals("Wrong link", "previous chapter", link.getTitle());
            Assert.assertEquals("Wrong link", "http://example.com/TheBook/chapter2", link.getUri().toString());
        }
    }
}
