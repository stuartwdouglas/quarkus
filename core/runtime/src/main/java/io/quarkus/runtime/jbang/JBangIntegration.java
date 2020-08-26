package io.quarkus.runtime.jbang;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.quarkus.launcher.JBangRuntimeBuilder;

public class JBangIntegration {

    public static final String CONFIG = "//CONFIG";

    public static Map<String, byte[]> postBuild(Path classesDir, Path pomFile, List<Map.Entry<String, Path>> dependencies,
            List<String> comments) {
        for (String comment : comments) {
            //we allow config to be provided via //CONFIG name=value
            if (comment.startsWith(CONFIG)) {
                String conf = comment.substring(CONFIG.length()).trim();
                int equals = conf.indexOf("=");
                if (equals == -1) {
                    throw new RuntimeException("invalid config  " + comment);
                }
                System.setProperty(conf.substring(0, equals), conf.substring(equals + 1));
            }
        }

        return JBangRuntimeBuilder.postBuild(classesDir, pomFile, dependencies);
    }

}
