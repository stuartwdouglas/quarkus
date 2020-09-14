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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
public class PreconditionRfc7232Test {
    private Client client;
    private WebTarget webTarget;

    @Before
    public void before() throws Exception {
        client = ClientBuilder.newClient();
        webTarget = client.target(generateURL("/precedence"));
    }

    @After
    public void close() {
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
    public void testPrecedence_AllMatch() {
        Response response = webTarget.request().header(HttpHeaderNames.IF_MATCH, "1") // true
                .header(HttpHeaderNames.IF_UNMODIFIED_SINCE, "Mon, 1 Jan 2007 00:00:00 GMT") // true
                .header(HttpHeaderNames.IF_NONE_MATCH, "2") // true
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT").get(); // true

        Assert.assertEquals(Status.OK, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if all match without IF_MATCH
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testPrecedence_IfMatchWithNonMatchingEtag() {
        Response response = webTarget.request()
                .header(HttpHeaderNames.IF_MATCH, "2") // false
                .header(HttpHeaderNames.IF_UNMODIFIED_SINCE, "Mon, 1 Jan 2007 00:00:00 GMT") // true
                .header(HttpHeaderNames.IF_NONE_MATCH, "2") // true
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT").get(); // true

        Assert.assertEquals(Status.PRECONDITION_FAILED, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if all match without IF_MATCH
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testPrecedence_IfMatchNotPresentUnmodifiedSinceBeforeLastModified() {
        Response response = webTarget.request()
                .header(HttpHeaderNames.IF_UNMODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT") //false
                .header(HttpHeaderNames.IF_NONE_MATCH, "2") // true
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT").get(); // true

        Assert.assertEquals(Status.PRECONDITION_FAILED, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Response if IF_MATH is missing
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testPrecedence_IfNoneMatchWithMatchingEtag() {
        Response response = webTarget.request()
                .header(HttpHeaderNames.IF_NONE_MATCH, "1") // true
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Mon, 1 Jan 2007 00:00:00 GMT") // true
                .get();
        Assert.assertEquals(Status.NOT_MODIFIED, response.getStatus());

        response.close();
    }

    /**
     * @tpTestDetails Response if IF_MATH is missing and IF_NONE_MATCH don't match
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testPrecedence_IfNoneMatchWithNonMatchingEtag() {
        Response response = webTarget.request()
                .header(HttpHeaderNames.IF_NONE_MATCH, "2") // false
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Mon, 1 Jan 2007 00:00:00 GMT") // true
                .get();
        Assert.assertEquals(TestUtil.getErrorMessageForKnownIssue("JBEAP-4705"), Status.OK, response.getStatus());

        response.close();
    }

    /**
     * @tpTestDetails Response if IF_MODIFIED_SINCE don't match
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testPrecedence_IfNoneMatchNotPresent_IfModifiedSinceBeforeLastModified() {
        Response response = webTarget.request()
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Sat, 30 Dec 2006 00:00:00 GMT") // false
                .get();
        Assert.assertEquals(Status.OK, response.getStatus());

        response.close();
    }

    /**
     * @tpTestDetails Response if IF_MODIFIED_SINCE match
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testPrecedence_IfNoneMatchNotPresent_IfModifiedSinceAfterLastModified() {
        Response response = webTarget.request()
                .header(HttpHeaderNames.IF_MODIFIED_SINCE, "Tue, 2 Jan 2007 00:00:00 GMT") // true
                .get();
        Assert.assertEquals(Status.NOT_MODIFIED, response.getStatus());
        response.close();
    }
}
