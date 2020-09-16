package io.quarkus.rest.test.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

public class ClientInvocationBuilderTest extends ClientTestBase {

    @Path("/")
    public static class ClientInvocationBuilderResource {
        @POST
        @Produces("text/plain")
        public String post(String s) {
            return s;
        }

        @GET
        @Produces("text/plain")
        public String get() {
            String s = "default";
            return s;
        }
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ClientInvocationBuilderTest.class);
                    war.addClass(ClientTestBase.class);
                    return TestUtil.finishContainerPrepare(war, null, ClientInvocationBuilderResource.class);
                }
            });

    @Test
    public void testBuildMethodReturnNewInstance() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            QuarkusRestWebTarget webTarget = client.target(generateURL(""));
            Builder invocationBuilder = webTarget.request();

            // GET invocation
            ClientInvocation getInvocation = (ClientInvocation) invocationBuilder.accept(MediaType.TEXT_PLAIN_TYPE)
                    .build("GET");
            Assert.assertEquals("default", getInvocation.invoke(String.class));

            // Alter invocationBuilder
            invocationBuilder.accept(MediaType.APPLICATION_XML_TYPE);
            invocationBuilder.property("property1", "property1Value");
            // Previously built getInvocation must not have been altered.(Those
            // tests are not about immutability of
            // getInvocation instance but about Builder pattern behavior).
            Assert.assertFalse(getInvocation.getHeaders().getAcceptableMediaTypes()
                    .contains(MediaType.APPLICATION_XML_TYPE));
            Assert.assertFalse(getInvocation.getConfiguration().getProperties().containsKey("property1"));

            // POST invocation
            ClientInvocation postInvocation = (ClientInvocation) invocationBuilder.accept(MediaType.TEXT_PLAIN_TYPE)
                    .build("POST", Entity.text("test"));
            // Previous lines must build a new postInvocation instance and not
            // modify previously built getInvocation instance.
            // (It's all about Builder pattern behavior not immutability since
            // Invocation is a mutable object.)
            Assert.assertNotSame(getInvocation, postInvocation);
            Assert.assertEquals("default", getInvocation.invoke(String.class));
            Assert.assertTrue(postInvocation.getConfiguration().getProperties().containsKey("property1"));
            Assert.assertEquals("test", postInvocation.invoke(String.class));
        } finally {
            client.close();
        }
    }

    @Test
    public void testBuildMethodResetEntity() throws InterruptedException, ExecutionException {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            QuarkusRestWebTarget webTarget = client.target(generateURL(""));
            Builder invocationBuilder = webTarget.request().accept(MediaType.TEXT_PLAIN_TYPE);

            // POST invocation
            ClientInvocation postInvocation = (ClientInvocation) invocationBuilder.build("POST", Entity.text("test"));
            Assert.assertEquals("test", postInvocation.invoke(String.class));

            // GET invocation
            ClientInvocation getInvocation = (ClientInvocation) invocationBuilder.build("GET");
            // In order the request to be OK, invocation instance built from
            // invocationBuilder must not contain the previous entity used for
            // post request.
            Assert.assertNull(getInvocation.getEntity());
            Assert.assertEquals("default", getInvocation.invoke(String.class));

            // Same test for async request
            AsyncInvoker async = invocationBuilder.async();

            // POST invocation
            Future<String> postFuture = async.post(Entity.text("test"), String.class);
            Assert.assertEquals("test", postFuture.get());

            // GET invocation
            Future<String> getFuture = async.get(String.class);
            Assert.assertEquals("default", getFuture.get());
        } finally {
            client.close();
        }
    }

}
