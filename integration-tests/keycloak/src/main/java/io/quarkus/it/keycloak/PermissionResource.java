package io.quarkus.it.keycloak;

import javax.ws.rs.Path;

@Path("/api/permission")
public class PermissionResource {

    //    @Inject
    //    KeycloakSecurityContext keycloakSecurityContext;
    //
    //    @GET
    //    @Path("/{name}")
    //    @RolesAllowed("user")
    //    @Produces(MediaType.APPLICATION_JSON)
    //    public List<Permission> permissions() {
    //        return keycloakSecurityContext.getAuthorizationContext().getPermissions();
    //    }
}
