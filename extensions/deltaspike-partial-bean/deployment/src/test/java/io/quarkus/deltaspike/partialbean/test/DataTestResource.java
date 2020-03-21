package io.quarkus.deltaspike.partialbean.test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/orm-sql-load-script")
public class DataTestResource {

    @Inject
    EntityManager em;

    @Inject
    DataRepository dataRepository;

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName(@PathParam("id") long id) {
        MyEntity entity = dataRepository.find(id);
        if (entity != null) {
            return entity.getName();
        }

        return "no entity";
    }
}
