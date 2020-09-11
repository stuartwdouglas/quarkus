package io.quarkus.rest.test.cdi.basic.resource;

import javax.ejb.Local;
import javax.ws.rs.Path;

@Local
@Path("/")
public interface EJBLocalResource extends EJBResourceParent {
}
