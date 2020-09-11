package io.quarkus.rest.test.core.spi;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.core.spi.resource.ResourceClassProcessorNotAppliedImplementation;
import io.quarkus.rest.test.core.spi.resource.ResourceClassProcessorPureEndPoint;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter ResourceClassProcessor SPI
 * @tpChapter Integration tests
 * @tpTestCaseDetails ResourceClassProcessor should not be used in some case
 * @tpSince RESTEasy 3.6
 */
@RunWith(Arquillian.class)
public class ResourceClassProcessorNotAppliedTest {

    protected static final Logger logger = Logger.getLogger(ResourceClassProcessorNotAppliedTest.class.getName());

    private static List<String> visitedProcessors = new ArrayList<>();

    public static synchronized void addToVisitedProcessors(String item) {
        visitedProcessors.add(item);
    }

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ResourceClassProcessorNotAppliedTest.class);
                    war.addClass(PortProviderUtil.class);
                    war.addClass(ResourceClassProcessorNotAppliedImplementation.class);

                    return TestUtil.finishContainerPrepare(war, null,
                            ResourceClassProcessorPureEndPoint.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResourceClassProcessorNotAppliedTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails ResourceClassProcessor implementation should not be used if web.xml doesn't contains provider name
     *                and resteasy.scan is not allowed
     * @tpSince RESTEasy 3.6
     */
    @Test
    public void notAppliedTest() {
        // init client
        client = (QuarkusRestClient) ClientBuilder.newClient();

        // do request
        Response response = client.target(generateURL("/pure/pure")).request().get();
        Assert.assertEquals(Status.OK, response.getStatus());

        // log visited processors
        int i = 0;
        for (String item : visitedProcessors) {
            logger.info(String.format("%d. %s", ++i, item));
        }

        // asserts
        Assert.assertThat("ResourceClassProcessor was used although it should not be used",
                visitedProcessors.size(), greaterThanOrEqualTo(0));

        // close client
        client.close();
    }
}
