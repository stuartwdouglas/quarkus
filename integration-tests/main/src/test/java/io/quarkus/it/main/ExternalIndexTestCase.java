package io.quarkus.it.main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.shared.SharedResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.web.WebTest;

@QuarkusTest
@TestHTTPEndpoint(SharedResource.class)
public class ExternalIndexTestCase {

    @Test
    @WebTest
    public void testJAXRSResourceFromExternalLibrary(String body) {
        Assertions.assertEquals("Shared Resource", body);
    }

    @Test
    @WebTest("/transformed")
    public void testTransformedExternalResources(String body) {
        Assertions.assertEquals("Transformed Endpoint", body);
    }
}
