package io.quarkus.rest.test.xxe;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.xxe.resource.XXEBasicResource;

/**
 * @tpSubChapter XXE
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-637
 *                    Basic XXE test.
 * @tpSince RESTEasy 3.0.16
 */
public class XXEBasicTest {

    static String request;
    static {
        String filename = TestUtil.getResourcePath(XXEBasicTest.class, "testpasswd.txt");
        request = new StringBuilder()
                .append("<?xml version=\"1.0\"?>\r")
                .append("<!DOCTYPE foo\r")
                .append("[<!ENTITY xxe SYSTEM \"").append(filename).append("\">\r")
                .append("]>\r")
                .append("<search><user>&xxe;</user></search>").toString();
    }

    static QuarkusRestClient client;
    protected final Logger logger = LogManager.getLogger(XXEBasicTest.class.getName());

    public static Archive<?> deploy(String expandEntityReferences) {
        WebArchive war = TestUtil.prepareArchive(expandEntityReferences);
        Map<String, String> contextParam = new HashMap<>();
        contextParam.put(ResteasyContextParameters.RESTEASY_EXPAND_ENTITY_REFERENCES, expandEntityReferences);
        contextParam.put(ResteasyContextParameters.RESTEASY_DISABLE_DTDS, "false");
        return TestUtil.finishContainerPrepare(war, contextParam, XXEBasicResource.class);
    }

    @Deployment(name = "true")
    public static Archive<?> deployDefault() {
        return deploy("true");
    }

    @Deployment(name = "false")
    public static Archive<?> deployOne() {
        return deploy("false");
    }

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
        client = null;
    }

    /**
     * @tpTestDetails "resteasy.document.secure.disableDTDs" is set to false
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testXXEWithoutExpansion() throws Exception {
        logger.info(String.format("Request body: %s", request));

        Response response = client.target(PortProviderUtil.generateURL("/", "false")).request()
                .post(Entity.entity(request, "application/xml"));

        Assert.assertEquals(Status.NO_CONTENT, response.getStatus());
        String entity = response.readEntity(String.class);
        Assert.assertEquals(entity, null);
        response.close();
    }

    /**
     * @tpTestDetails "resteasy.document.secure.disableDTDs" is set to true
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testXXEWithExpansion() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/", "true")).request()
                .post(Entity.entity(request, "application/xml"));
        Assert.assertEquals(Status.OK, response.getStatus());
        String entity = response.readEntity(String.class);
        Assert.assertEquals("xx:xx:xx:xx:xx:xx:xx", entity);
        response.close();
    }
}
