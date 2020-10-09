package io.quarkus.it.main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.config.ApplicationInfoResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.web.WebTest;

@QuarkusTest
@TestHTTPEndpoint(ApplicationInfoResource.class)
public class ApplicationInfoTestCase {

    @Test
    @WebTest
    public void testConfigPropertiesProperlyInjected(String body) {
        Assertions.assertEquals("main-integration-test/1.0/test", body);
    }
}
