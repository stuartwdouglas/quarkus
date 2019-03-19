package io.quarkus.swaggerui.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;
import io.undertow.servlet.ServletExtension;

@Template
public class SwaggerUiTemplate {

    private static final Logger log = Logger.getLogger(SwaggerUiTemplate.class.getName());

    public ServletExtension createSwaggerUiExtension(String path, String resourceDir, BeanContainer container,
            ShutdownContext shutdown) {
        SwaggerUiServletExtension extension = container.instance(SwaggerUiServletExtension.class);
        extension.setPath(path);
        extension.setResourceDir(resourceDir);
        shutdown.addShutdownTask(() -> cleanup(resourceDir));
        return extension;
    }

    private void cleanup(String resourceDir) {
        try {
            Files.walk(Paths.get(resourceDir))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
