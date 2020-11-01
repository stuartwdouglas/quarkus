package io.quarkus.rest.test.simple;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;

import io.quarkus.rest.Blocking;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

@Path("/simple")
public class SimpleQuarkusRestResource {

    @Inject
    HelloService service;

    @GET
    public String get() {
        return "GET";
    }

    @Path("sub")
    public Object subResource() {
        return new SubResource();
    }

    @GET
    @Path("/hello")
    public String hello() {
        return service.sayHello();
    }

    @GET
    @Path("{id}")
    public String get(@PathParam("id") String id) {
        return "GET:" + id;
    }

    @POST
    @Path("params/{p}")
    public String params(@PathParam("p") String p,
            @QueryParam("q") String q,
            @HeaderParam("h") int h,
            @FormParam("f") String f) {
        return "params: p: " + p + ", q: " + q + ", h: " + h + ", f: " + f;
    }

    @POST
    public String post() {
        return "POST";
    }

    @DELETE
    public String delete() {
        return "DELETE";
    }

    @PUT
    public String put() {
        return "PUT";
    }

    @PATCH
    public String patch() {
        return "PATCH";
    }

    @OPTIONS
    public String options() {
        return "OPTIONS";
    }

    @HEAD
    public Response head() {
        return Response.ok().header("Stef", "head").build();
    }

    @GET
    @Path("/person")
    @Produces(MediaType.APPLICATION_JSON)
    public Person getPerson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        return person;
    }

    @GET
    @Path("/async-person")
    @Produces(MediaType.APPLICATION_JSON)
    public void getPerson(@Suspended AsyncResponse response) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Person person = new Person();
                person.setFirst("Bob");
                person.setLast("Builder");
                response.resume(person);
            }
        }).start();
    }

    @POST
    @Path("/person")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person getPerson(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return person;
    }

    @POST
    @Path("/person-large")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person personTest(Person person) {
        //large requests should get bumped from the IO thread
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should have dispatched");
        }
        return person;
    }

    @POST
    @Path("/person-validated")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person getValidatedPerson(@Valid Person person) {
        return person;
    }

    @POST
    @Path("/person-invalid-result")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Valid
    public Person getInvalidPersonResult(@Valid Person person) {
        person.setLast(null);
        return person;
    }

    @GET
    @Path("/blocking")
    @Blocking
    public String blocking() {
        return String.valueOf(BlockingOperationControl.isBlockingAllowed());
    }

    @GET
    @Path("providers")
    public Response filters(@Context Providers providers) {
        // TODO: enhance this test
        return Response.ok().entity(providers.getExceptionMapper(TestException.class).getClass().getName()).build();
    }

    @GET
    @Path("filters")
    public Response filters(@Context HttpHeaders headers) {
        return Response.ok().header("filter-request", headers.getHeaderString("filter-request")).build();
    }

    @GET
    @Path("feature-filters")
    public Response featureFilters(@Context HttpHeaders headers) {
        return Response.ok().header("feature-filter-request", headers.getHeaderString("feature-filter-request")).build();
    }

    @GET
    @Path("dynamic-feature-filters")
    public Response dynamicFeatureFilters(@Context HttpHeaders headers) {
        return Response.ok().header("feature-filter-request", headers.getHeaderString("feature-filter-request")).build();
    }

    @GET
    @Path("fooFilters")
    @Foo
    public Response fooFilters(@Context HttpHeaders headers) {
        return Response.ok().header("filter-request", headers.getHeaderString("filter-request")).build();
    }

    @GET
    @Path("barFilters")
    @Bar
    public Response barFilters(@Context HttpHeaders headers) {
        return Response.ok().header("filter-request", headers.getHeaderString("filter-request")).build();
    }

    @GET
    @Path("fooBarFilters")
    @Foo
    @Bar
    public Response fooBarFilters(@Context HttpHeaders headers) {
        return Response.ok().header("filter-request", headers.getHeaderString("filter-request")).build();
    }

    @GET
    @Path("mapped-exception")
    public String mappedException() {
        throw new TestException();
    }

    @GET
    @Path("feature-mapped-exception")
    public String featureMappedException() {
        throw new FeatureMappedException();
    }

    @GET
    @Path("unknown-exception")
    public String unknownException() {
        throw new RuntimeException("OUCH");
    }

    @GET
    @Path("web-application-exception")
    public String webApplicationException() {
        throw new WebApplicationException(Response.status(666).entity("OK").build());
    }

    @GET
    @Path("writer")
    public TestClass writer() {
        return new TestClass();
    }

    @GET
    @Path("fast-writer")
    @Produces("text/plain")
    public String fastWriter(@Context QuarkusRestRequestContext context) {
        return "OK";
    }

    @GET
    @Path("lookup-writer")
    public Object slowWriter(@Context QuarkusRestRequestContext context) {
        return "OK";
    }

    @GET
    @Path("writer/vertx-buffer")
    public Buffer vertxBuffer(@Context QuarkusRestRequestContext context) {
        return Buffer.buffer("VERTX-BUFFER");
    }

    @GET
    @Path("async/cs/ok")
    public CompletionStage<String> asyncCompletionStageOK() {
        return CompletableFuture.completedFuture("CS-OK");
    }

    @GET
    @Path("async/cs/fail")
    public CompletionStage<String> asyncCompletionStageFail() {
        CompletableFuture<String> ret = new CompletableFuture<>();
        ret.completeExceptionally(new TestException());
        return ret;
    }

    @GET
    @Path("async/uni/ok")
    public Uni<String> asyncUniOK() {
        return Uni.createFrom().item("UNI-OK");
    }

    @GET
    @Path("async/uni/fail")
    public Uni<String> asyncUniStageFail() {
        return Uni.createFrom().failure(new TestException());
    }

    @GET
    @Path("pre-match")
    public String preMatchGet() {
        return "pre-match-get";
    }

    @POST
    @Path("pre-match")
    public String preMatchPost() {
        return "pre-match-post";
    }

    @GET
    @Path("request-response-params")
    public String requestAndResponseParams(@Context HttpServerRequest request, @Context HttpServerResponse response) {
        response.headers().add("dummy", "value");
        return request.remoteAddress().host();
    }

    @GET
    @Path("jax-rs-request")
    public String jaxRsRequest(@Context Request request) {
        return request.getMethod();
    }

    @GET
    @Path("resource-info")
    public Response resourceInfo(@Context ResourceInfo resourceInfo, @Context HttpHeaders headers) {
        return Response.ok()
                .header("class-name", resourceInfo.getResourceClass().getSimpleName())
                .header("method-name", headers.getHeaderString("method-name"))
                .build();
    }
}
