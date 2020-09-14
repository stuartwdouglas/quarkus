package io.quarkus.rest.test.providers.atom;

import static org.hamcrest.CoreMatchers.containsString;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.atom.resource.LinkProduct;
import io.quarkus.rest.test.providers.atom.resource.LinkProductService;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Atom provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for org.jboss.resteasy.plugins.providers.atom.Link class
 * @tpSince RESTEasy 3.0.16
 */
public class LinkTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(LinkProduct.class);
                    return TestUtil.finishContainerPrepare(war, null, LinkProductService.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, LinkTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test response as java custom object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRelativeLinkProductOutput() throws Exception {
        Response response = client.target(generateURL("/products/333")).request().get();
        LinkProduct product = response.readEntity(LinkProduct.class);
        Assert.assertEquals("/LinkTest/products/333/self", product.getLinks().get(0).getHref().getPath());
        Assert.assertEquals("/LinkTest/products", product.getLinks().get(1).getHref().getPath());
        response.close();
    }

    /**
     * @tpTestDetails Test response as XML String
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testRelativeLinkStringOutput() throws Exception {
        Response response = client.target(generateURL("/products/333")).request().get();
        String stringResponse = response.readEntity(String.class);
        Assert.assertThat("Wrong link in response", stringResponse, containsString("/LinkTest/products/333/self\""));
        Assert.assertThat("Wrong link in response", stringResponse, containsString("/LinkTest/products\""));
        response.close();
    }
}
