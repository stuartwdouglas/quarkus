package io.quarkus.rest.test.client;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;


import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.client.resource.NullEntityResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Regression for RESTEASY-1057
 */
public class NullEntityTest extends ClientTestBase {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, NullEntityResource.class);
                }
            });

    /**
     * @tpTestDetails Test to send null by post request.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testPostNull() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/null"));
        String response = target.request().post(null, String.class);
        Assert.assertEquals("Wrong response", "", response);
        client.close();
    }

    /**
     * @tpTestDetails Test to send null via entity by post request.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testEntity() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/entity"));
        String response = target.request().post(Entity.entity(null, MediaType.WILDCARD), String.class);
        Assert.assertEquals("Wrong response", "", response);
        client.close();
    }

    /**
     * @tpTestDetails Test to send null via form
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testForm() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/form"));
        String response = target.request().post(Entity.form((Form) null), String.class);
        Assert.assertEquals("Wrong response", null, response);
        client.close();
    }

    /**
     * @tpTestDetails Test resource with "text/html" media type
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testHtml() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/html"));
        String response = target.request().post(Entity.html(null), String.class);
        Assert.assertEquals("Wrong response", "", response);
        client.close();
    }

    /**
     * @tpTestDetails Test resource with "application/xhtml+xml" media type
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testXhtml() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/xhtml"));
        String response = target.request().post(Entity.xhtml(null), String.class);
        Assert.assertEquals("Wrong response", "", response);
        client.close();
    }

    /**
     * @tpTestDetails Test resource with "application/xml" media type
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testXml() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/xml"));
        String response = target.request().post(Entity.xml(null), String.class);
        Assert.assertEquals("Wrong response", "", response);
        client.close();
    }
}
