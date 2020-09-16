package io.quarkus.rest.test.resource.param;

import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

@DisplayName("Matrix Path Param Test")
public class MatrixPathParamTest {

    @Deployment
    public static Archive<?> deploy() throws Exception {
        WebArchive war = TestUtil.prepareArchive(MatrixPathParamTest.class.getSimpleName());
        return TestUtil.finishContainerPrepare(war, null, TestResourceServer.class, TestSubResourceServer.class);
    }

    private String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(MatrixPathParamTest.class.getSimpleName());
    }

    @Path("/")
    @DisplayName("Test Resource Server")
    public static class TestResourceServer {

        @Path("matrix1")
        public TestSubResourceServer getM1(@MatrixParam("m1") String m1) {
            return new TestSubResourceServer(m1);
        }
    }

    @DisplayName("Test Sub Resource Server")
    public static class TestSubResourceServer {

        protected String m1;

        TestSubResourceServer(final String m1) {
            this.m1 = m1;
        }

        @GET
        @Path("matrix2")
        public String getM2(@MatrixParam("m2") String m2) {
            return m1 + m2;
        }
    }

    @Path("/")
    @DisplayName("Test Interface Client")
    public interface TestInterfaceClient {

        @Path("matrix1")
        TestSubInterfaceClient getM1(@MatrixParam("m1") String m1);
    }

    @DisplayName("Test Sub Interface Client")
    public interface TestSubInterfaceClient {

        @GET
        @Path("matrix2")
        String getM2(@MatrixParam("m2") String m2);
    }

    @Test
    @DisplayName("Test Single Accept Header")
    public void testSingleAcceptHeader() throws Exception {
        Client client = ClientBuilder.newClient();
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateBaseUrl());
        TestInterfaceClient proxy = target.proxy(TestInterfaceClient.class);
        String result = proxy.getM1("a").getM2("b");
        Assertions.assertEquals(result, "ab");
        client.close();
    }
}
