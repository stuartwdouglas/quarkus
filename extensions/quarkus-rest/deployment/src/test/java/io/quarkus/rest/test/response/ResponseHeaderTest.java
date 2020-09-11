package io.quarkus.rest.test.response;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.response.resource.ResponseHeaderExceptionMapper;
import io.quarkus.rest.test.response.resource.ResponseHeaderExceptionMapperRuntimeException;
import io.quarkus.rest.test.response.resource.ResponseHeaderResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check that HEADS can replace existing text with new specified text
 * @tpSince RESTEasy 3.0.23
 */
public class ResponseHeaderTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ResponseHeaderExceptionMapperRuntimeException.class);
                    return TestUtil.finishContainerPrepare(war, null,
                            ResponseHeaderExceptionMapper.class,
                            ResponseHeaderResource.class);
                }
            });

    /**
     * @tpTestDetails Check the response headers contain the changes made via the
     *                from custom exception mapper. Using Resteasy client.
     * @tpSince RESTEasy 3.0.23
     */
    @Test
    public void testMapperWithQuarkusRestClient() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        WebTarget base = client.target(PortProviderUtil.generateURL("/test",
                ResponseHeaderTest.class.getSimpleName()));
        Response response = base.request().get();
        MultivaluedMap<String, Object> headers = response.getHeaders();
        List<Object> objs = headers.get("Server");

        if (objs instanceof ArrayList) {
            if (objs.size() != 2) {
                Assert.fail("2 array objects expected " + objs.size() + " were returned");
            }

            Assert.assertEquals("Wrong headers",
                    (String) objs.get(0) + "," + (String) objs.get(1),
                    "WILDFLY/TEN.Full,AndOtherStuff");
        } else {
            Assert.fail("Expected header data value to be of type ArrayList.  It was not.");
        }

        response.close();
        client.close();
    }
}
