package io.quarkus.bootstrap.runner;

import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Experimental class, should not be commited
 */
public class AugmentEntryPoint {

    public static void main(String... args) throws Exception {
        System.setProperty("java.util.logging.manager", org.jboss.logmanager.LogManager.class.getName());
        Timing.staticInitStarted();

        String path = AugmentEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        Path appRoot = Paths.get(decodedPath).getParent().getParent();

        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(appRoot.resolve("deployment-quarkus/deployment-class-path.dat")))) {
            List<String> paths = (List<String>) in.readObject();
            List<URL> urls = paths.stream().map((s) -> {
                try {
                    return appRoot.resolve(s).toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            //yuck, should use runner class loader
            URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]));
            try {
                loader.loadClass("io.quarkus.deployment.mutability.MutabilityEntryPoint")
                        .getDeclaredMethod("main", Path.class, String[].class).invoke(null, appRoot, args);
            } finally {
                loader.close();
            }
        }

    }
}
