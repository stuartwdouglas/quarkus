package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import javax.enterprise.inject.Typed;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * tests that inherited @QuarkusTest instances work correctly
 */
@QuarkusTest
@Typed(TestInheritanceTestCase.class)
public class TestInheritanceTestCase extends TestInheritanceSuperclassTestCase {

    @Test
    public void testServlet() {
        RestAssured.when().get("/testservlet").then()
                .body(is("A message"));
    }

}
