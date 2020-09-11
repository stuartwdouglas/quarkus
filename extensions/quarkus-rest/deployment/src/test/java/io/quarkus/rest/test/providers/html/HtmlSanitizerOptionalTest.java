package io.quarkus.rest.test.providers.html;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.html.resource.HtmlSanitizerOptionalResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-2034
 * 
 * @tpSince RESTEasy 4.0.0
 */
public class HtmlSanitizerOptionalTest {

    static QuarkusRestClient client;

    private static final String ENABLED = "_enabled";
    private static final String DISABLED = "_disabled";
    private static final String DEFAULT = "_default";

    public static final String input = "<html &lt;\"abc\" 'xyz'&gt;/>";
    private static final String output = "&lt;html &amp;lt;&quot;abc&quot; &#x27;xyz&#x27;&amp;gt;&#x2F;&gt;";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsWebInfResource(HtmlSanitizerOptionalTest.class.getPackage(),
                            "HtmlSanitizerOptional_Enabled_web.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, HtmlSanitizerOptionalResource.class);
                }
            });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsWebInfResource(HtmlSanitizerOptionalTest.class.getPackage(),
                            "HtmlSanitizerOptional_Disabled_web.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, HtmlSanitizerOptionalResource.class);
                }
            });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsWebInfResource(HtmlSanitizerOptionalTest.class.getPackage(),
                            "HtmlSanitizerOptional_Default_web.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, HtmlSanitizerOptionalResource.class);
                }
            });

    private String generateURL(String path, String version) {
        return PortProviderUtil.generateURL(path, HtmlSanitizerOptionalTest.class.getSimpleName() + version);
    }

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Context parameter "resteasy.disable.html.sanitizer" is set to "true".
     * @tpPassCrit Input string should be unchanged.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testHtmlSanitizerDisabled() throws Exception {
        Response response = client.target(generateURL("/test", DISABLED)).request().get();
        Assert.assertEquals(input, response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Context parameter "resteasy.disable.html.sanitizer" is set to "false"
     * @tpPassCrit Input string should be sanitized.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testHtmlSanitizerEnabled() throws Exception {
        Response response = client.target(generateURL("/test", ENABLED)).request().get();
        Assert.assertEquals(output, response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Context parameter "resteasy.disable.html.sanitizer" is not set.
     * @tpPassCrit Input string should be sanitized.
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testHtmlSanitizerDefault() throws Exception {
        Response response = client.target(generateURL("/test", DEFAULT)).request().get();
        Assert.assertEquals(output, response.readEntity(String.class));
    }
}
