package io.quarkus.rest.test.exception.resource;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.tracing.RESTEasyTracingLogger;
import org.jboss.resteasy.tracing.api.RESTEasyTracing;
import org.jboss.resteasy.tracing.api.RESTEasyTracingLevel;

import io.quarkus.rest.test.exception.ClosedResponseHandlingTest;

@Provider
@PreMatching
public class ClosedResponseHandlingEnableTracingRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        // force verbose tracing, enabling via finishContainerPrepare()'s contextParams didn't work
        containerRequestContext.setProperty(RESTEasyTracing.PROPERTY_NAME,
                RESTEasyTracingLogger.create(this.toString(), RESTEasyTracingLevel.VERBOSE.name(),
                        ClosedResponseHandlingTest.class.getSimpleName()));
    }
}
