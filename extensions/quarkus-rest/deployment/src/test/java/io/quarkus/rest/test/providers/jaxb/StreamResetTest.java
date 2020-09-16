package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.StreamResetPerson;
import io.quarkus.rest.test.providers.jaxb.resource.StreamResetPlace;
import io.quarkus.rest.test.providers.jaxb.resource.StreamResetResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class StreamResetTest {

    private final Logger logger = Logger.getLogger(StreamResetTest.class);

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(StreamResetTest.class);
                    return TestUtil.finishContainerPrepare(war, null, StreamResetPlace.class, StreamResetPerson.class,
                            StreamResetResource.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, StreamResetTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Regression test for JBEAP-2138. BufferEntity method is called.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testJBEAP2138() throws Exception {
        WebTarget target = client.target(generateURL("/test"));
        Response response = target.request().get();

        response.bufferEntity();

        try {
            response.readEntity(StreamResetPlace.class);
        } catch (Exception e) {
        }

        response.readEntity(StreamResetPerson.class);
    }

    /**
     * @tpTestDetails Regression test for JBEAP-2138. BufferEntity method is not called.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testJBEAP2138WithoutBufferedEntity() throws Exception {
        try {
            WebTarget target = client.target(generateURL("/test"));
            Response response = target.request().get();

            try {
                response.readEntity(StreamResetPlace.class);
            } catch (Exception e) {
            }

            response.readEntity(StreamResetPerson.class);

            Assert.fail();
        } catch (IllegalStateException e) {
            logger.info("Expected IllegalStateException was thrown");
        }
    }

}
