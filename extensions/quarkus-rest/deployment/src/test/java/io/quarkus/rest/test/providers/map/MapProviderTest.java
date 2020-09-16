package io.quarkus.rest.test.providers.map;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.map.resource.MapProvider;
import io.quarkus.rest.test.providers.map.resource.MapProviderAbstractProvider;
import io.quarkus.rest.test.providers.map.resource.MapProviderResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Map Provider Test")
public class MapProviderTest {

    static Client client;

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(MapProviderAbstractProvider.class);
            return TestUtil.finishContainerPrepare(war, null, MapProviderResource.class, MapProvider.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MapProviderTest.class.getSimpleName());
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Client sends POST request with specified mediatype and entity of type APPLICATION_FORM_URLENCODED_TYPE.
     *                This entity is read by application provided MapProvider, which creates Multivaluedmap and adds item into
     *                it.
     *                Server sends response using application provided MapProvider, replacing content of the first item in the
     *                map.
     * @tpPassCrit Correct response is returned from the server and map contains replaced item
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Map Provider")
    public void testMapProvider() {
        // writers sorted by type, mediatype, and then by app over builtin
        Response response = client.target(generateURL("/map")).request(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .post(Entity.entity("map", MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        Assertions.assertEquals(response.getStatus(), 200);
        String data = response.readEntity(String.class);
        Assertions.assertTrue(data.contains("MapWriter"));
        response.close();
    }
}
