package io.quarkus.it.main;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PropertyInjectionTestCase {

    @ConfigProperty(name = "web-message")
    String message;

    @Test
    public void testConfigPropertyInjectedIntoTest() {
        Assertions.assertEquals("A message", message);
    }
}
