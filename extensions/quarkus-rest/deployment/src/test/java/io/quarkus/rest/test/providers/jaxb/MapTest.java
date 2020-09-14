package io.quarkus.rest.test.providers.jaxb;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.w3c.dom.Element;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.MapFoo;
import io.quarkus.rest.test.providers.jaxb.resource.MapJaxb;
import io.quarkus.rest.test.providers.jaxb.resource.MapResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class MapTest {

    private static Logger logger = Logger.getLogger(MapTest.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, MapFoo.class, MapJaxb.class, MapResource.class);
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

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MapTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests marshalling and unmarshalling jaxb object into/from map
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMap() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<resteasy:map xmlns=\"http://foo.com\" xmlns:resteasy=\"http://jboss.org/resteasy\">"
                + "<resteasy:entry key=\"bill\"><mapFoo name=\"hello\"/></resteasy:entry>"
                + "</resteasy:map>";

        JAXBContext ctx = JAXBContext.newInstance(MapJaxb.class, MapJaxb.Entry.class, MapFoo.class);

        MapJaxb map = new MapJaxb("entry", "key", "http://jboss.org/resteasy");
        map.addEntry("bill", new MapFoo("hello"));

        JAXBElement<MapJaxb> element = new JAXBElement<MapJaxb>(new QName("http://jboss.org/resteasy", "map", "resteasy"),
                MapJaxb.class, map);

        StringWriter writer = new StringWriter();
        ctx.createMarshaller().marshal(element, writer);
        Assert.assertEquals(xml, writer.toString());

        ByteArrayInputStream is = new ByteArrayInputStream(writer.toString().getBytes());
        StreamSource source = new StreamSource(is);
        JAXBContext ctx2 = JAXBContext.newInstance(MapJaxb.class);
        element = ctx2.createUnmarshaller().unmarshal(source, MapJaxb.class);

        Element entry = (Element) element.getValue().getValue().get(0);

        JAXBContext ctx3 = JAXBContext.newInstance(MapJaxb.Entry.class);
        JAXBElement<MapJaxb.Entry> e = ctx3.createUnmarshaller().unmarshal(entry, MapJaxb.Entry.class);
    }

    /**
     * @tpTestDetails Tests Jaxb object which is send to the server and from server, the response is read by using GenericType
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testProvider() throws Exception {
        String xml = "<resteasy:map xmlns:resteasy=\"http://jboss.org/resteasy\">"
                + "<resteasy:entry key=\"bill\" xmlns=\"http://foo.com\">"
                + "<mapFoo name=\"bill\"/></resteasy:entry>"
                + "<resteasy:entry key=\"monica\" xmlns=\"http://foo.com\">"
                + "<mapFoo name=\"monica\"/></resteasy:entry>"
                + "</resteasy:map>";

        QuarkusRestWebTarget target = client.target(generateURL("/map"));

        Map<String, MapFoo> entity = target.request().post(Entity.xml(xml), new GenericType<Map<String, MapFoo>>() {
        });
        Assert.assertEquals("The response from the server has unexpected content", 2, entity.size());
        Assert.assertNotNull("The response from the server has unexpected content", entity.get("bill"));
        Assert.assertNotNull("The response from the server has unexpected content", entity.get("monica"));
        Assert.assertEquals("The response from the server has unexpected content", entity.get("bill").getName(), "bill");
        Assert.assertEquals("The response from the server has unexpected content", entity.get("monica").getName(), "monica");

        String entityString = target.request().post(Entity.xml(xml), String.class);
        logger.info(entityString);

    }

    /**
     * @tpTestDetails Tests Jaxb object which is send to the server and from server, the response is read by using GenericType,
     *                The tested entity contains integer key types
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testProviderMapIntegerFoo() throws Exception {
        String xml = "<resteasy:map xmlns:resteasy=\"http://jboss.org/resteasy\">"
                + "<resteasy:entry key=\"1\" xmlns=\"http://foo.com\">"
                + "<mapFoo name=\"bill\"/></resteasy:entry>"
                + "<resteasy:entry key=\"2\" xmlns=\"http://foo.com\">"
                + "<mapFoo name=\"monica\"/></resteasy:entry>"
                + "</resteasy:map>";

        QuarkusRestWebTarget target = client.target(generateURL("/map/integerFoo"));
        Response response = target.request().post(Entity.xml(xml));
        Assert.assertEquals(Status.OK, response.getStatus());

        Map<String, MapFoo> entity = response.readEntity(new GenericType<Map<String, MapFoo>>() {
        });
        Assert.assertEquals("The response from the server has unexpected content", 2, entity.size());
        Assert.assertNotNull("The response from the server has unexpected content", entity.get("1"));
        Assert.assertNotNull("The response from the server has unexpected content", entity.get("2"));
        Assert.assertEquals("The response from the server has unexpected content", entity.get("1").getName(), "bill");
        Assert.assertEquals("The response from the server has unexpected content", entity.get("2").getName(), "monica");

        String entityString = target.request().post(Entity.xml(xml), String.class);

        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<map xmlns:ns2=\"http://foo.com\">"
                + "<entry key=\"1\"><ns2:mapFoo name=\"bill\"/></entry>"
                + "<entry key=\"2\"><ns2:mapFoo name=\"monica\"/></entry>"
                + "</map>";
        Assert.assertEquals(result, entityString);
    }

    /**
     * @tpTestDetails Tests Jaxb object which is send to the server and from server, the response is read by using GenericType,
     *                the resource is annotated with @WrappedMap annotation
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testWrapped() throws Exception {
        String xml = "<map xmlns:mapFoo=\"http://foo.com\">"
                + "<entry key=\"bill\">"
                + "<mapFoo:mapFoo name=\"bill\"/></entry>"
                + "<entry key=\"monica\">"
                + "<mapFoo:mapFoo name=\"monica\"/></entry>"
                + "</map>";

        QuarkusRestWebTarget target = client.target(generateURL("/map/wrapped"));
        Map<String, MapFoo> entity = target.request().post(Entity.xml(xml), new GenericType<Map<String, MapFoo>>() {
        });

        Assert.assertEquals("The response from the server has unexpected content", 2, entity.size());
        Assert.assertNotNull("The response from the server has unexpected content", entity.get("bill"));
        Assert.assertNotNull("The response from the server has unexpected content", entity.get("monica"));
        Assert.assertEquals("The response from the server has unexpected content", entity.get("bill").getName(), "bill");
        Assert.assertEquals("The response from the server has unexpected content", entity.get("monica").getName(), "monica");

    }

    /**
     * @tpTestDetails Tests that Jaxb object with wrong structure returns bad request (400) response code
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testBadWrapped() throws Exception {
        String xml = "<resteasy:map xmlns:resteasy=\"http://jboss.org/resteasy\">"
                + "<resteasy:entry key=\"bill\" xmlns=\"http://foo.com\">"
                + "<mapFoo name=\"bill\"/></resteasy:entry>"
                + "<resteasy:entry key=\"monica\" xmlns=\"http://foo.com\">"
                + "<mapFoo name=\"monica\"/></resteasy:entry>"
                + "</resteasy:map>";

        QuarkusRestWebTarget target = client.target(generateURL("/map/wrapped"));
        Response response = target.request().post(Entity.xml(xml));
        Assert.assertEquals(Status.BAD_REQUEST, response.getStatus());

    }
}
