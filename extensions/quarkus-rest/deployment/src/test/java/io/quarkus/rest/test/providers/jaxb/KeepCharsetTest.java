package io.quarkus.rest.test.providers.jaxb;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import io.quarkus.rest.test.providers.jaxb.resource.KeepCharsetFavoriteMovieXmlRootElement;
import io.quarkus.rest.test.providers.jaxb.resource.KeepCharsetMovieResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1066. If the content-type of the response is not specified in the request,
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Keep Charset Test")
public class KeepCharsetTest {

    protected final Logger logger = Logger.getLogger(KeepCharsetTest.class.getName());

    static QuarkusRestClient client;

    protected static final MediaType APPLICATION_XML_UTF16_TYPE;

    private static final String EXPAND = "war_expand";

    private static final String NO_EXPAND = "war_no_expand";

    private static final String entityXml = "<?xml version=\"1.0\"?>\r"
            + "<keepCharsetFavoriteMovieXmlRootElement><title>La Règle du Jeu</title></keepCharsetFavoriteMovieXmlRootElement>";

    static {
        Map<String, String> params = new HashMap<String, String>();
        params.put("charset", "UTF-16");
        APPLICATION_XML_UTF16_TYPE = new MediaType("application", "xml", params);
    }

    @Deployment(name = NO_EXPAND)
    public static Archive<?> deployExpandFalse() {
        Map<String, String> params = new HashMap<String, String>();
        WebArchive war = TestUtil.prepareArchive(NO_EXPAND);
        params.put("charset", "UTF-16");
        params.put("resteasy.document.expand.entity.references", "false");
        return TestUtil.finishContainerPrepare(war, params, KeepCharsetMovieResource.class,
                KeepCharsetFavoriteMovieXmlRootElement.class);
    }

    @Deployment(name = EXPAND)
    public static Archive<?> deployExpandTrue() {
        Map<String, String> params = new HashMap<String, String>();
        WebArchive war = TestUtil.prepareArchive(EXPAND);
        params.put("charset", "UTF-16");
        params.put("resteasy.document.expand.entity.references", "true");
        return TestUtil.finishContainerPrepare(war, params, KeepCharsetMovieResource.class,
                KeepCharsetFavoriteMovieXmlRootElement.class);
    }

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Client sends POST request with jaxb annotated object entity. Request encoding is different than from
     *                encoding of the server. Default encoding is used and entity expansion is set to true.
     * @tpPassCrit The response is returned in encoding of the original request not in encoding of the server.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Default Expand")
    public void testXmlDefaultExpand() throws Exception {
        xmlDefault(EXPAND);
    }

    /**
     * @tpTestDetails Client sends POST request with jaxb annotated object entity. Request encoding is different than from
     *                encoding of the server. Default encoding is used and entity expansion is set to false.
     * @tpPassCrit The response is returned in encoding of the original request not in encoding of the server.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Default No Expand")
    public void testXmlDefaultNoExpand() throws Exception {
        xmlDefault(NO_EXPAND);
    }

    private void xmlDefault(String path) throws Exception {
        WebTarget target = client.target(PortProviderUtil.generateURL("/xml/default", path));
        logger.info(entityXml);
        logger.info("client default charset: " + Charset.defaultCharset());
        logger.info("Sending request");
        Response response = target.request().accept(MediaType.APPLICATION_XML_TYPE)
                .post(Entity.entity(entityXml, MediaType.APPLICATION_XML_TYPE));
        logger.info("Received response");
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        KeepCharsetFavoriteMovieXmlRootElement entity = response.readEntity(KeepCharsetFavoriteMovieXmlRootElement.class);
        logger.info("Result: " + entity);
        Assertions.assertEquals("La Règle du Jeu", entity.getTitle(), "Incorrect xml entity was returned from the server");
    }

    /**
     * @tpTestDetails Client sends POST request with jaxb annotated object entity. Request encoding is different than from
     *                encoding of the server. Encoding is set up in resource produces annotation and entity expansion is set to
     *                true.
     * @tpPassCrit The response is returned in encoding of the original request not in encoding of the server.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Produces Expand")
    public void testXmlProducesExpand() throws Exception {
        XmlProduces(EXPAND);
    }

    /**
     * @tpTestDetails Client sends POST request with jaxb annotated object entity. Request encoding is different than from
     *                encoding of the server. Encoding is set up in resource produces annotation and entity expansion is set to
     *                false.
     * @tpPassCrit The response is returned in encoding of the original request not in encoding of the server.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Produces No Expand")
    public void testXmlProducesNoExpand() throws Exception {
        XmlProduces(NO_EXPAND);
    }

    private void XmlProduces(String path) throws Exception {
        WebTarget target = client.target(PortProviderUtil.generateURL("/xml/produces", path));
        logger.info(entityXml);
        logger.info("client default charset: " + Charset.defaultCharset());
        Response response = target.request().post(Entity.entity(entityXml, APPLICATION_XML_UTF16_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        KeepCharsetFavoriteMovieXmlRootElement entity = response.readEntity(KeepCharsetFavoriteMovieXmlRootElement.class);
        logger.info("Result: " + entity);
        Assertions.assertEquals("La Règle du Jeu", entity.getTitle(), "Incorrect xml entity was returned from the server");
    }

    /**
     * @tpTestDetails Client sends POST request with jaxb annotated object entity. Request encoding is different than from
     *                encoding of the server. Encoding is set up in the request accepts header and entity expansion is set to
     *                true.
     * @tpPassCrit The response is returned in encoding of the original request not in encoding of the server.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Accepts Expand")
    public void testXmlAcceptsExpand() throws Exception {
        XmlAccepts(EXPAND);
    }

    /**
     * @tpTestDetails Client sends POST request with jaxb annotated object entity. Request encoding is different than from
     *                encoding of the server. Encoding is set up in the request accepts header and entity expansion is set to
     *                false.
     * @tpPassCrit The response is returned in encoding of the original request not in encoding of the server.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Xml Accepts No Expand")
    public void testXmlAcceptsNoExpand() throws Exception {
        XmlAccepts(NO_EXPAND);
    }

    private void XmlAccepts(String path) throws Exception {
        WebTarget target = client.target(PortProviderUtil.generateURL("/xml/accepts", path));
        logger.info(entityXml);
        logger.info("client default charset: " + Charset.defaultCharset());
        Response response = target.request().accept(APPLICATION_XML_UTF16_TYPE)
                .post(Entity.entity(entityXml, APPLICATION_XML_UTF16_TYPE));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        KeepCharsetFavoriteMovieXmlRootElement entity = response.readEntity(KeepCharsetFavoriteMovieXmlRootElement.class);
        logger.info("Result: " + entity);
        Assertions.assertEquals("La Règle du Jeu", entity.getTitle(), "Incorrect xml entity was returned from the server");
    }
}
