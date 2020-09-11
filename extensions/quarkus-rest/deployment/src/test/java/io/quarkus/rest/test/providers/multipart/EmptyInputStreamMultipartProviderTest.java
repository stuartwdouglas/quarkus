package io.quarkus.rest.test.providers.multipart;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.multipart.resource.EmptyInputStreamMultipartProviderMyBean;
import io.quarkus.rest.test.providers.multipart.resource.EmptyInputStreamMultipartProviderResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-204.
 *                    POJO with empty InputStream field returned as "mutlipart/form-data" produces no headers in multipart
 * @tpSince RESTEasy 3.0.16
 */
public class EmptyInputStreamMultipartProviderTest {

    protected final Logger logger = Logger.getLogger(EmptyInputStreamMultipartProviderTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, EmptyInputStreamMultipartProviderResource.class,
                            EmptyInputStreamMultipartProviderMyBean.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, EmptyInputStreamMultipartProviderTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Resource returning POJO with empty InputStream field, the response is checked to contain the header
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void test() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/rest/zba"));
        Response response = target.request().get();
        Assert.assertEquals(Status.OK, response.getStatus());
        String string = response.readEntity(String.class);
        logger.info(string);
        Assert.assertTrue("The response doesn't contain the expected header", string.indexOf("Content-Length") > -1);
        client.close();
    }

}
