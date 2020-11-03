package io.quarkus.vertx.http.deployment.devmode;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;

import org.yaml.snakeyaml.Yaml;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.qute.Engine;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Results;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.ValueResolvers;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class DevConsole implements Handler<RoutingContext> {

    Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers()
            .addValueResolver(new ReflectionValueResolver())
            .addValueResolver(ValueResolvers.rawResolver())
            .addNamespaceResolver(NamespaceResolver.builder("inject").resolve(ctx -> {
                Object result = DevConsoleManager.resolve(ctx.getName());
                return result == null ? Results.Result.NOT_FOUND : result;
            }).build()).build();

    @Override
    public void handle(RoutingContext event) {
        try {
            String path = event.normalisedPath().substring(event.mountPoint().length());
            if (path.isEmpty() || path.equals("/")) {
                sendMainPage(event);
            } else {
                URL url = getClass().getClassLoader().getResource("/dev-templates/" + path + ".html");
                if (url != null) {
                    Template template = readTemplate(url);
                    event.response().setStatusCode(200).headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                    renderTemplate(event, template.instance());
                }
                event.response().setStatusCode(404).end();
            }
        } catch (IOException e) {
            event.fail(e);
        }
    }

    protected void renderTemplate(RoutingContext event, TemplateInstance template) {
        template.renderAsync().handle(new BiFunction<String, Throwable, Object>() {
            @Override
            public Object apply(String s, Throwable throwable) {
                if (throwable != null) {
                    event.fail(throwable);
                } else {
                    event.response().end(s);
                }
                return null;
            }
        });
    }

    public void sendMainPage(RoutingContext event) {
        try {
            Template devTemplate = readTemplate("/dev-templates/index.html");
            Enumeration<URL> extensionDescriptors = getClass().getClassLoader()
                    .getResources("/META-INF/quarkus-extension.yaml");
            List<Map<String, Object>> extensions = new ArrayList<>();
            Yaml yaml = new Yaml();
            while (extensionDescriptors.hasMoreElements()) {
                URL extensionDescriptor = extensionDescriptors.nextElement();
                String desc = readURL(extensionDescriptor);
                Map<String, Object> loaded = yaml.load(desc);
                String artifactId = (String) loaded.get("artifact-id");
                URL extensionSimple = getClass().getClassLoader().getResource("/dev-templates/" + artifactId + ".html");
                if (extensionSimple != null) {
                    Template template = readTemplate(extensionSimple);
                    String result = template.render();
                    loaded.put("_dev", result);
                }
                extensions.add(loaded);
            }
            Collections.sort(extensions, (a, b) -> {
                return ((String) a.get("name")).compareTo((String) b.get("name"));
            });
            TemplateInstance instance = devTemplate.data("extensions", extensions);
            renderTemplate(event, instance);
        } catch (IOException e) {
            event.fail(e);

        }
    }

    private Template readTemplate(String path) throws IOException {
        URL url = getClass().getClassLoader().getResource(path);
        return readTemplate(url);
    }

    private Template readTemplate(URL url) throws IOException {
        return engine.parse(readURL(url));
    }

    private String readURL(URL url) throws IOException {
        try (Scanner scanner = new Scanner(url.openStream(),
                StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            String templateBody = scanner.hasNext() ? scanner.next() : null;
            return templateBody;
        }
    }

}
