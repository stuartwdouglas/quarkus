package io.quarkus.rest.test.validation;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.resource.ValidationNullErrorValueResourceWithNullFieldAndProperty;
import io.quarkus.rest.test.validation.resource.ValidationNullErrorValueResourceWithNullParameterAndReturnValue;

/**
 * @tpSubChapter Validation
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for validation of null error value
 * @tpSince RESTEasy 3.0.16
 */
public class ValidationNullErrorValueTest {

    static QuarkusRestClient client;

    public static Archive<?> generateArchive(Class<?> clazz) {
        WebArchive war = TestUtil.prepareArchive(clazz.getSimpleName());
        return TestUtil.finishContainerPrepare(war, null, clazz);
    }

    @BeforeClass
    public static void before() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    @Deployment(name = "ValidationNullErrorValueResourceWithNullFieldAndProperty")
    public static Archive<?> createTestArchiveDefault() {
        return generateArchive(ValidationNullErrorValueResourceWithNullFieldAndProperty.class);
    }

    @Deployment(name = "ValidationNullErrorValueResourceWithNullParameterAndReturnValue")
    public static Archive<?> createTestArchiveFalse() {
        return generateArchive(ValidationNullErrorValueResourceWithNullParameterAndReturnValue.class);
    }

    /**
     * @tpTestDetails Test null field and property.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNullFieldAndProperty() throws Exception {
        Response response = client.target(PortProviderUtil.generateURL("/get",
                ValidationNullErrorValueResourceWithNullFieldAndProperty.class.getSimpleName())).request()
                .accept(MediaType.APPLICATION_XML).get();
        ViolationReport report = response.readEntity(ViolationReport.class);
        TestUtil.countViolations(report, 2, 0, 0, 0);
        response.close();
    }

    /**
     * @tpTestDetails Test null return value
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNullParameterAndReturnValue() throws Exception {
        {
            // Null query parameter
            Response response = client.target(PortProviderUtil.generateURL("/post",
                    ValidationNullErrorValueResourceWithNullParameterAndReturnValue.class.getSimpleName())).request()
                    .accept(MediaType.APPLICATION_XML).post(Entity.text(new String()));
            ViolationReport report = response.readEntity(ViolationReport.class);
            TestUtil.countViolations(report, 0, 0, 1, 0);
            response.close();
        }

        {
            // Null return value
            Response response = client.target(PortProviderUtil.generateURL("/get",
                    ValidationNullErrorValueResourceWithNullParameterAndReturnValue.class.getSimpleName())).request()
                    .accept(MediaType.APPLICATION_XML).get();
            ViolationReport report = response.readEntity(ViolationReport.class);
            TestUtil.countViolations(report, 0, 0, 0, 1);
            response.close();
        }
    }
}
