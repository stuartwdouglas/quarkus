package io.quarkus.launcher;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;

public class QuarkusLauncher {

    public static void launch(String callingClass, String quarkusApplication, Consumer<Integer> exitHandler, String... args) {

        String classResource = callingClass.replace(".", "/") + ".class";
        URL resource = Thread.currentThread().getContextClassLoader().getResource(classResource);
        String path = resource.getPath();
        path = path.substring(0, path.length() - classResource.length());

        Path appClasses = Paths.get(path);

        try {
            CuratedApplication app = QuarkusBootstrap.builder(appClasses)
                    .setBaseClassLoader(QuarkusLauncher.class.getClassLoader())
                    .setMode(QuarkusBootstrap.Mode.DEV)
                    .build().bootstrap();

        } catch (BootstrapException e) {
            throw new RuntimeException(e);
        }
    }

}
