package io.quarkus.swaggerui.deployment;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.index.ClassPathArtifactResolver;
import io.quarkus.deployment.index.ResolvedArtifact;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.openapi.deployment.SmallRyeOpenApiProcessor;
import io.quarkus.swaggerui.runtime.SwaggerUiTemplate;
import io.quarkus.undertow.deployment.ServletExtensionBuildItem;

public class SwaggerUiProcessor {

    private static final Logger log = Logger.getLogger(SwaggerUiProcessor.class.getName());

    private static final String SWAGGER_UI_WEBJAR_GROUP_ID = "org.webjars";
    private static final String SWAGGER_UI_WEBJAR_ARTIFACT_ID = "swagger-ui";
    private static final String SWAGGER_UI_WEBJAR_PREFIX = "META-INF/resources/webjars/swagger-ui";
    private static final String SWAGGER_UI_DEFAULT_API_URL = "https://petstore.swagger.io/v2/swagger.json";
    private static final String OPEN_API_DEFAULT_URL = "/openapi";
    private static final String TEMP_DIR_PREFIX = "quarkus-swagger-ui_" + System.nanoTime();

    /**
     * The configuration for swagger-ui.
     */
    private SwaggerUiConfig swaggerUiConfig;

    @Inject
    private LaunchModeBuildItem launch;

    /**
     * Register this extension as a swagger-ui feature
     *
     * @return
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SWAGGER_UI);
    }

    SmallRyeOpenApiProcessor.SmallRyeOpenApiConfig openapi;

    private static String cachedOpenAPIPath;
    private static String cachedDirectory;

    /**
     * Register the Swagger UI servlet extension
     *
     * @param template - Swagger UI runtime template
     * @param container - the BeanContainer for creating CDI beans
     * @return servlet extension build item
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerSwaggerUiServletExtension(SwaggerUiTemplate template,
            BuildProducer<ServletExtensionBuildItem> servletExtension,
            BeanContainerBuildItem container) {
        if (launch.getLaunchMode().isDevOrTest()) {
            if (cachedDirectory == null) {
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FileUtils.deleteDirectory(new File(cachedDirectory));
                        } catch (IOException e) {
                            log.error("Failed to clean swagger UI on shutdown", e);
                        }
                    }
                }, "Swagger UI Shutdown Hook"));
            }
            if (cachedDirectory == null || !cachedOpenAPIPath.equals(openapi.path)) {
                if (cachedDirectory != null) {
                    try {
                        FileUtils.deleteDirectory(new File(cachedDirectory));
                    } catch (IOException e) {
                        log.error("Failed to clean swagger UI on shutdown", e);
                    }
                    cachedDirectory = null;
                    cachedOpenAPIPath = null;
                }
            }
            try {
                ResolvedArtifact artifact = getSwaggerUiArtifact();
                Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
                extractSwaggerUi(artifact, tempDir);
                updateApiUrl(tempDir.resolve("index.html"));
                cachedDirectory = tempDir.toAbsolutePath().toString();
                cachedOpenAPIPath = openapi.path;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            servletExtension.produce(
                    new ServletExtensionBuildItem(
                            template.createSwaggerUiExtension(
                                    swaggerUiConfig.path,
                                    cachedDirectory,
                                    container.getValue())));
        }
    }

    private ResolvedArtifact getSwaggerUiArtifact() {
        ClassPathArtifactResolver resolver = new ClassPathArtifactResolver(SwaggerUiProcessor.class.getClassLoader());
        return resolver.getArtifact(SWAGGER_UI_WEBJAR_GROUP_ID, SWAGGER_UI_WEBJAR_ARTIFACT_ID, null);
    }

    private void extractSwaggerUi(ResolvedArtifact artifact, Path resourceDir) throws IOException {
        File artifactFile = artifact.getArtifactPath().toFile();
        JarFile jarFile = new JarFile(artifactFile);
        Enumeration<JarEntry> entries = jarFile.entries();
        String versionedSwaggerUiWebjarPrefix = format("%s/%s/", SWAGGER_UI_WEBJAR_PREFIX, artifact.getVersion());
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith(versionedSwaggerUiWebjarPrefix) && !entry.isDirectory()) {
                InputStream inputStream = jarFile.getInputStream(entry);
                String filename = entry.getName().replace(versionedSwaggerUiWebjarPrefix, "");
                Files.copy(inputStream, resourceDir.resolve(filename));
            }
        }
    }

    private void updateApiUrl(Path indexHtml) throws IOException {
        String content = new String(Files.readAllBytes(indexHtml));
        content = content.replaceAll(SWAGGER_UI_DEFAULT_API_URL, openapi.path);
        Files.write(indexHtml, content.getBytes());
    }

    @ConfigRoot
    static final class SwaggerUiConfig {
        /**
         * The path of the swagger-ui servlet.
         */
        @ConfigItem(defaultValue = "/swagger-ui")
        String path;
    }
}
