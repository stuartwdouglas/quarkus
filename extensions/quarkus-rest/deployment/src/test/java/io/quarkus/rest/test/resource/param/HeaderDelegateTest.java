package io.quarkus.rest.test.resource.param;

import java.util.Date;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.delegates.DateDelegate;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.DateUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateDate;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateDelegate;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateInterface1;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateInterface2;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateInterface3;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateInterface4;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateResource;
import io.quarkus.rest.test.resource.param.resource.HeaderDelegateSubDelegate;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-915
 * @tpSince RESTEasy 3.0.16
 */

public class HeaderDelegateTest {
    private static Logger logger = Logger.getLogger(HeaderDelegateTest.class);

    public static final Date RIGHT_AFTER_BIG_BANG = new HeaderDelegateDate(3000);

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(HeaderDelegateDate.class);
                    war.addClass(HeaderDelegateDelegate.class);
                    war.addClass(HeaderDelegateInterface1.class);
                    war.addClass(HeaderDelegateInterface2.class);
                    war.addClass(HeaderDelegateInterface3.class);
                    war.addClass(HeaderDelegateInterface4.class);
                    war.addClass(HeaderDelegateSubDelegate.class);
                    war.addClass(PortProviderUtil.class);
                    war.addClass(HeaderDelegateTest.class);

                    // required by arquillian PortProviderUtil

                    return TestUtil.finishContainerPrepare(war, null, HeaderDelegateResource.class);
                }
            });

    private ResteasyProviderFactory factory;

    @BeforeEach
    public void init() {
        factory = ResteasyProviderFactory.newInstance();
        RegisterBuiltin.register(factory);
        ResteasyProviderFactory.setInstance(factory);
    }

    @AfterEach
    public void after() throws Exception {
        // Clear the singleton
        ResteasyProviderFactory.clearInstanceIfEqual(factory);
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, HeaderDelegateTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test delegation by client
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void lastModifiedTest() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/last"));
        Invocation.Builder request = target.request();
        Response response = request.get();
        logger.info("lastModified string: " + response.getHeaderString("last-modified"));
        Date last = response.getLastModified();
        Assert.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        Assert.assertEquals("Wrong response", DateUtil.formatDate(RIGHT_AFTER_BIG_BANG), DateUtil.formatDate(last));
        client.close();
    }

    /**
     * @tpTestDetails Check delegation rules from ResteasyProviderFactory
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void localTest() throws Exception {

        Assert.assertEquals("Wrong delegation", DateDelegate.class,
                factory.getHeaderDelegate(HeaderDelegateDate.class).getClass());
        Assert.assertEquals("Wrong delegation", DateDelegate.class,
                factory.createHeaderDelegate(HeaderDelegateDate.class).getClass());

        @SuppressWarnings("rawtypes")
        HeaderDelegateSubDelegate<?> delegate = new HeaderDelegateSubDelegate();
        factory.addHeaderDelegate(HeaderDelegateInterface1.class, delegate);
        Assert.assertEquals("Wrong delegation", delegate, factory.getHeaderDelegate(HeaderDelegateInterface1.class));
        Assert.assertEquals("Wrong delegation", delegate, factory.getHeaderDelegate(HeaderDelegateInterface2.class));
        Assert.assertEquals("Wrong delegation", delegate, factory.getHeaderDelegate(HeaderDelegateInterface3.class));
        Assert.assertEquals("Wrong delegation", delegate, factory.getHeaderDelegate(HeaderDelegateInterface4.class));
        Assert.assertEquals("Wrong delegation", delegate, factory.getHeaderDelegate(HeaderDelegateDelegate.class));
        Assert.assertEquals("Wrong delegation", delegate, factory.getHeaderDelegate(HeaderDelegateSubDelegate.class));
    }
}
