package io.quarkus.it.main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.config.ConfigPropertiesResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.web.WebTest;

@QuarkusTest
@TestHTTPEndpoint(ConfigPropertiesResource.class)
public class ConfigPropertiesTestCase {

    @Test
    @WebTest
    public void testConfigPropertiesProperlyInjected(String body) {
        Assertions.assertEquals("HelloONE!", body);
    }
}
