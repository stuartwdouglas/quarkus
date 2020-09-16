package io.quarkus.rest.test.client;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.client.resource.ClientResponseRedirectIntf;
import io.quarkus.rest.test.client.resource.ClientResponseRedirectResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ClientResponseRedirectTest extends ClientTestBase {

    protected static final Logger logger = Logger.getLogger(ClientResponseRedirectTest.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ClientTestBase.class);
                    // Use of PortProviderutil in the deployment

                    return TestUtil.finishContainerPrepare(war, null, ClientResponseRedirectResource.class,
                            PortProviderUtil.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Tests redirection of the request using ProxyBuilder client
     * @tpPassCrit The header 'location' contains the redirected target
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRedirectProxyBuilder() throws Exception {
        testRedirect(ProxyBuilder.builder(ClientResponseRedirectIntf.class, client.target(generateURL(""))).build().get());
    }

    /**
     * @tpTestDetails Tests redirection of the request using Client Webtarget request
     * @tpPassCrit The header 'location' contains the redirected target
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRedirectClientTargetRequest() throws Exception {
        testRedirect(client.target(generateURL("/redirect")).request().get());
    }

    /**
     * @tpTestDetails Tests redirection of the request using HttpUrlConnection
     * @tpPassCrit The header 'location' contains the redirected target
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRedirectHttpUrlConnection() throws Exception {
        URL url = PortProviderUtil.createURL("/redirect", ClientResponseRedirectTest.class.getSimpleName());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("GET");
        //        for (Object name : conn.getHeaderFields().keySet()) {
        //            logger.debug(name);
        //        }
        Assert.assertEquals("The response from the server was: " + conn.getResponseCode(),
                Status.SEE_OTHER, conn.getResponseCode());
    }

    @SuppressWarnings(value = "unchecked")
    private void testRedirect(Response response) {
        MultivaluedMap<String, Object> headers = response.getHeaders();
        //        logger.debug("size: " + headers.size());
        //        for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
        //            logger.debug(entry.getKey() + ":" + entry.getValue().get(0));
        //        }
        Assert.assertTrue(headers.getFirst("location").toString().equalsIgnoreCase(
                PortProviderUtil.generateURL("/redirect/data", ClientResponseRedirectTest.class.getSimpleName())));
    }

}
