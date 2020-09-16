package io.quarkus.rest.test.validation;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.api.validation.ViolationReport;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.resource.ValidationCoreFooReaderWriter;
import io.quarkus.rest.test.validation.resource.ValidationOnGetterValidateExecutableResource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Response
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test getter validation
 * @tpSince RESTEasy 3.0.16
 */
public class ValidationOnGetterTest {
    QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, ValidationOnGetterValidateExecutableResource.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient().register(ValidationCoreFooReaderWriter.class);
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ValidationOnGetterTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test xml media type.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testGetter() throws Exception {
        Response response = client.target(generateURL("/resource/executable/getter")).request()
                .accept(MediaType.APPLICATION_XML).get();
        ViolationReport report = response.readEntity(ViolationReport.class);
        TestUtil.countViolations(report, 1, 0, 0, 0);
        response.close();
    }
}
