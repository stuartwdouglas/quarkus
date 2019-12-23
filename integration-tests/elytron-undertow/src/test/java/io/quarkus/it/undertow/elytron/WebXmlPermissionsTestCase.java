package io.quarkus.it.undertow.elytron;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class WebXmlPermissionsTestCase {

    @Test
    public void testPost() {
        // This is a regression test in that we had a problem where the Vert.x request was not paused
        // before the authentication filters ran and the post message was thrown away by Vert.x because
        // RESTEasy hadn't registered its request handlers yet.
        given()
                .header("Authorization", "Basic am9objpqb2hu")
                .body("Bill")
                .contentType(ContentType.TEXT)
                .when()
                .post("/foo/")
                .then()
                .statusCode(200)
                .body(is("hello Bill"));
    }

    @Test
    public void testOpenApiNoPermissions() {
        given()
                .when()
                .get("/foo/openapi")
                .then()
                .statusCode(401);
    }

    @Test
    public void testOpenApiWithWrongAuth() {
        given()
                .header("Authorization", "Basic am9objpqb2hu")
                .when()
                .get("/foo/openapi")
                .then()
                .statusCode(403);
    }

    @Test
    public void testOpenApiWithAuth() {
        given()
                .auth()
                .basic("mary", "mary")
                .when()
                .get("/foo/openapi")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSecuredServletWithWrongAuth() {
        given()
                .header("Authorization", "Basic am9objpqb2hu")
                .when()
                .get("/foo/secure/a")
                .then()
                .statusCode(403);
    }

    @Test
    public void testSecuredServletWithNoAuth() {
        given()
                .when()
                .get("/foo/secure/a")
                .then()
                .statusCode(401);
    }

    @Test
    public void testSecuredServletWithAuth() {
        given()
                .auth()
                .basic("mary", "mary")
                .when()
                .get("/foo/secure/a")
                .then()
                .statusCode(200);
    }
}
