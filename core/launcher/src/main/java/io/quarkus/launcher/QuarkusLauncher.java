package io.quarkus.launcher;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.bootstrap.BootstrapClassLoaderFactory;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.util.PropertyUtils;

public class QuarkusLauncher {

    public static void main(String... params) throws Exception {

        Path root = Paths.get("");
        //some trickery, get the class that has invoked us, and use this to figure out the
        //classes root
        String callingClass = Thread.currentThread().getStackTrace()[2].getClassName();
        String classResource = callingClass.replace(".", "/") + ".class";
        URL resource = Thread.currentThread().getContextClassLoader().getResource(classResource);
        String path = resource.getPath();
        path = path.substring(0, path.length() - classResource.length());

        //more hacks, we need to figure out the current version
        resource = Thread.currentThread().getContextClassLoader()
                .getResource(QuarkusLauncher.class.getName().replace(".", "/") + ".class");
        Matcher m = Pattern.compile("quarkus-launcher-(.*?)\\.jar").matcher(resource.getPath());
        m.find();
        String version = m.group(1);

        AppDependency devMode = new AppDependency(new AppArtifact("io.quarkus", "quarkus-development-mode", version),
                "compile");

        Path wiringDir = Files.createTempDirectory("quarkus-wiring");

        Path appClasses = Paths.get(path);
        BootstrapClassLoaderFactory loc = BootstrapClassLoaderFactory.newInstance()
                .setAppClasses(appClasses)
                .addAdditionalDependency(devMode)
                .addToClassPath(wiringDir)
                .setParent(QuarkusLauncher.class.getClassLoader())
                .setOffline(PropertyUtils.getBooleanOrNull(BootstrapClassLoaderFactory.PROP_OFFLINE))
                .setLocalProjectsDiscovery(
                        PropertyUtils.getBoolean(BootstrapClassLoaderFactory.PROP_WS_DISCOVERY, true))
                .setEnableClasspathCache(PropertyUtils.getBoolean(BootstrapClassLoaderFactory.PROP_CP_CACHE, true));

        ClassLoader loader = loc.newAllInclusiveClassLoader(false, false);
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> launcherClass = loader.loadClass("io.quarkus.dev.LauncherMain");
        Method main = launcherClass.getMethod("main", Path.class, Path.class, URL[].class, String[].class);
        main.invoke(null, appClasses, wiringDir, loc.newAllInclusiveClassPath(), (Object) params);
    }

}
