package io.quarkus.rest.test.client.proxy;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.client.proxy.resource.SubResourceLocatorProxyBookResource;
import io.quarkus.rest.test.client.proxy.resource.SubResourceLocatorProxyChapterResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class SubResourceLocatorProxyTest {

    public interface Book {
        @GET
        @Path("/title")
        @Produces("text/plain")
        String getTitle();

        @Path("/ch/{number}")
        Chapter getChapter(@PathParam("number") int number);
    }

    public interface Chapter {
        @GET
        @Path("title")
        @Produces("text/plain")
        String getTitle();

        @GET
        @Path("body")
        @Produces("text/plain")
        String getBody();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(SubResourceLocatorProxyTest.class);
                    return TestUtil.finishContainerPrepare(war, null, SubResourceLocatorProxyBookResource.class,
                            SubResourceLocatorProxyChapterResource.class);
                }
            });

    static QuarkusRestClient client;

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SubResourceLocatorProxyTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Client sends request thru client proxy. The processing of the response goes first to the Book
     *                resource which creates Chapter subresource and creates the response.
     * @tpPassCrit Expected string is returned in the response.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testSubresourceProxy() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/gulliverstravels"));
        Book book = target.proxy(Book.class);

        Assert.assertEquals("GET request thru client proxy failed", "Gulliver's Travels", book.getTitle());

        Chapter ch1 = book.getChapter(1);
        Assert.assertEquals("GET request thru client proxy failed", "Chapter 1", ch1.getTitle());

        Chapter ch2 = book.getChapter(2);
        Assert.assertEquals("GET request thru client proxy failed", "Chapter 2", ch2.getTitle());
    }
}
