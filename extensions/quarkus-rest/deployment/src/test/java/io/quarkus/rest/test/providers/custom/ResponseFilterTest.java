package io.quarkus.rest.test.providers.custom;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.custom.resource.ResponseFilter;
import io.quarkus.rest.test.providers.custom.resource.ResponseFilterResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Response Filter Test")
public class ResponseFilterTest {

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(CollectionProviderTest.class);
            return TestUtil.finishContainerPrepare(war, null, ResponseFilterResource.class, ResponseFilter.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResponseFilterTest.class.getSimpleName());
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Client sends POST requests for each possible Response status code as the entity. The Response is
     *                set up for the response code of the request. Response is then processed by ResponseFilter which sets
     *                response code
     *                to 200 (OK) and puts the original response code into the body of the response.
     * @tpPassCrit The response code status is changed to 200 (SUCCESS) and response contains the original code from the
     *             request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Status")
    public void testStatus() {
        for (Response.Status status : Response.Status.values()) {
            String content = String.valueOf(status.getStatusCode());
            Response response = client.target(generateURL("/resource/getstatus")).request().post(Entity.text(content));
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(content, response.readEntity(String.class),
                    "The entity doesn't contain the original http code of the request");
            response.close();
        }
    }

    /**
     * @tpTestDetails Client sends POST requests for each possible Response status code as the entity. The Response is
     *                set up for the response code of the request. Response is then processed by ResponseFilter which sets
     *                response code
     *                to 200 (OK) if the statusInfo() method of the responseContext returns non-null result.
     *                Then it puts the original response code into the body of the response. If the statusInfo() method returns
     *                null result
     *                the entity of the response is set up to null.
     * @tpPassCrit The response code status is changed to 200 (SUCCESS) and response contains the original code from the
     *             request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Status Info")
    public void testStatusInfo() {
        for (Response.Status status : Response.Status.values()) {
            String content = String.valueOf(status.getStatusCode());
            Response response = client.target(generateURL("/resource/getstatusinfo")).request().post(Entity.text(content));
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(content, response.readEntity(String.class),
                    "The entity doesn't contain the original http code of the request");
            response.close();
        }
    }

    /**
     * @tpTestDetails Client sends POST requests with the text entity containing information abou type. Response filter
     *                uses getEntityType() method of the responseContext to get information about type of the entity in the
     *                Response.
     * @tpPassCrit The response code status is changed to 200 (SUCCESS) and response contains the original code from the
     *             request
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Entity Type")
    public void testEntityType() {
        String content = "string";
        Response response = client.target(generateURL("/resource/getentitytype")).request().post(Entity.text(content));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals(String.class.getName(), response.readEntity(String.class),
                "The entity doesn't contain the original entity type");
        response.close();
    }
}
