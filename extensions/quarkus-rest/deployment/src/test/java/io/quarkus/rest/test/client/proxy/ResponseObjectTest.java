package io.quarkus.rest.test.client.proxy;

import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.proxy.resource.ResponseObjectBasicObjectIntf;
import io.quarkus.rest.test.client.proxy.resource.ResponseObjectClientIntf;
import io.quarkus.rest.test.client.proxy.resource.ResponseObjectHateoasObject;
import io.quarkus.rest.test.client.proxy.resource.ResponseObjectResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ResponseObjectTest {

    protected static final Logger logger = LogManager.getLogger(ResponseObjectTest.class.getName());
    static QuarkusRestClient client;
    ResponseObjectClientIntf responseObjectClientIntf;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, ResponseObjectResource.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        responseObjectClientIntf = ProxyBuilder.builder(ResponseObjectClientIntf.class, client.target(generateURL(""))).build();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResponseObjectTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests ResponseObject annotation on a client interface, invoking the request with ProxyBuilder instance
     * @tpPassCrit The response contains the expected header
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSimpleProxyBuilder() {
        ResponseObjectBasicObjectIntf obj = responseObjectClientIntf.get();
        Assert.assertEquals(Status.OK, obj.status());
        Assert.assertEquals("The response object doesn't contain the expected string", "ABC", obj.body());
        try {
            Assert.assertEquals("The response object doesn't contain the expected header",
                    "text/plain;charset=UTF-8", obj.response().getHeaders().getFirst("Content-Type"));
        } catch (ClassCastException ex) {
            Assert.fail(TestUtil.getErrorMessageForKnownIssue("JBEAP-2446"));
        }
        Assert.assertEquals("The response object doesn't contain the expected header", "text/plain;charset=UTF-8",
                obj.contentType());
    }

    /**
     * @tpTestDetails Tests ResponseObject annotation on a client interface, and resource containg a Link object,
     *                forwarding to another resource and invoking the request with ProxyBuilder instance
     * @tpPassCrit The request was forwarded to another resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testLinkFollowProxyBuilder() {
        ResponseObjectHateoasObject obj = responseObjectClientIntf.performGetBasedOnHeader();
        Assert.assertEquals(Status.NO_CONTENT, obj.status());
        Assert.assertTrue("The resource was not forwarded", obj.nextLink().getPath().endsWith("next-link"));
        try {
            Assert.assertEquals("The resource was not forwarded", "forwarded", obj.followNextLink());
        } catch (ProcessingException ex) {
            Assert.fail(TestUtil.getErrorMessageForKnownIssue("JBEAP-2446"));
        }
    }
}
