package io.quarkus.it.resteasy.reactive.kotlin

import kotlinx.coroutines.delay
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/hello-resteasy-reactive")
class ReactiveGreetingResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    suspend fun hello(): String {
        delay(50)
        return "Hello RestEASY Reactive"
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/standard")
    fun standard(): String {
        return "Hello RestEASY Reactive"
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}")
    suspend fun hello(name:String): String {
        delay(50)
        return "Hello ${name}"
    }

}
