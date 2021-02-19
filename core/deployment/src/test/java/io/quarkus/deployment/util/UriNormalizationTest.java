package io.quarkus.deployment.util;

import java.net.URI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UriNormalizationTest {
    @Test
    void testNormalizeSlashRelativePaths() {
        URI root = UriNormalizationUtil.toURI("/", true);
        Assertions.assertEquals("/", UriNormalizationUtil.toURI("./", true).getPath());
        Assertions.assertEquals("/", UriNormalizationUtil.normalizeWithBase(root, "#metrics", false).getPath());
        Assertions.assertEquals("/", UriNormalizationUtil.normalizeWithBase(root, "?metrics", false).getPath());

        URI app = UriNormalizationUtil.toURI("/app", true);
        URI q = UriNormalizationUtil.toURI("/app/q", true);

        Assertions.assertEquals("/metrics", UriNormalizationUtil.normalizeWithBase(root, "metrics", false).getPath());
        Assertions.assertEquals("/metrics", UriNormalizationUtil.normalizeWithBase(root, "./metrics", false).getPath());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "junk/../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "junk/../../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "../metrics", false));

        Assertions.assertEquals("/app/metrics", UriNormalizationUtil.normalizeWithBase(app, "metrics", false).getPath());
        Assertions.assertEquals("/app/metrics", UriNormalizationUtil.normalizeWithBase(app, "./metrics", false).getPath());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(app, "junk/../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(app, "junk/../../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(app, "../metrics", false));

        Assertions.assertEquals("/app/q/metrics", UriNormalizationUtil.normalizeWithBase(q, "metrics", false).getPath());
        Assertions.assertEquals("/app/q/metrics", UriNormalizationUtil.normalizeWithBase(q, "./metrics", false).getPath());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(q, "junk/../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(q, "junk/../../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(q, "../metrics", false));
    }

    @Test
    void normalizeAbsolutePaths() {
        URI root = UriNormalizationUtil.toURI("/", true);
        URI app = UriNormalizationUtil.toURI("/app", true);
        URI q = UriNormalizationUtil.toURI("/app/q", true);

        Assertions.assertEquals("/metrics", UriNormalizationUtil.normalizeWithBase(root, "/metrics", false).getPath());
        Assertions.assertEquals("/metrics", UriNormalizationUtil.normalizeWithBase(root, "//metrics", false).getPath());
        Assertions.assertEquals("/metrics", UriNormalizationUtil.normalizeWithBase(root, "/metrics#", false).getPath());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "/%2fmetrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "/junk/../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "/junk/../../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "/../metrics", false));

        Assertions.assertEquals("/metrics", UriNormalizationUtil.normalizeWithBase(app, "/metrics", false).getPath());
        Assertions.assertEquals("/metrics", UriNormalizationUtil.normalizeWithBase(app, "//metrics", false).getPath());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(app, "/%2fmetrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(app, "/junk/../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(app, "/junk/../../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(app, "/../metrics", false));

        Assertions.assertEquals("/metrics", UriNormalizationUtil.normalizeWithBase(q, "/metrics", false).getPath());
        Assertions.assertEquals("/metrics", UriNormalizationUtil.normalizeWithBase(q, "//metrics", false).getPath());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(q, "/%2fmetrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(q, "/junk/../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(q, "/junk/../../metrics", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(q, "/../metrics", false));
    }

    @Test
    void testNormalizeRelativePaths() {
        URI root = UriNormalizationUtil.toURI("bob", true);
        Assertions.assertEquals("bob/", UriNormalizationUtil.normalizeWithBase(root, "#metrics", false).getPath());
        Assertions.assertEquals("bob/", UriNormalizationUtil.normalizeWithBase(root, "?metrics", false).getPath());
        Assertions.assertEquals("bob/george", UriNormalizationUtil.normalizeWithBase(root, "george", false).getPath());
        Assertions.assertEquals("/george", UriNormalizationUtil.normalizeWithBase(root, "/george", false).getPath());
    }
}
