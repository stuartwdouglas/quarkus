package io.quarkus.vertx.http.deployment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpRootPathBuildItemTest {
    @Test
    void testResolvePathWithSlash() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");

        Assertions.assertEquals("/", buildItem.resolvePath(""));
        Assertions.assertEquals("/extension", buildItem.resolvePath("extension"));
        Assertions.assertEquals("/extension", buildItem.resolvePath("/extension"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../extension"));
    }

    @Test
    void testResolvePathWithSlashApp() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");

        Assertions.assertEquals("/app/", buildItem.resolvePath(""));
        Assertions.assertEquals("/app/extension", buildItem.resolvePath("extension"));
        Assertions.assertEquals("/extension", buildItem.resolvePath("/extension"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../extension"));
    }
}
