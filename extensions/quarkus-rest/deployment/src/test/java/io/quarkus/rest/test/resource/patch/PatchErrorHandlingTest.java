package io.quarkus.rest.test.resource.patch;

import java.util.function.Supplier;

import javax.json.Json;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
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

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Patch Error Handling Test")
public class PatchErrorHandlingTest {

    static Client client;

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
        client = null;
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, StudentResource.class, Student.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, PatchErrorHandlingTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails When the server determines that the patch document provided by the client is not properly formatted,
     *                it SHOULD return a 400 (Bad Request) response.
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    @DisplayName("Test Malformed Patch Document")
    public void testMalformedPatchDocument() throws Exception {
        WebTarget base = client.target(generateURL("/students"));
        Student newStudent = new Student().setId(1L).setFirstName("Taylor").setSchool("school1");
        base.request().post(Entity.entity(newStudent, MediaType.APPLICATION_JSON_TYPE));
        WebTarget patchTarget = client.target(generateURL("/students/1"));
        javax.json.JsonArray patchRequest = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("op", "copyyy").add("from", "/firstName").add("path", "/lastName").build())
                .build();
        Response res = patchTarget.request()
                .build(HttpMethod.PATCH, Entity.entity(patchRequest, MediaType.APPLICATION_JSON_PATCH_JSON)).invoke();
        Assertions.assertEquals(Status.BAD_REQUEST.getStatusCode(), res.getStatus());
    }

    /**
     * @tpTestDetails Client sends PATCH request in the format which is not supported for the resource.
     *                Server should return 415 (Unsupported media type)
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    @DisplayName("Test Unsupported Patch Document")
    public void testUnsupportedPatchDocument() throws Exception {
        WebTarget patchTarget = client.target(generateURL("/students/1"));
        Student student = new Student().setFirstName("test");
        Response res = patchTarget.request().build(HttpMethod.PATCH, Entity.entity(student, MediaType.APPLICATION_JSON_TYPE))
                .invoke();
        Assertions.assertEquals(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), res.getStatus());
    }

    /**
     * @tpTestDetails Client sends valid Patch request with with valid format descriptor, but the resource doesn't exists.
     *                Server should return 404 (Not found).
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    @DisplayName("Test Resource Not Found")
    public void testResourceNotFound() throws Exception {
        WebTarget base = client.target(generateURL("/students"));
        Student newStudent = new Student().setId(1L).setFirstName("Taylor").setSchool("school1");
        base.request().post(Entity.entity(newStudent, MediaType.APPLICATION_JSON_TYPE));
        WebTarget patchTarget = client.target(generateURL("/students/1088"));
        javax.json.JsonArray patchRequest = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("op", "copy").add("from", "/firstName").add("path", "/lastName").build())
                .build();
        Response res = patchTarget.request()
                .build(HttpMethod.PATCH, Entity.entity(patchRequest, MediaType.APPLICATION_JSON_PATCH_JSON)).invoke();
        Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), res.getStatus());
    }

    /**
     * @tpTestDetails Client sends Patch request to patch property of the object which doesn't exists.
     *                Server should return 409 (Conflict)
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    @DisplayName("Test Conflicting State")
    public void testConflictingState() throws Exception {
        WebTarget base = client.target(generateURL("/students"));
        Student newStudent = new Student().setId(1L).setFirstName("Taylor").setSchool("school1");
        base.request().post(Entity.entity(newStudent, MediaType.APPLICATION_JSON_TYPE));
        WebTarget patchTarget = client.target(generateURL("/students/1"));
        javax.json.JsonArray patchRequest = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("op", "replace").add("path", "/wrongProperty").add("value", "John").build())
                .build();
        Response res = patchTarget.request()
                .build(HttpMethod.PATCH, Entity.entity(patchRequest, MediaType.APPLICATION_JSON_PATCH_JSON)).invoke();
        Assertions.assertEquals(Status.CONFLICT.getStatusCode(), res.getStatus());
    }
}
