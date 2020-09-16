package io.quarkus.rest.test.providers.custom;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

@DisplayName("Retrieve Registered Classes Test")
public class RetrieveRegisteredClassesTest {

    @Path("/testResource")
    @Produces(MediaType.APPLICATION_XML)
    @DisplayName("Test Resource")
    public static final class TestResource {

        @GET
        public String get() {
            return TestResource.class.getName();
        }
    }

    @DisplayName("My Filter")
    private static class MyFilter implements ClientRequestFilter {

        // To discard empty constructor
        private MyFilter(final Object value) {
        }

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
        }
    }

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(RetrieveRegisteredClassesTest.class.getSimpleName());
        return TestUtil.finishContainerPrepare(war, null, TestResource.class);
    }

    @Test
    @DisplayName("Test")
    public void test() {
        Client client = ClientBuilder.newClient();
        try {
            String uri = PortProviderUtil.generateURL("/testResource", RetrieveRegisteredClassesTest.class.getSimpleName());
            MyFilter myFilter = new MyFilter(new Object());
            WebTarget firstWebTarget = client.target(uri).register(myFilter);
            String firstResult = firstWebTarget.request(MediaType.APPLICATION_XML).get(String.class);
            Configuration firstWebTargetConfiguration = firstWebTarget.getConfiguration();
            Set<Class<?>> classes = firstWebTargetConfiguration.getClasses();
            Set<Object> instances = firstWebTargetConfiguration.getInstances();
            Assertions.assertFalse(classes.contains(MyFilter.class));
            Assertions.assertTrue(instances.contains(myFilter));
            WebTarget secondWebTarget = client.target(uri);
            Configuration secondWebTargetConfiguration = secondWebTarget.getConfiguration();
            for (Class<?> classz : classes) {
                if (!secondWebTargetConfiguration.isRegistered(classz)) {
                    secondWebTarget.register(classz);
                }
            }
            for (Object instance : instances) {
                if (!secondWebTargetConfiguration.isRegistered(instance.getClass())) {
                    secondWebTarget.register(instance);
                }
            }
            String secondeResult = secondWebTarget.request(MediaType.APPLICATION_XML).get(String.class);
            classes = secondWebTargetConfiguration.getClasses();
            instances = secondWebTargetConfiguration.getInstances();
            Assertions.assertFalse(classes.contains(MyFilter.class));
            Assertions.assertTrue(instances.contains(myFilter));
            Assertions.assertEquals(firstResult, secondeResult);
        } finally {
            client.close();
        }
    }
}
