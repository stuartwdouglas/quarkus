package io.quarkus.rest.server.runtime.injection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.Sse;

import io.quarkus.rest.common.runtime.core.QuarkusRestContext;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.jaxrs.QuarkusRestResourceContext;
import io.quarkus.rest.server.runtime.jaxrs.QuarkusRestSse;
import io.quarkus.rest.server.runtime.spi.SimplifiedResourceInfo;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * Provides CDI producers for objects that can be injected via @Context
 * In quarkus-rest this works because @Context is considered an alias for @Inject
 * through the use of {@code AutoInjectAnnotationBuildItem}
 */
@Singleton
public class ContextProducers {

    // NOTE: Same list for parameters in ContextParamExtractor
    // and in EndpointIndexer.CONTEXT_TYPES

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @RequestScoped
    @Produces
    QuarkusRestContext quarkusRestContext() {
        return getContext();
    }

    @RequestScoped
    @Produces
    UriInfo uriInfo() {
        return getContext().getUriInfo();
    }

    @RequestScoped
    @Produces
    HttpHeaders headers() {
        return getContext().getHttpHeaders();
    }

    @ApplicationScoped
    @Produces
    Sse sse() {
        return QuarkusRestSse.INSTANCE;
    }

    @RequestScoped
    @Produces
    Request request() {
        return getContext().getRequest();
    }

    // HttpServerRequest, HttpServerRequest are Vert.x types so it's not necessary to have it injectable via @Context,
    // however we do use it in the Quickstarts so let's make it work

    @RequestScoped
    @Produces
    HttpServerRequest httpServerRequest() {
        return currentVertxRequest.getCurrent().request();
    }

    @RequestScoped
    @Produces
    HttpServerResponse httpServerResponse() {
        return currentVertxRequest.getCurrent().response();
    }

    @ApplicationScoped
    @Produces
    Providers providers() {
        return getContext().getProviders();
    }

    @RequestScoped
    @Produces
    ResourceInfo resourceInfo() {
        return getContext().getTarget().getLazyMethod();
    }

    @RequestScoped
    @Produces
    SimplifiedResourceInfo simplifiedResourceInfo() {
        return getContext().getTarget().getSimplifiedResourceInfo();
    }

    @ApplicationScoped
    @Produces
    Configuration config() {
        return getContext().getDeployment().getConfiguration();
    }

    @ApplicationScoped
    @Produces
    Application application() {
        return getContext().getDeployment().getApplicationSupplier().get();
    }

    @ApplicationScoped
    @Produces
    ResourceContext resourceContext() {
        return QuarkusRestResourceContext.INSTANCE;
    }

    @ApplicationScoped
    @Produces
    SecurityContext securityContext() {
        return getContext().getSecurityContext();
    }

    private QuarkusRestRequestContext getContext() {
        return (QuarkusRestRequestContext) currentVertxRequest.getOtherHttpContextObject();
    }
}
