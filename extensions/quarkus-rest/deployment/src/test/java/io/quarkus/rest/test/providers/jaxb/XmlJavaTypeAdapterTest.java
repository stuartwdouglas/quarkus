package io.quarkus.rest.test.providers.jaxb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.XmlJavaTypeAdapterAlien;
import io.quarkus.rest.test.providers.jaxb.resource.XmlJavaTypeAdapterAlienAdapter;
import io.quarkus.rest.test.providers.jaxb.resource.XmlJavaTypeAdapterFoo;
import io.quarkus.rest.test.providers.jaxb.resource.XmlJavaTypeAdapterHuman;
import io.quarkus.rest.test.providers.jaxb.resource.XmlJavaTypeAdapterResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Xml Java Type Adapter Test")
public class XmlJavaTypeAdapterTest {

    private final Logger logger = Logger.getLogger(XmlJavaTypeAdapterTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(XmlJavaTypeAdapterTest.class);
            // Arquillian in the deployment and use of PortProviderUtil in the deployment
            return TestUtil.finishContainerPrepare(war, null, XmlJavaTypeAdapterAlien.class,
                    XmlJavaTypeAdapterAlienAdapter.class, XmlJavaTypeAdapterFoo.class, XmlJavaTypeAdapterHuman.class,
                    XmlJavaTypeAdapterResource.class, PortProviderUtil.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, XmlJavaTypeAdapterTest.class.getSimpleName());
    }

    @DisplayName("Tralfamadorean")
    public static class Tralfamadorean extends XmlJavaTypeAdapterAlien {
    }

