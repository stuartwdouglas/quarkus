package io.quarkus.rest.test;

import org.junit.jupiter.api.Assertions;

public class Assert {
    public static void assertEquals(String message, Object i, Object i1) {
        Assertions.assertEquals(i, i1, message);
    }

    public static void assertTrue(String message, boolean contains) {
        Assertions.assertTrue(contains, message);
    }

    public static void assertEquals(Object o, Object a) {
        Assertions.assertEquals(o, a);
    }

    public static void assertNotNull(Object link) {
        Assertions.assertNotNull(link);
    }
}
