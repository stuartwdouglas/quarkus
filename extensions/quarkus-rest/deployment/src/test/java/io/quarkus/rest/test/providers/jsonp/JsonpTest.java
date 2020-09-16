package io.quarkus.rest.test.providers.jsonp;

import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
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

import io.quarkus.rest.test.providers.jsonp.resource.JsonpResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Json-p provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Jsonp Test")
public class JsonpTest {

    protected static final Logger logger = Logger.getLogger(JsonpTest.class.getName());

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(JsonpTest.class);
            return TestUtil.finishContainerPrepare(war, null, JsonpResource.class);
        }
    });

    @BeforeEach
    public void init() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, JsonpTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends POST request with JsonObject entity. The JsonObject should be returned back by the
     *                response and should contain the same field values as original request.
     *                in the second case multiple json entities as String.
     * @tpPassCrit The resource returns JsonObject with correct values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Object")
    public void testObject() throws Exception {
        doTestObject("UTF-8");
        doTestObject("UTF-16");
        doTestObject("UTF-32");
        doTestObject(null);
    }

    private void doTestObject(String charset) throws Exception {
        WebTarget target = client.target(generateURL("/test/json/object"));
        MediaType mediaType = MediaType.APPLICATION_JSON_TYPE.withCharset(charset);
        Entity<String> entity = Entity.entity("{ \"name\" : \"Bill\" }", mediaType);
        String json = target.request().post(entity, String.class);
        logger.info("Request entity: " + json);
        JsonObject obj = Json.createObjectBuilder().add("name", "Bill").add("id", 10001).build();
        obj = target.request().post(Entity.json(obj), JsonObject.class);
        Assertions.assertTrue(obj.containsKey("name"), "JsonObject from the response doesn't contain field 'name'");
        Assertions.assertEquals(obj.getJsonString("name").getString(), "Bill",
                "JsonObject from the response doesn't contain correct value for the field 'name'");
        Assertions.assertTrue(obj.containsKey("id"), "JsonObject from the response doesn't contain field 'id'");
        Assertions.assertEquals(obj.getJsonNumber("id").longValue(), 10001,
                "JsonObject from the response doesn't contain correct value for the field 'id'");
    }

    /**
     * @tpTestDetails Client sends POST request with JsonArray entity. The JsonArray should be returned back by the
     *                response and should contain the same field values as original request.
     *                in the second case multiple json entities as String.
     * @tpPassCrit The resource returns JsonArray with correct values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Array")
    public void testArray() throws Exception {
        doTestArray("UTF-8");
        doTestArray("UTF-16");
        doTestArray("UTF-32");
        doTestArray(null);
    }

    private void doTestArray(String charset) {
        WebTarget target = client.target(generateURL("/test/json/array"));
        MediaType mediaType = MediaType.APPLICATION_JSON_TYPE.withCharset(charset);
        Entity<String> entity = Entity.entity("[{ \"name\" : \"Bill\" },{ \"name\" : \"Monica\" }]", mediaType);
        // String json = target.request().post(Entity.json("[{ \"name\" : \"Bill\" },{ \"name\" : \"Monica\" }]"), String.class);
        String json = target.request().post(entity, String.class);
        logger.info("Request entity: " + json);
        JsonArray array = Json.createArrayBuilder().add(Json.createObjectBuilder().add("name", "Bill").build())
                .add(Json.createObjectBuilder().add("name", "Monica").build()).build();
        array = target.request().post(Entity.json(array), JsonArray.class);
        Assertions.assertEquals(2, array.size(), "JsonArray from the response doesn't contain two elements as it should");
        JsonObject obj = array.getJsonObject(0);
        Assertions.assertTrue(obj.containsKey("name"), "JsonObject[0] from the response doesn't contain field 'name'");
        Assertions.assertEquals(obj.getJsonString("name").getString(), "Bill",
                "JsonObject[0] from the response doesn't contain correct value for the field 'name'");
        obj = array.getJsonObject(1);
        Assertions.assertTrue(obj.containsKey("name"), "JsonObject[1] from the response doesn't contain field 'name'");
        Assertions.assertEquals(obj.getJsonString("name").getString(), "Monica",
                "JsonObject[1] from the response doesn't contain correct value for the field 'name'");
    }

    /**
     * @tpTestDetails Client sends POST request with JsonStructure entity. The JsonStructure should be returned back by the
     *                response and should contain the same field values as original request.
     *                in the second case multiple json entities as String.
     * @tpPassCrit The resource returns JsonStructure with correct values
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Structure")
    public void testStructure() throws Exception {
        doTestStructure("UTF-8");
        doTestStructure("UTF-16");
        doTestStructure("UTF-32");
        doTestStructure(null);
    }

    @Test
    @DisplayName("Test Json String")
    public void testJsonString() throws Exception {
        WebTarget target = client.target(generateURL("/test/json/string"));
        JsonString jsonString = Json.createValue("Resteasy");
        JsonString result = target.request().post(Entity.json(jsonString), JsonString.class);
        Assertions.assertTrue(result.getString().equals("Hello Resteasy"),
                "JsonString object with Hello Resteasy value is expected");
    }

    @Test
    @DisplayName("Test Json Number")
    public void testJsonNumber() throws Exception {
        WebTarget target = client.target(generateURL("/test/json/number"));
        JsonNumber jsonNumber = Json.createValue(100);
        JsonNumber result = target.request().post(Entity.json(jsonNumber), JsonNumber.class);
        Assertions.assertTrue(result.intValue() == 200, "JsonNumber object with 200 value is expected");
    }

    private void doTestStructure(String charset) {
        WebTarget target = client.target(generateURL("/test/json/structure"));
        MediaType mediaType = MediaType.APPLICATION_JSON_TYPE.withCharset(charset);
        Entity<String> entity = Entity.entity("{ \"name\" : \"Bill\" }", mediaType);
        String json = target.request().post(entity, String.class);
        logger.info("Request entity: " + json);
        JsonStructure str = (JsonStructure) Json.createObjectBuilder().add("name", "Bill").build();
        JsonStructure structure = target.request().post(Entity.json(str), JsonStructure.class);
        JsonObject obj = (JsonObject) structure;
        Assertions.assertTrue(obj.containsKey("name"), "JsonObject from the response doesn't contain field 'name'");
        Assertions.assertEquals(obj.getJsonString("name").getString(), "Bill",
                "JsonObject from the response doesn't contain correct value for the field 'name'");
    }
}
