package io.quarkus.rest.test.response;

import static io.quarkus.rest.test.ContainerConstants.DEFAULT_CONTAINER_QUALIFIER;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.response.resource.DuplicitePathDupliciteApplicationOne;
import io.quarkus.rest.test.response.resource.DuplicitePathDupliciteApplicationTwo;
import io.quarkus.rest.test.response.resource.DuplicitePathDupliciteResourceOne;
import io.quarkus.rest.test.response.resource.DuplicitePathDupliciteResourceTwo;
import io.quarkus.rest.test.response.resource.DuplicitePathMethodResource;
import io.quarkus.rest.test.response.resource.DuplicitePathNoDupliciteApplication;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-3459
 * @tpSince RESTEasy 3.0.17
 */
@DisplayName("Duplicite Path Test")
public class DuplicitePathTest {

    static QuarkusRestClient client;

    /**
     * Init servlet warning count ( WFLYUT0101: Duplicate servlet mapping /a/* found )
     */
    private static int initServletWarningsCount;

    /**
     * Get RESTEasy warning count
     */
    private static int getWarningCount() {
        return TestUtil.getWarningCount("RESTEASY002142", false, DEFAULT_CONTAINER_QUALIFIER);
    }

    /**
     * Gets servlet warning count
     * Warning comes from server (outside of the resteasy)
     * Example: WFLYUT0101: Duplicate servlet mapping /a/* found
     */
    private static int getServletMappingWarningCount() {
        return TestUtil.getWarningCount("WFLYUT0101", false, DEFAULT_CONTAINER_QUALIFIER);
    }

    @Deployment
    public static Archive<?> deploySimpleResource() {
        initServletWarningsCount = getServletMappingWarningCount();
        WebArchive war = ShrinkWrap.create(WebArchive.class, DuplicitePathTest.class.getSimpleName() + ".war");
        war.addClass(DuplicitePathDupliciteApplicationOne.class);
        war.addClass(DuplicitePathDupliciteApplicationTwo.class);
        war.addClass(DuplicitePathDupliciteResourceOne.class);
        war.addClass(DuplicitePathDupliciteResourceTwo.class);
        war.addClass(DuplicitePathMethodResource.class);
        war.addClass(DuplicitePathNoDupliciteApplication.class);
        return war;
    }

    @BeforeAll
    public static void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterAll
    public static void close() {
        client.close();
        client = null;
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, DuplicitePathTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check that warning message was logged, if client makes request to path,
     *                that is handled by two methods in two end-point in two application classes
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Duplication Two App Two Resource Same Method Path")
    public void testDuplicationTwoAppTwoResourceSameMethodPath() throws Exception {
        WebTarget base = client.target(generateURL("/a/b/c"));
        Response response = null;
        try {
            response = base.request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            String strResponse = response.readEntity(String.class);
            Assert.assertThat("Wrong body of response", strResponse,
                    either(is(DuplicitePathDupliciteResourceOne.DUPLICITE_RESPONSE))
                            .or(is(DuplicitePathDupliciteResourceTwo.DUPLICITE_RESPONSE)));
        } finally {
            response.close();
        }
        Assertions.assertEquals(TestUtil.getErrorMessageForKnownIssue("RESTEASY-1445", "Wrong count of warnings in server log"),
                1, getServletMappingWarningCount() - initServletWarningsCount);
    }

    /**
     * @tpTestDetails Check that warning message was logged, if client makes request to path,
     *                that is handled by two methods in two end-point in two application classes
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Duplication More Accepts")
    public void testDuplicationMoreAccepts() throws Exception {
        int initWarningsCount = getWarningCount();
        WebTarget base = client.target(generateURL("/f/g/i"));
        Response response = null;
        try {
            response = base.request().accept(MediaType.TEXT_PLAIN, MediaType.WILDCARD).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            String strResponse = response.readEntity(String.class);
            Assert.assertThat("Wrong body of response", strResponse, is(DuplicitePathMethodResource.NO_DUPLICITE_RESPONSE));
        } finally {
            response.close();
        }
        Assertions.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-3459", "Wrong count of warnings in server log"), 0,
                getWarningCount() - initWarningsCount);
    }

    /**
     * @tpTestDetails Check that warning message was logged, if client makes request to path,
     *                that is handled by two methods in two end-point in two application classes
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Duplication Moretypes")
    public void testDuplicationMoretypes() throws Exception {
        int initWarningsCount = getWarningCount();
        WebTarget base = client.target(generateURL("/f/g/j"));
        Response response = null;
        try {
            response = base.request().accept(MediaType.TEXT_PLAIN, MediaType.WILDCARD).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            String strResponse = response.readEntity(String.class);
            Assert.assertThat("Wrong body of response", strResponse, is(DuplicitePathMethodResource.DUPLICITE_TYPE_GET));
        } finally {
            response.close();
        }
        Assertions.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-3459", "Wrong count of warnings in server log"), 0,
                getWarningCount() - initWarningsCount);
    }

    /**
     * @tpTestDetails Check that warning message was logged, if client makes request to path,
     *                that is handled by two methods in two end-point in one application classes
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Duplication One App Two Resources With Same Path")
    public void testDuplicationOneAppTwoResourcesWithSamePath() throws Exception {
        int initWarningsCount = getWarningCount();
        WebTarget base = client.target(generateURL("/f/b/c"));
        Response response = null;
        try {
            response = base.request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            String strResponse = response.readEntity(String.class);
            Assert.assertThat("Wrong body of response", strResponse,
                    either(is(DuplicitePathDupliciteResourceOne.DUPLICITE_RESPONSE))
                            .or(is(DuplicitePathDupliciteResourceTwo.DUPLICITE_RESPONSE)));
        } finally {
            response.close();
        }
        Assertions.assertEquals(1, getWarningCount() - initWarningsCount, "Wrong count of warnings in server log");
    }

    /**
     * @tpTestDetails Check that warning message was logged, if client makes request to path, that is handled by two methods
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Duplication Path In Method")
    public void testDuplicationPathInMethod() throws Exception {
        int initWarningsCount = getWarningCount();
        WebTarget base = client.target(generateURL("/f/g/h"));
        Response response = null;
        try {
            response = base.request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assert.assertThat("Wrong body of response", response.readEntity(String.class),
                    either(is(DuplicitePathMethodResource.DUPLICITE_RESPONSE_1))
                            .or(is(DuplicitePathMethodResource.DUPLICITE_RESPONSE_2)));
        } finally {
            response.close();
        }
        Assertions.assertEquals(1, getWarningCount() - initWarningsCount, "Wrong count of warnings in server log");
    }

    /**
     * @tpTestDetails Check that warning message was not logged, if client makes request to path, that is handled by one method
     *                (correct behaviour)
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test No Duplication Path In Method")
    public void testNoDuplicationPathInMethod() throws Exception {
        int initWarningsCount = getWarningCount();
        WebTarget base = client.target(generateURL("/f/g/i"));
        Response response = null;
        try {
            response = base.request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals(DuplicitePathMethodResource.NO_DUPLICITE_RESPONSE, response.readEntity(String.class),
                    "Wrong body of response");
        } finally {
            response.close();
        }
        Assertions.assertEquals(0, getWarningCount() - initWarningsCount, "Wrong count of warnings in server log");
    }
}