    /**
     * @tpTestDetails Tests jaxb resource is returning correct string with @XmlJavaTypeAdapter in place
     * @tpInfo RESTEASY-1088
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Human")
    public void testPostHuman() {
        QuarkusRestWebTarget target = client.target(generateURL("/human"));
        XmlJavaTypeAdapterHuman human = new XmlJavaTypeAdapterHuman();
        human.setName("bill");
        String response = target.request().post(Entity.entity(human, MediaType.APPLICATION_XML_TYPE), String.class);
        Assertions.assertEquals("bill", response, "The received response was not the expected one");
    }

    /**
     * @tpTestDetails Tests jaxb with class annotated by @XmlJavaTypeAdapter, resource returning Foo object
     * @tpInfo RESTEASY-1088
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Foo To Foo")
    public void testPostFooToFoo() {
        QuarkusRestWebTarget target = client.target(generateURL("/foo/foo"));
        XmlJavaTypeAdapterFoo foo = new XmlJavaTypeAdapterFoo();
        foo.setName("bill");
        XmlJavaTypeAdapterFoo response = target.request().post(Entity.entity(foo, MediaType.APPLICATION_XML_TYPE),
                XmlJavaTypeAdapterFoo.class);
        Assertions.assertEquals(foo, response, "The received response was not the expected one");
    }

    /**
     * @tpTestDetails Tests jaxb with class annotated by @XmlJavaTypeAdapter, resource returning String
     * @tpInfo RESTEASY-1088
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Foo To String")
    public void testPostFooToString() {
        QuarkusRestWebTarget target = client.target(generateURL("/foo/foo"));
        XmlJavaTypeAdapterFoo foo = new XmlJavaTypeAdapterFoo();
        foo.setName("bill");
        String response = target.request().post(Entity.entity(foo, MediaType.APPLICATION_XML_TYPE), String.class);
        logger.info("response: \"" + response + "\"");
        Assertions.assertTrue(
                response.contains("<xmlJavaTypeAdapterFoo><alien><name>llib</name></alien></xmlJavaTypeAdapterFoo>"),
                "The received response was not the expected one");
    }

    /**
     * @tpTestDetails Tests jaxb with class annotated by @XmlJavaTypeAdapter, resource returning list of Human objects
     * @tpInfo RESTEASY-1088
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Human List")
    public void testPostHumanList() {
        QuarkusRestWebTarget target = client.target(generateURL("/list/human"));
        List<XmlJavaTypeAdapterHuman> list = new ArrayList<XmlJavaTypeAdapterHuman>();
        XmlJavaTypeAdapterHuman human = new XmlJavaTypeAdapterHuman();
        human.setName("bill");
        list.add(human);
        human = new XmlJavaTypeAdapterHuman();
        human.setName("bob");
        list.add(human);
        GenericEntity<List<XmlJavaTypeAdapterHuman>> entity = new GenericEntity<List<XmlJavaTypeAdapterHuman>>(list) {
        };
        String response = target.request().post(Entity.entity(entity, MediaType.APPLICATION_XML_TYPE), String.class);
        Assertions.assertEquals("|bill|bob", response, "The received response was not the expected one");
    }

    /**
     * @tpTestDetails Tests jaxb with class annotated by @XmlJavaTypeAdapter, resource returning list of Alien objects,
     *                where application expects the use of Human class with jaxb annotation, XmlJavaTypeAdapter is used to
     *                convert Alien
     *                to Human and back
     * @tpInfo RESTEASY-1088
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Alien List")
    public void testPostAlienList() {
        QuarkusRestWebTarget target = client.target(generateURL("/list/alien"));
        List<XmlJavaTypeAdapterAlien> list = new ArrayList<XmlJavaTypeAdapterAlien>();
        XmlJavaTypeAdapterAlien alien1 = new XmlJavaTypeAdapterAlien();
        alien1.setName("bill");
        list.add(alien1);
        XmlJavaTypeAdapterAlien alien2 = new XmlJavaTypeAdapterAlien();
        alien2.setName("bob");
        list.add(alien2);
        GenericEntity<List<XmlJavaTypeAdapterAlien>> entity = new GenericEntity<List<XmlJavaTypeAdapterAlien>>(list) {
        };
        GenericType<List<XmlJavaTypeAdapterAlien>> alienListType = new GenericType<List<XmlJavaTypeAdapterAlien>>() {
        };
        List<XmlJavaTypeAdapterAlien> response = target.request().post(Entity.entity(entity, MediaType.APPLICATION_XML_TYPE),
                alienListType);
        logger.info("response: \"" + response + "\"");
        Assertions.assertEquals(2, response.size(), "The received response was not the expected one");
        Assertions.assertTrue(response.contains(alien1), "The received response was not the expected one");
        Assertions.assertTrue(response.contains(alien2), "The received response was not the expected one");
        Assertions.assertEquals(4, XmlJavaTypeAdapterAlienAdapter.marshalCounter.get(),
                "The marshalling of the Alien didn't happen the correct way");
        Assertions.assertEquals(4, XmlJavaTypeAdapterAlienAdapter.unmarshalCounter.get(),
                "The unmarshalling of the Human didn't happen the correct way");
        XmlJavaTypeAdapterAlienAdapter.unmarshalCounter.set(0);
        XmlJavaTypeAdapterAlienAdapter.marshalCounter.set(0);
    }

    /**
     * @tpTestDetails Tests jaxb with class annotated by @XmlJavaTypeAdapter, resource returning array of Alien objects,
     *                where application expects the use of Human class with jaxb annotation, XmlJavaTypeAdapter is used to
     *                convert Alien
     *                to Human and back
     * @tpInfo RESTEASY-1088
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Alien Array")
    public void testPostAlienArray() {
        QuarkusRestWebTarget target = client.target(generateURL("/array/alien"));
        XmlJavaTypeAdapterAlien[] array = new XmlJavaTypeAdapterAlien[2];
        XmlJavaTypeAdapterAlien alien1 = new XmlJavaTypeAdapterAlien();
        alien1.setName("bill");
        array[0] = alien1;
        XmlJavaTypeAdapterAlien alien2 = new XmlJavaTypeAdapterAlien();
        alien2.setName("bob");
        array[1] = alien2;
        GenericEntity<XmlJavaTypeAdapterAlien[]> entity = new GenericEntity<XmlJavaTypeAdapterAlien[]>(array) {
        };
        GenericType<XmlJavaTypeAdapterAlien[]> alienArrayType = new GenericType<XmlJavaTypeAdapterAlien[]>() {
        };
        XmlJavaTypeAdapterAlien[] response = target.request().post(Entity.entity(entity, MediaType.APPLICATION_XML_TYPE),
                alienArrayType);
        logger.info("response: \"" + response + "\"");
        Assertions.assertEquals(2, response.length, "The received response was not the expected one");
        Assertions.assertTrue(
                (alien1.equals(response[0]) && alien2.equals(response[1]))
                        || (alien1.equals(response[1]) && alien2.equals(response[0])),
                "The received response was not the expected one");
        Assertions.assertEquals(4, XmlJavaTypeAdapterAlienAdapter.marshalCounter.get(),
                "The marshalling of the Alien didn't happen the correct way");
        Assertions.assertEquals(4, XmlJavaTypeAdapterAlienAdapter.unmarshalCounter.get(),
                "The unmarshalling of the Human didn't happen the correct way");
        XmlJavaTypeAdapterAlienAdapter.unmarshalCounter.set(0);
        XmlJavaTypeAdapterAlienAdapter.marshalCounter.set(0);
    }

    /**
     * @tpTestDetails Tests jaxb with class annotated by @XmlJavaTypeAdapter, resource returning map of Alien objects,
     *                where application expects the use of Human class with jaxb annotation, XmlJavaTypeAdapter is used to
     *                convert Alien
     *                to Human and back
     * @tpInfo RESTEASY-1088
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Alien Map")
    public void testPostAlienMap() {
        QuarkusRestWebTarget target = client.target(generateURL("/map/alien"));
        Map<String, XmlJavaTypeAdapterAlien> map = new HashMap<String, XmlJavaTypeAdapterAlien>();
        XmlJavaTypeAdapterAlien alien1 = new XmlJavaTypeAdapterAlien();
        alien1.setName("bill");
        map.put("abc", alien1);
        XmlJavaTypeAdapterAlien alien2 = new XmlJavaTypeAdapterAlien();
        alien2.setName("bob");
        map.put("xyz", alien2);
        GenericEntity<Map<String, XmlJavaTypeAdapterAlien>> entity = new GenericEntity<Map<String, XmlJavaTypeAdapterAlien>>(
                map) {
        };
        GenericType<Map<String, XmlJavaTypeAdapterAlien>> alienMapType = new GenericType<Map<String, XmlJavaTypeAdapterAlien>>() {
        };
        Map<String, XmlJavaTypeAdapterAlien> response = target.request()
                .post(Entity.entity(entity, MediaType.APPLICATION_XML_TYPE), alienMapType);
        logger.info("response: \"" + response + "\"");
        Assertions.assertEquals(2, response.size(), "The received response was not the expected one");
        Assertions.assertTrue(alien1.equals(response.get("abc")), "The received response was not the expected one");
        Assertions.assertTrue(alien2.equals(response.get("xyz")), "The received response was not the expected one");
        Assertions.assertEquals(4, XmlJavaTypeAdapterAlienAdapter.marshalCounter.get(),
                "The marshalling of the Alien didn't happen the correct way");
        Assertions.assertEquals(4, XmlJavaTypeAdapterAlienAdapter.unmarshalCounter.get(),
                "The unmarshalling of the Human didn't happen the correct way");
        XmlJavaTypeAdapterAlienAdapter.unmarshalCounter.set(0);
        XmlJavaTypeAdapterAlienAdapter.marshalCounter.set(0);
    }

    /**
     * @tpTestDetails Tests jaxb with class annotated by @XmlJavaTypeAdapter, resource returning list of Alien objects,
     *                where application expects the use of Human class with jaxb annotation, XmlJavaTypeAdapter is used to
     *                convert Alien
     *                to Human and back. The Entity send to the server extends Alien class.
     * @tpInfo RESTEASY-1088
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Tralfamadorean List")
    public void testPostTralfamadoreanList() {
        QuarkusRestWebTarget target = client.target(generateURL("/list/alien"));
        List<XmlJavaTypeAdapterAlien> list = new ArrayList<XmlJavaTypeAdapterAlien>();
        Tralfamadorean tralfamadorean1 = new Tralfamadorean();
        tralfamadorean1.setName("bill");
        list.add(tralfamadorean1);
        Tralfamadorean tralfamadorean2 = new Tralfamadorean();
        tralfamadorean2.setName("bob");
        list.add(tralfamadorean2);
        GenericEntity<List<XmlJavaTypeAdapterAlien>> entity = new GenericEntity<List<XmlJavaTypeAdapterAlien>>(list) {
        };
        GenericType<List<XmlJavaTypeAdapterAlien>> alienListType = new GenericType<List<XmlJavaTypeAdapterAlien>>() {
        };
        List<XmlJavaTypeAdapterAlien> response = target.request().post(Entity.entity(entity, MediaType.APPLICATION_XML_TYPE),
                alienListType);
        logger.info("response: \"" + response + "\"");
        Assertions.assertEquals(2, response.size(), "The received response was not the expected one");
        Assertions.assertTrue(response.contains(tralfamadorean1), "The received response was not the expected one");
        Assertions.assertTrue(response.contains(tralfamadorean2), "The received response was not the expected one");
        Assertions.assertEquals(4, XmlJavaTypeAdapterAlienAdapter.marshalCounter.get(),
                "The marshalling of the Alien didn't happen the correct way");
        Assertions.assertEquals(4, XmlJavaTypeAdapterAlienAdapter.unmarshalCounter.get(),
                "The unmarshalling of the Human didn't happen the correct way");
        XmlJavaTypeAdapterAlienAdapter.unmarshalCounter.set(0);
        XmlJavaTypeAdapterAlienAdapter.marshalCounter.set(0);
    }
}
