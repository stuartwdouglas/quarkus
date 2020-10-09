package io.quarkus.it.main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.corestuff.CustomConfigTesting;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.web.WebTest;

@QuarkusTest
@TestHTTPEndpoint(CustomConfigTesting.class)
public class CustomConfigSourceTestCase {

    @Test
    @WebTest
    public void testCustomConfig(String body) {
        Assertions.assertEquals("OK", body);
    }
}
