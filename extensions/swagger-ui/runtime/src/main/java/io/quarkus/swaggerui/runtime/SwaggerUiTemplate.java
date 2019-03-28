package io.quarkus.swaggerui.runtime;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;
import io.undertow.servlet.ServletExtension;

@Template
public class SwaggerUiTemplate {

    private static final Logger log = Logger.getLogger(SwaggerUiTemplate.class.getName());

    public ServletExtension createSwaggerUiExtension(String path, String resourceDir, BeanContainer container) {
        SwaggerUiServletExtension extension = container.instance(SwaggerUiServletExtension.class);
        extension.setPath(path);
        extension.setResourceDir(resourceDir);
        return extension;
    }
}
