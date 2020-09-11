package io.quarkus.rest.test.providers.jaxb.resource;

import javax.ejb.Stateless;
import javax.ws.rs.Path;

@Path("test2")
@Stateless
public class GenericResourceResource2 extends GenericResourceAbstractResource<GenericResourceModel> {

}
