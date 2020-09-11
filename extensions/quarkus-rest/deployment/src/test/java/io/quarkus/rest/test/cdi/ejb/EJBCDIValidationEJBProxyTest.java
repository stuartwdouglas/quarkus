package io.quarkus.rest.test.cdi.ejb;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.ejb.resource.EJBCDIValidationEJBProxyGreeterResource;
import io.quarkus.rest.test.cdi.ejb.resource.EJBCDIValidationEJBProxyGreeting;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails EJB, CDI, Validation, and RESTEasy integration test: RESTEASY-2358
 * @tpSince RESTEasy 4.5.0
 */
public class EJBCDIValidationEJBProxyTest {

    private static final String VALID_REQUEST = "{\n"
            + "\"name\":\"Hugo\"\n"
            + "}";

    private static final String INVALID_REQUEST = "{\n"
            + "\"name\":\"123456789010\"\n"
            + "}";

    private static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(EJBCDIValidationEJBProxyGreeting.class);
                    war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
                    war.setWebXML(EJBCDIValidationEJBProxyTest.class.getPackage(), "web.xml");

                    return TestUtil.finishContainerPrepare(war, null,
                            EJBCDIValidationEJBProxyGreeterResource.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, EJBCDIValidationEJBProxyTest.class.getSimpleName());
    }

    @BeforeClass
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @Test
    public void buggyBeanValidation() {
        final WebTarget greeterTarget = client.target(generateURL("/greeter"));

        Response response = greeterTarget.request().post(Entity.entity(INVALID_REQUEST, MediaType.APPLICATION_JSON));
        String answer = response.readEntity(String.class);
        ViolationReport r = new ViolationReport(answer);
        TestUtil.countViolations(r, 0, 0, 1, 0);

        response = greeterTarget.request().post(Entity.entity(VALID_REQUEST, MediaType.APPLICATION_JSON));
        final String helloHugo = response.readEntity(String.class);
        Assert.assertEquals("Hello Hugo!", helloHugo);

        response = greeterTarget.request().post(Entity.entity(INVALID_REQUEST, MediaType.APPLICATION_JSON));
        answer = response.readEntity(String.class);
        r = new ViolationReport(answer);
        TestUtil.countViolations(r, 0, 0, 1, 0);

        response = greeterTarget.request().post(Entity.entity(INVALID_REQUEST, MediaType.APPLICATION_JSON));
        answer = response.readEntity(String.class);
        r = new ViolationReport(answer);
        TestUtil.countViolations(r, 0, 0, 1, 0);
    }
}
