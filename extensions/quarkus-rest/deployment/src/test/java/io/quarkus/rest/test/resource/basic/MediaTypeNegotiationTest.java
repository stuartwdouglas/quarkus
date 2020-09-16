package io.quarkus.rest.test.resource.basic;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.resteasy.core.NoMessageBodyWriterFoundFailure;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

@DisplayName("Media Type Negotiation Test")
public class MediaTypeNegotiationTest {

    @XmlRootElement
    @DisplayName("Message")
    public static class Message {

        private int status;

        private String message;

        public Message() {
        }

        public int getStatus() {
            return this.status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getMessage() {
            return this.message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @Path("echo")
    @DisplayName("Echo Resource")
    public static class EchoResource {

        @Produces({ "*/xml" })
        @GET
        public Response echo(@QueryParam("msg") String msg) {
            Message message = new Message();
            message.setStatus(Status.OK.getStatusCode());
            message.setMessage(String.valueOf(msg));
            return Response.ok(message).build();
        }

        @Produces({ "foo/bar" })
        @GET
        @Path("missingMBW")
        public Response echoMissingMBW(@QueryParam("msg") String msg) {
            Message message = new Message();
            message.setStatus(Status.OK.getStatusCode());
            message.setMessage(String.valueOf(msg));
            return Response.ok(message).build();
        }
    }

    @DisplayName("Not Found Exception Mapper")
    public static class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

        @Override
        public Response toResponse(NotFoundException notFoundException) {
            Message message = new Message();
            message.setStatus(Status.NOT_FOUND.getStatusCode());
            message.setMessage(Status.NOT_FOUND.getReasonPhrase());
            return Response.status(Status.NOT_FOUND).entity(message).build();
        }
    }

    @DisplayName("No Message Body Writer Found Failure Exception Mapper")
    public static class NoMessageBodyWriterFoundFailureExceptionMapper
            implements ExceptionMapper<NoMessageBodyWriterFoundFailure> {

        @Override
        public Response toResponse(NoMessageBodyWriterFoundFailure noMessageBodyWriterFoundFailure) {
            Message message = new Message();
            message.setStatus(Status.INTERNAL_SERVER_ERROR.getStatusCode());
            message.setMessage(noMessageBodyWriterFoundFailure.getClass().getName());
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_XML_TYPE).entity(message).build();
        }
    }

    private static Client client;

    private static final String DEP = "MediaTypeNegotiationTest";

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(DEP);
        war.addClass(Message.class);
        war.addClass(EchoResource.class);
        return TestUtil.finishContainerPrepare(war, null, NotFoundExceptionMapper.class,
                NoMessageBodyWriterFoundFailureExceptionMapper.class, EchoResource.class);
    }

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void cleanup() {
        client.close();
    }

    private String generateURL() {
        return PortProviderUtil.generateBaseUrl(DEP);
    }

    @Test
    @DisplayName("Test Accept Application Star")
    public void testAcceptApplicationStar() throws Exception {
        Invocation.Builder request = client.target(generateURL()).path("echo").queryParam("msg", "Hello world")
                .request("application/*");
        Response response = request.get();
        try {
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertTrue(response.getMediaType().toString().startsWith(MediaType.APPLICATION_XML));
        } finally {
            response.close();
        }
    }

    @Test
    @DisplayName("Test Accept Star Xml")
    public void testAcceptStarXml() throws Exception {
        Invocation.Builder request = client.target(generateURL()).path("echo").queryParam("msg", "Hello world")
                .request("*/xml");
        Response response = request.get();
        try {
            Assertions.assertEquals(Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        } finally {
            response.close();
        }
    }

    @Test
    @DisplayName("Test Accept Foo Bar")
    public void testAcceptFooBar() throws Exception {
        Invocation.Builder request = client.target(generateURL()).path("echo/missingMBW").queryParam("msg", "Hello world")
                .request("foo/bar");
        Response response = request.get();
        try {
            Assertions.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
            Assertions.assertEquals(MediaType.APPLICATION_XML_TYPE.getType(), response.getMediaType().getType());
            Assertions.assertEquals(MediaType.APPLICATION_XML_TYPE.getSubtype(), response.getMediaType().getSubtype());
            Message message = response.readEntity(Message.class);
            Assertions.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), message.getStatus());
            Assertions.assertEquals(NoMessageBodyWriterFoundFailure.class.getName(), message.getMessage());
        } finally {
            response.close();
        }
    }

    @Test
    @DisplayName("Should _ Return XML Encoded Message Entity _ When _ Not Found Exception")
    public void Should_ReturnXMLEncodedMessageEntity_When_NotFoundException() throws Exception {
        Invocation.Builder request = client.target(generateURL()).path("notFound").request(MediaType.APPLICATION_XML_TYPE);
        Response response = request.get();
        try {
            Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            Assertions.assertEquals(MediaType.APPLICATION_XML_TYPE.getType(), response.getMediaType().getType());
            Assertions.assertEquals(MediaType.APPLICATION_XML_TYPE.getSubtype(), response.getMediaType().getSubtype());
            Message message = response.readEntity(Message.class);
            Assertions.assertEquals(Status.NOT_FOUND.getStatusCode(), message.getStatus());
            Assertions.assertEquals(Status.NOT_FOUND.getReasonPhrase(), message.getMessage());
        } finally {
            response.close();
        }
    }
}
