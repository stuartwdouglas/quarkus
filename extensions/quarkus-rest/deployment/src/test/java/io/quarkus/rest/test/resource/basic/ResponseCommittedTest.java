package io.quarkus.rest.test.resource.basic;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.test.resource.basic.resource.ResponseCommittedResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1238
 * @tpSince RESTEasy 3.1.3.Final
 */
@DisplayName("Response Committed Test")
public class ResponseCommittedTest {

    public static int TEST_STATUS = 444;

    private static Client client;

    @Deployment
    public static Archive<?> deploy() throws Exception {
        WebArchive war = TestUtil.prepareArchive(ResponseCommittedTest.class.getSimpleName());
        return TestUtil.finishContainerPrepare(war, null, ResponseCommittedResource.class);
    }

    private String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(ResponseCommittedTest.class.getSimpleName());
    }

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @Test
    @DisplayName("Test Works")
    public void testWorks() throws Exception {
        Invocation.Builder request = client.target(generateBaseUrl()).request();
        Response response = request.get();
        Assertions.assertEquals(TEST_STATUS, response.getStatus());
        response.close();
        client.close();
    }
}
