package io.quarkus.rest.test.xxe;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.xxe.resource.XxeSecureProcessingFavoriteMovieXmlRootElement;
import io.quarkus.rest.test.xxe.resource.XxeSecureProcessingMovieResource;

/**
 * @tpSubChapter XXE
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-869
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Xxe Secure Processing Test")
public class XxeSecureProcessingTest {

    private QuarkusRestClient client;

    public final Logger logger = Logger.getLogger(XxeSecureProcessingTest.class.getName());

    String doctype = "<!DOCTYPE foodocument [" + "<!ENTITY foo 'foo'>"
            + "<!ENTITY foo1 '&foo;&foo;&foo;&foo;&foo;&foo;&foo;&foo;&foo;&foo;'>"
            + "<!ENTITY foo2 '&foo1;&foo1;&foo1;&foo1;&foo1;&foo1;&foo1;&foo1;&foo1;&foo1;'>"
            + "<!ENTITY foo3 '&foo2;&foo2;&foo2;&foo2;&foo2;&foo2;&foo2;&foo2;&foo2;&foo2;'>"
            + "<!ENTITY foo4 '&foo3;&foo3;&foo3;&foo3;&foo3;&foo3;&foo3;&foo3;&foo3;&foo3;'>"
            + "<!ENTITY foo5 '&foo4;&foo4;&foo4;&foo4;&foo4;&foo4;&foo4;&foo4;&foo4;&foo4;'>"
            + "<!ENTITY foo6 '&foo5;&foo5;&foo5;&foo5;&foo5;&foo5;&foo5;&foo5;&foo5;&foo5;'>"
            + "<!ENTITY foo7 '&foo6;&foo6;&foo6;&foo6;&foo6;&foo6;&foo6;&foo6;&foo6;&foo6;'>"
            + "<!ENTITY foo8 '&foo7;&foo7;&foo7;&foo7;&foo7;&foo7;&foo7;&foo7;&foo7;&foo7;'>"
            + "<!ENTITY foo9 '&foo8;&foo8;&foo8;&foo8;&foo8;&foo8;&foo8;&foo8;&foo8;&foo8;'>" + "]>";

    String small = doctype
            + "<xxeSecureProcessingFavoriteMovieXmlRootElement><title>&foo4;</title></xxeSecureProcessingFavoriteMovieXmlRootElement>";

    String big = doctype
            + "<xxeSecureProcessingFavoriteMovieXmlRootElement><title>&foo5;</title></xxeSecureProcessingFavoriteMovieXmlRootElement>";

    private static final String T_DEFAULT = "default";

    private static final String T_TRUE = "true";

    private static final String T_FALSE = "false";

    public static Archive<?> deploy(String expandEntityReferences) {
        WebArchive war = TestUtil.prepareArchive(expandEntityReferences);
        Map<String, String> contextParam = new HashMap<>();
        contextParam.put("resteasy.document.secure.disableDTDs", "false");
        if (expandEntityReferences != null) {
            contextParam.put("resteasy.document.expand.entity.references", expandEntityReferences);
        }
        war.addClass(XxeSecureProcessingFavoriteMovieXmlRootElement.class);
        return TestUtil.finishContainerPrepare(war, contextParam, XxeSecureProcessingMovieResource.class);
    }

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    @Deployment(name = T_DEFAULT)
    public static Archive<?> deployDefault() {
        return deploy(null);
    }

    @Deployment(name = T_TRUE)
    public static Archive<?> deployTrue() {
        return deploy("true");
    }

    @Deployment(name = T_FALSE)
    public static Archive<?> deployFalse() {
        return deploy("false");
    }

    /**
     * @tpTestDetails Small request in XML root element. "resteasy.document.expand.entity.references" property is not set.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Root Element Default Small")
    public void testXmlRootElementDefaultSmall() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/xmlRootElement", null)).request()
                .post(Entity.entity(small, "application/xml"));
        Assertions.assertEquals(200, response.getStatus());
        String entity = response.readEntity(String.class);
        logger.debug("Result: " + entity.substring(0, 30));
        Assertions.assertEquals(10000, countFoos(entity));
        response.close();
    }

    /**
     * @tpTestDetails Big request in XML root element. "resteasy.document.expand.entity.references" property is not set.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Root Element Default Big")
    public void testXmlRootElementDefaultBig() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/xmlRootElement", null)).request()
                .post(Entity.entity(big, "application/xml"));
        Assertions.assertEquals(400, response.getStatus());
        String entity = response.readEntity(String.class);
        logger.debug("Result: " + entity);
        Assertions.assertTrue(entity.contains("javax.xml.bind.UnmarshalException"));
        response.close();
    }

    /**
     * @tpTestDetails Small request in XML root element. "resteasy.document.expand.entity.references" property is set to false.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Root Element Without External Expansion Small")
    public void testXmlRootElementWithoutExternalExpansionSmall() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/xmlRootElement", T_FALSE)).request()
                .post(Entity.entity(small, "application/xml"));
        Assertions.assertEquals(200, response.getStatus());
        String entity = response.readEntity(String.class);
        logger.debug("Result: " + entity.substring(0, 30));
        Assertions.assertEquals(10000, countFoos(entity));
        response.close();
    }

    /**
     * @tpTestDetails Big request in XML root element. "resteasy.document.expand.entity.references" property is set to false.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Root Element Without External Expansion Big")
    public void testXmlRootElementWithoutExternalExpansionBig() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/xmlRootElement", T_FALSE)).request()
                .post(Entity.entity(big, "application/xml"));
        Assertions.assertEquals(400, response.getStatus());
        String entity = response.readEntity(String.class);
        logger.debug("Result: " + entity);
        Assertions.assertTrue(entity.contains("javax.xml.bind.UnmarshalException"));
        response.close();
    }

    /**
     * @tpTestDetails Small request in XML root element. "resteasy.document.expand.entity.references" property is set to true.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Root Element With External Expansion Small")
    public void testXmlRootElementWithExternalExpansionSmall() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/xmlRootElement", T_TRUE)).request()
                .post(Entity.entity(small, "application/xml"));
        Assertions.assertEquals(200, response.getStatus());
        String entity = response.readEntity(String.class);
        logger.debug("Result: " + entity.substring(0, 30));
        Assertions.assertEquals(10000, countFoos(entity));
        response.close();
    }

    /**
     * @tpTestDetails Big request in XML root element. "resteasy.document.expand.entity.references" property is set to true.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Root Element With External Expansion Big")
    public void testXmlRootElementWithExternalExpansionBig() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/xmlRootElement", T_TRUE)).request()
                .post(Entity.entity(big, "application/xml"));
        Assertions.assertEquals(400, response.getStatus());
        String entity = response.readEntity(String.class);
        logger.debug("Result: " + entity);
        Assertions.assertTrue(entity.contains("javax.xml.bind.UnmarshalException"));
        response.close();
    }

    private int countFoos(String s) {
        int count = 0;
        int pos = 0;
        while (pos >= 0) {
            pos = s.indexOf("foo", pos);
            if (pos >= 0) {
                count++;
                pos += 3;
            }
        }
        return count;
    }
}
