package io.quarkus.rest.test.resource.request;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.request.resource.PreconditionRfc7232PrecedenceResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests for RFC 7232 functionality
 * @tpSince RESTEasy 3.0.17
 */
@DisplayName("Precondition Rfc 7232 Test")
public class PreconditionRfc7232Test {

    private Client client;

    private WebTarget webTarget;

    @BeforeEach
    public void before() throws Exception {
        client = ClientBuilder.newClient();
        webTarget = client.target(generateURL("/precedence"));
    }

    @AfterEach
    public void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(TestUtil.class, PortProviderUtil.class);
            Map<String, String> initParams = new HashMap<>();
            initParams.put("resteasy.rfc7232preconditions", "true");
            return TestUtil.finishContainerPrepare(war, initParams, PreconditionRfc7232PrecedenceResource.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, PreconditionRfc7232Test.class.getSimpleName());
    }

    /**
     * @tpTestDetails Response if all match
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Precedence _ All Match")
    public void testPrecedence_AllMatch() {
        Response response = // true
                webTarget.request().header(HttpHeaderNames.IF_MATCH, "1").header(HttpHeaderNames.IF_UNMODIFIED_SINCE, // true
                        "Mon, 1 Jan 2007 00:00:00 GMT").header(HttpHeaderNames.IF_NONE_MATCH, // true
                                "2")
                        .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if all match without IF_MATCH
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Precedence _ If Match With Non Matching Etag")
    public void testPrecedence_IfMatchWithNonMatchingEtag() {
        Response response = webTarget.request().header(HttpHeaderNames.IF_MATCH, // false
                "2").header(HttpHeaderNames.IF_UNMODIFIED_SINCE, // true
                        "Mon, 1 Jan 2007 00:00:00 GMT")
                .header(HttpHeaderNames.IF_NONE_MATCH, // true
                        "2")
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT").get();
        Assertions.assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if all match without IF_MATCH
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Precedence _ If Match Not Present Unmodified Since Before Last Modified")
    public void testPrecedence_IfMatchNotPresentUnmodifiedSinceBeforeLastModified() {
        Response response = webTarget.request().header(HttpHeaderNames.IF_UNMODIFIED_SINCE, // false
                "Sat, 30 Dec 2006 00:00:00 GMT").header(HttpHeaderNames.IF_NONE_MATCH, // true
                        "2")
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT").get();
        Assertions.assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if IF_MATH is missing
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Precedence _ If None Match With Matching Etag")
    public void testPrecedence_IfNoneMatchWithMatchingEtag() {
        Response response = webTarget.request().header(HttpHeaderNames.IF_NONE_MATCH, // true
                "1").header(HttpHeaderNames.IF_MODIFIED_SINCE, // true
                        "Mon, 1 Jan 2007 00:00:00 GMT")
                .get();
        Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if IF_MATH is missing and IF_NONE_MATCH don't match
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Precedence _ If None Match With Non Matching Etag")
    public void testPrecedence_IfNoneMatchWithNonMatchingEtag() {
        Response response = webTarget.request().header(HttpHeaderNames.IF_NONE_MATCH, // false
                "2").header(HttpHeaderNames.IF_MODIFIED_SINCE, // true
                        "Mon, 1 Jan 2007 00:00:00 GMT")
                .get();
        Assertions.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-4705"), Status.OK.getStatusCode(),
                response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if IF_MODIFIED_SINCE don't match
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Precedence _ If None Match Not Present _ If Modified Since Before Last Modified")
    public void testPrecedence_IfNoneMatchNotPresent_IfModifiedSinceBeforeLastModified() {
        Response response = webTarget.request().header(HttpHeaderNames.IF_MODIFIED_SINCE, // false
                "Sat, 30 Dec 2006 00:00:00 GMT").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if IF_MODIFIED_SINCE match
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Precedence _ If None Match Not Present _ If Modified Since After Last Modified")
    public void testPrecedence_IfNoneMatchNotPresent_IfModifiedSinceAfterLastModified() {
        Response response = webTarget.request().header(HttpHeaderNames.IF_MODIFIED_SINCE, // true
                "Tue, 2 Jan 2007 00:00:00 GMT").get();
        Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
        response.close();
    }
}
