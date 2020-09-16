package io.quarkus.rest.test.resource.request;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.request.resource.PreconditionEtagResource;
import io.quarkus.rest.test.resource.request.resource.PreconditionLastModifiedResource;
import io.quarkus.rest.test.resource.request.resource.PreconditionPrecedenceResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests for preconditions specified in the header of the request
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Precondition Test")
public class PreconditionTest {

    static Client client;

    static WebTarget precedenceWebTarget;

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
        precedenceWebTarget = client.target(generateURL("/precedence"));
    }

    @AfterAll
    public static void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, PreconditionLastModifiedResource.class,
                    PreconditionEtagResource.class, PreconditionPrecedenceResource.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, PreconditionTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Sets header IF_UNMODIFIED_SINCE date before last modified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Unmodified Since Before Last Modified")
    public void testIfUnmodifiedSinceBeforeLastModified() {
        WebTarget base = client.target(generateURL("/"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_UNMODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT")
                    .get();
            Assertions.assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Sets header IF_UNMODIFIED_SINCE date after last modified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Unmodified Since After Last Modified")
    public void testIfUnmodifiedSinceAfterLastModified() {
        WebTarget base = client.target(generateURL("/"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_UNMODIFIED_SINCE, "Tue, 2 Jan 2007 00:00:00 GMT")
                    .get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Sets header IF_MODIFIED_SINCE date before last modified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Modified Since Before Last Modified")
    public void testIfModifiedSinceBeforeLastModified() {
        WebTarget base = client.target(generateURL("/"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT").get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Sets header IF_MODIFIED_SINCE date after last modified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Modified Since After Last Modified")
    public void testIfModifiedSinceAfterLastModified() {
        WebTarget base = client.target(generateURL("/"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MODIFIED_SINCE, "Tue, 2 Jan 2007 00:00:00 GMT").get();
            Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Sets headers IF_MODIFIED_SINCE and IF_UNMODIFIED_SINCE date before last modified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Unmodified Since Before Last Modified _ If Modified Since Before Last Modified")
    public void testIfUnmodifiedSinceBeforeLastModified_IfModifiedSinceBeforeLastModified() {
        WebTarget base = client.target(generateURL("/"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_UNMODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT")
                    .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT").get();
            Assertions.assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Sets headers IF_MODIFIED_SINCE date after last modified and IF_UNMODIFIED_SINCE date before last modified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Unmodified Since Before Last Modified _ If Modified Since After Last Modified")
    public void testIfUnmodifiedSinceBeforeLastModified_IfModifiedSinceAfterLastModified() {
        WebTarget base = client.target(generateURL("/"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_UNMODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT")
                    .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Tue, 2 Jan 2007 00:00:00 GMT").get();
            Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Sets headers IF_MODIFIED_SINCE and IF_UNMODIFIED_SINCE date after last modified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Unmodified Since After Last Modified _ If Modified Since After Last Modified")
    public void testIfUnmodifiedSinceAfterLastModified_IfModifiedSinceAfterLastModified() {
        WebTarget base = client.target(generateURL("/"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_UNMODIFIED_SINCE, "Tue, 2 Jan 2007 00:00:00 GMT")
                    .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Tue, 2 Jan 2007 00:00:00 GMT").get();
            Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Sets headers IF_MODIFIED_SINCE date before last modified and IF_UNMODIFIED_SINCE date after last modified
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Unmodified Since After Last Modified _ If Modified Since Before Last Modified")
    public void testIfUnmodifiedSinceAfterLastModified_IfModifiedSinceBeforeLastModified() {
        WebTarget base = client.target(generateURL("/"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_UNMODIFIED_SINCE, "Tue, 2 Jan 2007 00:00:00 GMT")
                    .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT").get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Sets header IF_MATCH to an entity value which matches to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Match With Matching E Tag")
    public void testIfMatchWithMatchingETag() {
        testIfMatchWithMatchingETag("");
        testIfMatchWithMatchingETag("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_MATCH to an entity value which doesn't match to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Match Without Matching E Tag")
    public void testIfMatchWithoutMatchingETag() {
        testIfMatchWithoutMatchingETag("");
        testIfMatchWithoutMatchingETag("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_MATCH to a wildcard
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Match Wild Card")
    public void testIfMatchWildCard() {
        testIfMatchWildCard("");
        testIfMatchWildCard("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_NONE_MATCH to an entity value which matches to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Non Match With Matching E Tag")
    public void testIfNonMatchWithMatchingETag() {
        testIfNonMatchWithMatchingETag("");
        testIfNonMatchWithMatchingETag("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_NONE_MATCH to an entity value which doesn't match to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Non Match Without Matching E Tag")
    public void testIfNonMatchWithoutMatchingETag() {
        testIfNonMatchWithoutMatchingETag("");
        testIfNonMatchWithoutMatchingETag("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_NONE_MATCH to a wildcard
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Non Match Wild Card")
    public void testIfNonMatchWildCard() {
        testIfNonMatchWildCard("");
        testIfNonMatchWildCard("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_NONE_MATCH and IF_MATCH to an entity value which matches to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Match With Matching E Tag _ If Non Match With Matching E Tag")
    public void testIfMatchWithMatchingETag_IfNonMatchWithMatchingETag() {
        testIfMatchWithMatchingETag_IfNonMatchWithMatchingETag("");
        testIfMatchWithMatchingETag_IfNonMatchWithMatchingETag("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_NONE_MATCH to an entity value which doesn't match to eTag in the resource
     *                and IF_MATCH to an entity value which matches to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Match With Matching E Tag _ If Non Match Without Matching E Tag")
    public void testIfMatchWithMatchingETag_IfNonMatchWithoutMatchingETag() {
        testIfMatchWithMatchingETag_IfNonMatchWithoutMatchingETag("");
        testIfMatchWithMatchingETag_IfNonMatchWithoutMatchingETag("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_NONE_MATCH to an entity value which matches to eTag in the resource
     *                and IF_MATCH to an entity value which doesn't match to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Match Without Matching E Tag _ If Non Match With Matching E Tag")
    public void testIfMatchWithoutMatchingETag_IfNonMatchWithMatchingETag() {
        testIfMatchWithoutMatchingETag_IfNonMatchWithMatchingETag("");
        testIfMatchWithoutMatchingETag_IfNonMatchWithMatchingETag("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_NONE_MATCH to an entity value which doesn't match to eTag in the resource
     *                and IF_MATCH to an entity value which doesn't match to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Match Without Matching E Tag _ If Non Match Without Matching E Tag")
    public void testIfMatchWithoutMatchingETag_IfNonMatchWithoutMatchingETag() {
        testIfMatchWithoutMatchingETag_IfNonMatchWithoutMatchingETag("");
        testIfMatchWithoutMatchingETag_IfNonMatchWithoutMatchingETag("/fromField");
    }

    /**
     * @tpTestDetails Sets header IF_MATCH to an weak eTag value which matches to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Match With Matching Weak E Tag")
    public void testIfMatchWithMatchingWeakETag() {
        WebTarget base = client.target(generateURL("/etag/weak"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MATCH, "W/\"1\"").get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Sets header IF_MATCH to an weak eTag value which matches to eTag in the resource
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test If Match With Non Matching Weak Etag")
    public void testIfMatchWithNonMatchingWeakEtag() {
        WebTarget base = client.target(generateURL("/etag/weak"));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MATCH, "W/\"2\"").get();
            Assertions.assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // //////////
    public void testIfMatchWithMatchingETag(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MATCH, "\"1\"").get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testIfMatchWithoutMatchingETag(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MATCH, "\"2\"").get();
            Assertions.assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testIfMatchWildCard(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MATCH, "*").get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testIfNonMatchWithMatchingETag(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_NONE_MATCH, "\"1\"").get();
            Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
            Assertions.assertEquals("\"1\"", response.getStringHeaders().getFirst(HttpHeaderNames.ETAG),
                    "The eTag in the response doesn't match");
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testIfNonMatchWithoutMatchingETag(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_NONE_MATCH, "2").get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testIfNonMatchWildCard(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_NONE_MATCH, "*").get();
            Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
            Assertions.assertEquals("\"1\"", response.getStringHeaders().getFirst(HttpHeaderNames.ETAG),
                    "The eTag in the response doesn't match");
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testIfMatchWithMatchingETag_IfNonMatchWithMatchingETag(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MATCH, "\"1\"")
                    .header(HttpHeaderNames.IF_NONE_MATCH, "\"1\"").get();
            Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
            Assertions.assertEquals("\"1\"", response.getStringHeaders().getFirst(HttpHeaderNames.ETAG),
                    "The eTag in the response doesn't match");
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testIfMatchWithMatchingETag_IfNonMatchWithoutMatchingETag(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MATCH, "\"1\"")
                    .header(HttpHeaderNames.IF_NONE_MATCH, "\"2\"").get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testIfMatchWithoutMatchingETag_IfNonMatchWithMatchingETag(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MATCH, "\"2\"")
                    .header(HttpHeaderNames.IF_NONE_MATCH, "\"1\"").get();
            Assertions.assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testIfMatchWithoutMatchingETag_IfNonMatchWithoutMatchingETag(String fromField) {
        WebTarget base = client.target(generateURL("/etag" + fromField));
        try {
            Response response = base.request().header(HttpHeaderNames.IF_MATCH, "\"2\"")
                    .header(HttpHeaderNames.IF_NONE_MATCH, "\"2\"").get();
            Assertions.assertEquals(Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @tpTestDetails Response if all match
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Precedence _ All Match")
    public void testPrecedence_AllMatch() {
        Response response = precedenceWebTarget.request().header(HttpHeaderNames.IF_MATCH, // true
                "1").header(HttpHeaderNames.IF_UNMODIFIED_SINCE, // true
                        "Mon, 1 Jan 2007 00:00:00 GMT")
                .header(HttpHeaderNames.IF_NONE_MATCH, // true
                        "2")
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, // true
                        "Sat, 30 Dec 2006 00:00:00 GMT")
                .get();
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
        Response response = precedenceWebTarget.request().header(HttpHeaderNames.IF_MATCH, // false
                "2").header(HttpHeaderNames.IF_UNMODIFIED_SINCE, // true
                        "Mon, 1 Jan 2007 00:00:00 GMT")
                .header(HttpHeaderNames.IF_NONE_MATCH, // true
                        "2")
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, // true
                        "Sat, 30 Dec 2006 00:00:00 GMT")
                .get();
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
        Response response = precedenceWebTarget.request().header(HttpHeaderNames.IF_UNMODIFIED_SINCE, // false
                "Sat, 30 Dec 2006 00:00:00 GMT").header(HttpHeaderNames.IF_NONE_MATCH, // true
                        "2")
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, // true
                        "Sat, 30 Dec 2006 00:00:00 GMT")
                .get();
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
        Response response = precedenceWebTarget.request().header(HttpHeaderNames.IF_NONE_MATCH, // true
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
        Response response = precedenceWebTarget.request().header(HttpHeaderNames.IF_NONE_MATCH, // false
                "2").header(HttpHeaderNames.IF_MODIFIED_SINCE, // true
                        "Mon, 1 Jan 2007 00:00:00 GMT")
                .get();
        Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if IF_MODIFIED_SINCE don't match
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    @DisplayName("Test Precedence _ If None Match Not Present _ If Modified Since Before Last Modified")
    public void testPrecedence_IfNoneMatchNotPresent_IfModifiedSinceBeforeLastModified() {
        Response response = precedenceWebTarget.request().header(HttpHeaderNames.IF_MODIFIED_SINCE, // false
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
        Response response = precedenceWebTarget.request().header(HttpHeaderNames.IF_MODIFIED_SINCE, // true
                "Tue, 2 Jan 2007 00:00:00 GMT").get();
        Assertions.assertEquals(Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
        response.close();
    }
}
