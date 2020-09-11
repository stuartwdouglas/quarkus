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
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class PatchErrorHandlingTest {
    static Client client;

    @BeforeClass
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
        client = null;
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
        Assert.assertEquals(Status.BAD_REQUEST, res.getStatus());
    }

    /**
     * @tpTestDetails Client sends PATCH request in the format which is not supported for the resource.
     *                Server should return 415 (Unsupported media type)
     * @tpSince RESTEasy 3.5.0
     */
    @Test
    public void testUnsupportedPatchDocument() throws Exception {
        WebTarget patchTarget = client.target(generateURL("/students/1"));
        Student student = new Student().setFirstName("test");
        Response res = patchTarget.request().build(HttpMethod.PATCH, Entity.entity(student, MediaType.APPLICATION_JSON_TYPE))
                .invoke();
        Assert.assertEquals(Status.UNSUPPORTED_MEDIA_TYPE, res.getStatus());
    }

    /**
     * @tpTestDetails Client sends valid Patch request with with valid format descriptor, but the resource doesn't exists.
     *                Server should return 404 (Not found).
     * @tpSince RESTEasy 3.5.0
     */
    @Test
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
        Assert.assertEquals(Status.NOT_FOUND, res.getStatus());
    }

    /**
     * @tpTestDetails Client sends Patch request to patch property of the object which doesn't exists.
     *                Server should return 409 (Conflict)
     * @tpSince RESTEasy 3.5.0
     */
    @Test
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
        Assert.assertEquals(Status.CONFLICT, res.getStatus());
    }
}
