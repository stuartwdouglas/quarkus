package io.quarkus.vertx.http.deployment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NonApplicationRootPathBuildItemTest {
    @Test
    void testResolvePathWithSlashRelativeQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q");

        Assertions.assertEquals("/q/", buildItem.resolvePath(""));
        Assertions.assertEquals("/q/extension", buildItem.resolvePath("extension"));
        Assertions.assertEquals("/q/extension/sub/path", buildItem.resolvePath("extension/sub/path"));
        Assertions.assertEquals("/extension", buildItem.resolvePath("/extension"));
        Assertions.assertEquals("/extension/sub/path", buildItem.resolvePath("/extension/sub/path"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../extension"));
    }

    @Test
    void testResolvePathWithSlashAbsoluteQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q");

        Assertions.assertEquals("/q/", buildItem.resolvePath(""));
        Assertions.assertEquals("/q/extension", buildItem.resolvePath("extension"));
        Assertions.assertEquals("/extension", buildItem.resolvePath("/extension"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../extension"));
    }

    @Test
    void testResolvePathWithSlashAppWithRelativeQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/app", "q");

        Assertions.assertEquals("/app/q/", buildItem.resolvePath(""));
        Assertions.assertEquals("/app/q/extension", buildItem.resolvePath("extension"));
        Assertions.assertEquals("/extension", buildItem.resolvePath("/extension"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../extension"));
    }

    @Test
    void testResolvePathWithSlashAppWithAbsoluteQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/app", "/q");

        Assertions.assertEquals("/q/", buildItem.resolvePath(""));
        Assertions.assertEquals("/q/extension", buildItem.resolvePath("extension"));
        Assertions.assertEquals("/extension", buildItem.resolvePath("/extension"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../extension"));
    }

    @Test
    void testResolvePathWithSlashEmpty() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "");

        Assertions.assertEquals("/", buildItem.resolvePath(""));
        Assertions.assertEquals("/extension", buildItem.resolvePath("extension"));
        Assertions.assertEquals("/extension", buildItem.resolvePath("/extension"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../extension"));
    }

    @Test
    void testResolvePathWithSlashWithSlash() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/");

        Assertions.assertEquals("/", buildItem.resolvePath(""));
        Assertions.assertEquals("/extension", buildItem.resolvePath("extension"));
        Assertions.assertEquals("/extension", buildItem.resolvePath("/extension"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../extension"));
    }
}
