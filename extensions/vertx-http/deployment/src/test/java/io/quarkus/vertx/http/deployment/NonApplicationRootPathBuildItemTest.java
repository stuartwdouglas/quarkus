package io.quarkus.vertx.http.deployment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NonApplicationRootPathBuildItemTest {
    @Test
    void testResolvePathWithSlashRelativeQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q");

        Assertions.assertEquals("/q/", buildItem.resolvePath(""));
        Assertions.assertEquals("/q/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/q/foo/sub/path", buildItem.resolvePath("foo/sub/path"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
        Assertions.assertEquals("/foo/sub/path", buildItem.resolvePath("/foo/sub/path"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../foo"));
    }

    @Test
    void testResolvePathWithSlashAbsoluteQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q");

        Assertions.assertEquals("/q/", buildItem.resolvePath(""));
        Assertions.assertEquals("/q/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../foo"));
    }

    @Test
    void testResolvePathWithSlashAppWithRelativeQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/app", "q");

        Assertions.assertEquals("/app/q/", buildItem.resolvePath(""));
        Assertions.assertEquals("/app/q/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../foo"));
    }

    @Test
    void testResolvePathWithSlashAppWithAbsoluteQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/app", "/q");

        Assertions.assertEquals("/q/", buildItem.resolvePath(""));
        Assertions.assertEquals("/q/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../foo"));
    }

    @Test
    void testResolvePathWithSlashEmpty() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "");

        Assertions.assertEquals("/", buildItem.resolvePath(""));
        Assertions.assertEquals("/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../foo"));
    }

    @Test
    void testResolvePathWithSlashWithSlash() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/");

        Assertions.assertEquals("/", buildItem.resolvePath(""));
        Assertions.assertEquals("/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../foo"));
    }
}
