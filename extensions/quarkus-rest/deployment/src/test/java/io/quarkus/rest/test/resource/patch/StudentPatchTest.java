package io.quarkus.rest.test.resource.patch;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Student Patch Test")
public class StudentPatchTest {

    static Client client;

    static final String PATCH_DEPLOYMENT = "Patch";

    static final String DISABLED_PATCH_DEPLOYMENT = "DisablePatch";

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

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            Map<String, String> contextParam = new HashMap<>();
            contextParam.put(ResteasyContextParameters.RESTEASY_PATCH_FILTER_DISABLED, "true");
            return TestUtil.finishContainerPrepare(war, contextParam, StudentResource.class, Student.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, StudentPatchTest.class.getSimpleName());
    }

    @Test
    @OperateOnDeployment(PATCH_DEPLOYMENT)
    @DisplayName("Test Patch Student")
    public void testPatchStudent() throws Exception {
        QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).connectionPoolSize(10).build();
        WebTarget base = client.target(generateURL("/students"));
        // add a student, first name is Taylor and school is school1, other fields is null.
        Student newStudent = new Student().setId(1L).setFirstName("Taylor").setSchool("school1");
        Response response = base.request().post(Entity.<Student> entity(newStudent, MediaType.APPLICATION_JSON_TYPE));
        Student s = response.readEntity(Student.class);
        Assertions.assertNotNull(s, "Add student failed");
        Assertions.assertEquals(s.getFirstName(), "Taylor");
        Assert.assertNull("Last name is not null", s.getLastName());
        Assertions.assertEquals(s.getSchool(), "school1");
        Assert.assertNull("Gender is not null", s.getGender());
        // patch a student, after patch we can get a male student named John Taylor and school is null.
        WebTarget patchTarget = client.target(generateURL("/students/1"));
        javax.json.JsonArray patchRequest = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("op", "copy").add("from", "/firstName").add("path", "/lastName").build())
                .add(Json.createObjectBuilder().add("op", "replace").add("path", "/firstName").add("value", "John").build())
                .add(Json.createObjectBuilder().add("op", "remove").add("path", "/school").build())
                .add(Json.createObjectBuilder().add("op", "add").add("path", "/gender").add("value", "male").build()).build();
        patchTarget.request().build(HttpMethod.PATCH, Entity.entity(patchRequest, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke();
        // verify the patch update result
        WebTarget getTarget = client.target(generateURL("/students/1"));
        Response getResponse = getTarget.request().get();
        Student patchedStudent = getResponse.readEntity(Student.class);
        Assertions.assertEquals("Taylor", patchedStudent.getLastName(), "Expected lastname is changed to Taylor");
        Assertions.assertEquals("John", patchedStudent.getFirstName(), "Expected firstname is replaced from Taylor to John");
        Assertions.assertEquals(null, patchedStudent.getSchool(), "Expected school is null");
        Assertions.assertEquals("male", patchedStudent.getGender(), "Add gender");
        client.close();
    }

    @Test
    @OperateOnDeployment(PATCH_DEPLOYMENT)
    @DisplayName("Test Merge Patch Student")
    public void testMergePatchStudent() throws Exception {
        QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).connectionPoolSize(10).build();
        WebTarget base = client.target(generateURL("/students"));
        Student newStudent = new Student().setId(2L).setFirstName("Alice").setSchool("school2");
        Response response = base.request().post(Entity.<Student> entity(newStudent, MediaType.APPLICATION_JSON_TYPE));
        Student s = response.readEntity(Student.class);
        Assertions.assertNotNull(s, "Add student failed");
        Assertions.assertEquals(s.getFirstName(), "Alice");
        Assert.assertNull("Last name is not null", s.getLastName());
        Assertions.assertEquals(s.getSchool(), "school2");
        Assert.assertNull("Gender is not null", s.getGender());
        WebTarget patchTarget = client.target(generateURL("/students/2"));
        JsonObject object = Json.createObjectBuilder().add("lastName", "Green").addNull("school").build();
        Response result = patchTarget.request().build(HttpMethod.PATCH, Entity.entity(object, "application/merge-patch+json"))
                .invoke();
        Student patchedStudent = result.readEntity(Student.class);
        Assertions.assertEquals("Green", patchedStudent.getLastName(), "Expected lastname is changed to Green");
        Assertions.assertEquals("Alice", patchedStudent.getFirstName(), "Expected firstname is Alice");
        Assertions.assertEquals(null, patchedStudent.getSchool(), "Expected school is null");
        Assertions.assertEquals(null, patchedStudent.getGender(), "Expected gender is null");
        client.close();
    }

    @Test
    @OperateOnDeployment(DISABLED_PATCH_DEPLOYMENT)
    @DisplayName("Test Patch Disabled")
    public void testPatchDisabled() throws Exception {
        QuarkusRestClient client = ((QuarkusRestClientBuilder) ClientBuilder.newBuilder()).connectionPoolSize(10).build();
        WebTarget base = client.target(PortProviderUtil.generateURL("/students", DISABLED_PATCH_DEPLOYMENT));
        // add a student, first name is Taylor and school is school1, other fields is null.
        Student newStudent = new Student().setId(1L).setFirstName("Taylor").setSchool("school1");
        Response response = base.request().post(Entity.<Student> entity(newStudent, MediaType.APPLICATION_JSON_TYPE));
        Student s = response.readEntity(Student.class);
        Assertions.assertNotNull(s, "Add student failed");
        Assertions.assertEquals(s.getFirstName(), "Taylor");
        Assert.assertNull("Last name is not null", s.getLastName());
        Assertions.assertEquals(s.getSchool(), "school1");
        Assert.assertNull("Gender is not null", s.getGender());
        WebTarget patchTarget = client.target(PortProviderUtil.generateURL("/students/1", DISABLED_PATCH_DEPLOYMENT));
        javax.json.JsonArray patchRequest = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("op", "copy").add("from", "/firstName").add("path", "/lastName").build())
                .add(Json.createObjectBuilder().add("op", "replace").add("path", "/firstName").add("value", "John").build())
                .build();
        Response res = patchTarget.request()
                .build(HttpMethod.PATCH, Entity.entity(patchRequest, MediaType.APPLICATION_JSON_PATCH_JSON)).invoke();
        Assertions.assertEquals(400, res.getStatus(), "Http 400 is expected");
        client.close();
    }
}
