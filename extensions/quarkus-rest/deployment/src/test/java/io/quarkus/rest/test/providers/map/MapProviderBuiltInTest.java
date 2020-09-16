package io.quarkus.rest.test.providers.map;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.map.resource.MapProviderBuiltInResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class MapProviderBuiltInTest {

    static Client client;

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, MapProviderBuiltInResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MapProviderBuiltInTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client creates request of type "POST" with entity of type MultiValuesMap and sends it to the
     *                server using invocation method. The server returns response containing MultiValuedMap. The builtin
     *                Resteasy MapProvider
     *                is used for reading request and writing response.
     * @tpPassCrit Correct response is returned from the server and map contains original item
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMapInvoke() {
        // writers sorted by type, mediatype, and then by app over builtin
        MultivaluedMap<String, String> map = new MultivaluedHashMap<String, String>();
        map.add("map", "map");
        Response response = client.target(generateURL("/map")).request(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .build("POST", Entity.entity(map, MediaType.APPLICATION_FORM_URLENCODED)).invoke();
        Assert.assertEquals(response.getStatus(), 200);
        String data = response.readEntity(String.class);
        Assert.assertTrue(data.contains("map"));
        response.close();
    }

    /**
     * @tpTestDetails Client sends POST request with specified mediatype and entity of type APPLICATION_FORM_URLENCODED_TYPE
     *                using post method. The server returns response containing MultiValuedMap. The builtin Resteasy MapProvider
     *                is used for reading request and writing response.
     * @tpPassCrit Correct response is returned from the server and map contains original item
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMapPost() {
        // writers sorted by type, mediatype, and then by app over builtin
        MultivaluedMap<String, String> map = new MultivaluedHashMap<String, String>();
        map.add("map", "map");
        Response response = client.target(generateURL("/map")).request(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .post(Entity.entity(map, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        Assert.assertEquals(response.getStatus(), 200);
        String data = response.readEntity(String.class);
        Assert.assertTrue(data.contains("map"));
        response.close();
    }

}
