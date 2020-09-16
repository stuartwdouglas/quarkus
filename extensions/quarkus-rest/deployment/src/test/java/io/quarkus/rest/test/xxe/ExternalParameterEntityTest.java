package io.quarkus.rest.test.xxe;

import static org.jboss.resteasy.utils.PortProviderUtil.generateURL;

import java.io.File;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.xxe.resource.ExternalParameterEntityResource;
import io.quarkus.rest.test.xxe.resource.ExternalParameterEntityWrapper;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter XXE
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1073.
 *                    External parameter entities should be disabled when the resteasy.document.expand.entity.references
 *                    parameter was set to false.
 *                    A remote attacker able to send XML requests to a RESTEasy endpoint could use this flaw to read files
 *                    accessible to the user
 *                    running the application server, and potentially perform other more advanced XXE attacks.
 * @tpSince RESTEasy 3.0.16
 */
public class ExternalParameterEntityTest {

    protected final Logger logger = Logger.getLogger(ExternalParameterEntityTest.class.getName());
    static QuarkusRestClient client;

    private static final String EXPAND = "war_expand";
    private static final String NO_EXPAND = "war_no_expand";

    private String passwdFile = new File(
            TestUtil.getResourcePath(ExternalParameterEntityTest.class, "ExternalParameterEntityPasswd")).getAbsolutePath();
    private String dtdFile = new File(
            TestUtil.getResourcePath(ExternalParameterEntityTest.class, "ExternalParameterEntity.dtd")).getAbsolutePath();

    private String request = "<!DOCTYPE foo [\r" +
            "  <!ENTITY % file SYSTEM \"" + passwdFile + "\">\r" +
            "  <!ENTITY % start \"<![CDATA[\">\r" +
            "  <!ENTITY % end \"]]>\">\r" +
            "  <!ENTITY % dtd SYSTEM \"" + dtdFile + "\">\r" +
            "%dtd;\r" +
            "]>\r" +
            "<externalParameterEntityWrapper><name>&xxe;</name></externalParameterEntityWrapper>";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ExternalParameterEntityWrapper.class);
//                    war.addAsWebInfResource(ExternalParameterEntityTest.class.getPackage(),
                            "ExternalParameterEntityExpandWeb.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, ExternalParameterEntityResource.class);
                }
            });

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(ExternalParameterEntityWrapper.class);
//                    war.addAsWebInfResource(ExternalParameterEntityTest.class.getPackage(),
                            "ExternalParameterEntityNoExpandWeb.xml", "web.xml");
                    return TestUtil.finishContainerPrepare(war, null, ExternalParameterEntityResource.class);
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

    /**
     * @tpTestDetails "resteasy.document.expand.entity.references" is set to "true"
     * @tpPassCrit Passwd file should be returned by request.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testExternalParameterEntityExpand() throws Exception {
        logger.info(String.format("Request body: %s", this.request.replace('\r', '\n')));
        Response response = client.target(generateURL("/test", EXPAND)).request()
                .post(Entity.entity(this.request, MediaType.APPLICATION_XML));
        Assert.assertEquals(Status.OK, response.getStatus());
        String entity = response.readEntity(String.class);
        logger.info(String.format("Result: \"%s\"", entity.replace('\r', '\n')));
        Assert.assertEquals("root:x:0:0:root:/root:/bin/bash", entity.trim());
    }

    /**
     * @tpTestDetails "resteasy.document.expand.entity.references" is set to "false"
     * @tpPassCrit Passwd file should not be returned by request.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testExternalParameterEntityNoExpand() throws Exception {
        logger.info(String.format("Request body: %s", this.request.replace('\r', '\n')));
        Response response = client.target(generateURL("/test", NO_EXPAND)).request()
                .post(Entity.entity(this.request, MediaType.APPLICATION_XML));
        Assert.assertEquals(Status.OK, response.getStatus());
        String entity = response.readEntity(String.class);
        logger.info(String.format("Result: \"%s\"", entity.replace('\r', '\n')));
        Assert.assertEquals("", entity.trim());
    }
}
