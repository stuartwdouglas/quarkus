package io.quarkus.bootstrap.runner;

import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;

public class QuarkusEntryPoint {

    public static final String QUARKUS_APPLICATION_DAT = "quarkus/quarkus-application.dat";

    public static void main(String... args) throws Throwable {
        System.setProperty("java.util.logging.manager", org.jboss.logmanager.LogManager.class.getName());
        Timing.staticInitStarted();
        SerializedApplication app = null;
        try (InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(QUARKUS_APPLICATION_DAT)) {
            String path = QuarkusEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            app = SerializedApplication.read(in, new File(decodedPath).toPath().getParent().getParent());
            Thread.currentThread().setContextClassLoader(app.getRunnerClassLoader());
            Class<?> mainClass = app.getRunnerClassLoader().loadClass(app.getMainClass());
            mainClass.getMethod("main", String[].class).invoke(null, args);
        } finally {
            if (app != null) {
                app.getRunnerClassLoader().close();
            }
        }
    }
}
