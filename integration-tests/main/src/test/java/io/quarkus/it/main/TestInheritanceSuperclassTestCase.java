package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class TestInheritanceSuperclassTestCase {

    @Inject
    EntityManager entityManager;

    @AfterEach
    @Transactional
    void afterEach() {
        entityManager.createQuery("delete Person").executeUpdate();
    }

    @Test
    public void superclassTest() {
        RestAssured.when().get("/testservlet").then()
                .body(is("A message"));
    }

}
