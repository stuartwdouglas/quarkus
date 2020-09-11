package io.quarkus.rest.test.security.resource;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import javax.ws.rs.core.Response.Status;

@Provider
@PreMatching
public class SecurityContextContainerRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        SecurityContext securityContext = requestContext.getSecurityContext();
        if (!securityContext.isUserInRole("admin")) {
            requestContext.abortWith(Response.status(Status.UNAUTHORIZED)
                    .entity("User ordinaryUser is not authorized, coming from filter").build());
        }
    }
}
