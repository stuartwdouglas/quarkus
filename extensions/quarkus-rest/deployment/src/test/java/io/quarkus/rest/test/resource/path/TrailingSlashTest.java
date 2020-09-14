package io.quarkus.rest.test.resource.path;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.TrailingSlashResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.17
 * @tpTestCaseDetails Regression test for JBEAP-4698
 */
public class TrailingSlashTest {
    private static final String ERROR_MSG = "ResteasyUriInfo parsed slash wrongly";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, TrailingSlashResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResteasyTrailingSlashTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Simple construction of ResteasyUriInfo.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void oneArgConstructorTest() throws Exception {
        doOneArgConstructorTest(new URI("http://localhost/abc"), "/abc");
        doOneArgConstructorTest(new URI("http://localhost/abc/"), "/abc/");
    }

    void doOneArgConstructorTest(URI uri, String path) {
        ResteasyUriInfo ruri = new ResteasyUriInfo(uri);
        Assert.assertEquals(ERROR_MSG, path, ruri.getPath());
        Assert.assertEquals(ERROR_MSG, path, ruri.getPath(true));
        Assert.assertEquals(ERROR_MSG, path, ruri.getPath(false));
        Assert.assertEquals(ERROR_MSG, uri, ruri.getAbsolutePath());
        Assert.assertEquals(ERROR_MSG, uri, ruri.getBaseUri().resolve(ruri.getPath(false)));
    }

    /**
     * @tpTestDetails ResteasyUriInfo is based of two URIs.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void twoArgConstructorTest() throws Exception {
        doTwoArgConstructorTest(new URI("http://localhost/abc"), new URI("xyz"), "/xyz");
        doTwoArgConstructorTest(new URI("http://localhost/abc"), new URI("xyz/"), "/xyz/");
    }

    void doTwoArgConstructorTest(URI base, URI relative, String path) throws URISyntaxException {
        ResteasyUriInfo ruri = new ResteasyUriInfo(base, relative);
        Assert.assertEquals(ERROR_MSG, path, ruri.getPath());
        Assert.assertEquals(ERROR_MSG, path, ruri.getPath(true));
        Assert.assertEquals(ERROR_MSG, path, ruri.getPath(false));
        URI newUri;
        if (base.toString().endsWith("/")) {
            newUri = new URI(base.toString().substring(0, base.toString().length() - 1) + path);
        } else {
            newUri = new URI(base.toString() + path);
        }
        Assert.assertEquals(ERROR_MSG, newUri, ruri.getAbsolutePath());
    }

    /**
     * @tpTestDetails ResteasyUriInfo is based on queryString and contextPath.
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void threeArgConstructorTest() throws Exception {
        doTwoArgConstructorTest("http://localhost/abc", "/abc");
        doTwoArgConstructorTest("http://localhost/abc/", "/abc/");
    }

    void doTwoArgConstructorTest(String s, String path) throws URISyntaxException {
        ResteasyUriInfo ruri = new ResteasyUriInfo(s, "");
        URI uri = new URI(s);
        Assert.assertEquals(ERROR_MSG, path, ruri.getPath());
        Assert.assertEquals(ERROR_MSG, path, ruri.getPath(true));
        Assert.assertEquals(ERROR_MSG, path, ruri.getPath(false));
        Assert.assertEquals(ERROR_MSG, uri, ruri.getAbsolutePath());
        Assert.assertEquals(ERROR_MSG, uri, ruri.getBaseUri().resolve(ruri.getPath(false)));
    }

    /**
     * @tpTestDetails No slash at the end of URI
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testNoSlash() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/test"));
        Response response = target.request().get();
        Assert.assertEquals(ERROR_MSG, "/test", response.readEntity(String.class));
        client.close();
    }

    /**
     * @tpTestDetails Slash at the end of URI
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testSlash() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/test/"));
        Response response = target.request().get();
        Assert.assertEquals(ERROR_MSG, "/test/", response.readEntity(String.class));
        client.close();
    }
}
