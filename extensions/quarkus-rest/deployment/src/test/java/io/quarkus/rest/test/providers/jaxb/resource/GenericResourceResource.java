package io.quarkus.rest.test.providers.jaxb.resource;

import javax.ejb.Stateless;
import javax.ws.rs.Path;

@Stateless
@Path("/test")
public class GenericResourceResource extends GenericResourceOtherAbstractResource<GenericResourceModel> {

}
