package io.quarkus.rest.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.resource.basic.resource.DefaultCharsetResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Add charset "UTF-8" to response mediatype if context parameter "resteasy.add.charset" is true
 * @tpSince RESTEasy 3.1.1.Final
 */
public class DefaultCharsetTest {

    protected enum ADD_CHARSET {
        TRUE,
        FALSE,
        DEFAULT
    };

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsWebInfResource(DefaultCharsetTest.class.getPackage(), "DefaultCharsetTestWeb_true.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, DefaultCharsetResource.class);
                }
            });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsWebInfResource(DefaultCharsetTest.class.getPackage(), "DefaultCharsetTestWeb_false.xml",
                            "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, DefaultCharsetResource.class);
                }
            });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addAsWebInfResource(DefaultCharsetTest.class.getPackage(), "DefaultCharsetTestWeb_default.xml",
                            "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, DefaultCharsetResource.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
        client = null;
    }

    private String generateURL(String suffix, String context, String path) {
        return PortProviderUtil.generateURL(context + path, DefaultCharsetTest.class.getSimpleName() + suffix);
    }

    @Test
    public void testCharset() throws Exception {
        doTest("_true", "/true", "/nocharset", "UTF-8"); // "resteasy.add.charset" set to true, text media type, charset not set
        doTest("_true", "/true", "/charset", "UTF-16"); // "resteasy.add.charset" set to true, text media type, charset already set
        doTest("_true", "/true", "/nomediatype", null); // "resteasy.add.charset" set to true, no mediatype set in response
        doTest("_true", "/true", "/xml_nocharset", "UTF-8"); // "resteasy.add.charset" set to true, application/xml media type, charset not set
        doTest("_true", "/true", "/xml_charset", "UTF-16"); // "resteasy.add.charset" set to true, application/xml media type, charset already set
        doTest("_true", "/true", "/external", "UTF-8"); // "resteasy.add.charset" set to true, application/xml-... media type, charset not set
        doTest("_true", "/true", "/json", null); // "resteasy.add.charset" set to true, application/json media type, charset not set

        doTest("_false", "/false", "/nocharset", null); // "resteasy.add.charset" set to false, text media type, charset not set
        doTest("_false", "/false", "/charset", "UTF-16"); // "resteasy.add.charset" set to false, text media type, charset already set
        doTest("_false", "/false", "/nomediatype", null); // "resteasy.add.charset" set to false, no media type set in response
        doTest("_false", "/false", "/xml_nocharset", null); // "resteasy.add.charset" set to false, application/xml media type, charset not set
        doTest("_false", "/false", "/xml_charset", "UTF-16"); // "resteasy.add.charset" set to false, application/xml media type, charset already set
        doTest("_false", "/false", "/external", null); // "resteasy.add.charset" set to false, application/xml-... media type, charset not set
        doTest("_false", "/false", "/json", null); // "resteasy.add.charset" set to false, application/json media type, charset not set

        doTest("_default", "/default", "/nocharset", "UTF-8"); // "resteasy.add.charset" not set, text media type, charset not set
        doTest("_default", "/default", "/charset", "UTF-16"); // "resteasy.add.charset" not set, text media type, charset already set
        doTest("_default", "/default", "/nomediatype", null); // "resteasy.add.charset" not set, no mediatype set in response
        doTest("_default", "/default", "/xml_nocharset", "UTF-8"); // "resteasy.add.charset" not set, application/xml media type, charset not set
        doTest("_default", "/default", "/xml_charset", "UTF-16"); // "resteasy.add.charset" not set, application/xml media type, charset already set
        doTest("_default", "/default", "/external", "UTF-8"); // "resteasy.add.charset" not set, application/xml-... media type, charset not set
        doTest("_default", "/default", "/json", null); // "resteasy.add.charset" not set, application/json media type, charset not set
    }

    void doTest(String suffix, String mapping, String path, String expectedMediaType) throws Exception {
        WebTarget target = client.target(generateURL(suffix, mapping, path));
        Response response = target.request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expectedMediaType, response.getMediaType().getParameters().get(MediaType.CHARSET_PARAMETER));
        response.close();
    }
}
