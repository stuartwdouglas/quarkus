package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.parsing.ObjectFactory;
import io.quarkus.rest.test.providers.jaxb.resource.parsing.ParsingAbstractData;
import io.quarkus.rest.test.providers.jaxb.resource.parsing.ParsingDataCollectionPackage;
import io.quarkus.rest.test.providers.jaxb.resource.parsing.ParsingDataCollectionRecord;
import io.quarkus.rest.test.providers.jaxb.resource.parsing.ParsingStoreResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-143
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Parsing Test")
public class ParsingTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(ParsingAbstractData.class);
            war.addClass(ParsingDataCollectionPackage.class);
            war.addClass(ParsingDataCollectionRecord.class);
            war.addClass(ObjectFactory.class);
            return TestUtil.finishContainerPrepare(war, null, ParsingStoreResource.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ParsingTest.class.getSimpleName());
    }

    private static final String XML_CONTENT_DEFAULT_NS = "<ParsingDataCollectionPackage xmlns=\"http://www.example.org/ParsingDataCollectionPackage\">\n"
            + "  <sourceID>System A</sourceID>\n" + "  <eventID>Exercise B</eventID>\n" + "  <dataRecords>\n"
            + "     <ParsingDataCollectionRecord>\n" + "        <timestamp>2008-08-13T12:24:00</timestamp>\n"
            + "        <collectedData>Operator pushed easy button</collectedData>\n" + "     </ParsingDataCollectionRecord>\n"
            + "  </dataRecords>\n" + "</ParsingDataCollectionPackage>";

    private static final String XML_CONTENT = "<ns:ParsingDataCollectionPackage xmlns:ns=\"http://www.example.org/ParsingDataCollectionPackage\">\n"
            + "  <sourceID>System A</sourceID>\n" + "  <eventID>Exercise B</eventID>\n" + "  <dataRecords>\n"
            + "     <ParsingDataCollectionRecord>\n" + "        <timestamp>2008-08-13T12:24:00</timestamp>\n"
            + "        <collectedData>Operator pushed easy button</collectedData>\n" + "     </ParsingDataCollectionRecord>\n"
            + "  </dataRecords>\n" + "</ns:ParsingDataCollectionPackage>";

    /**
     * @tpTestDetails Check XML parsing
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Wire")
    public void testWire() throws Exception {
        {
            Response response = client.target(generateURL("/storeXML")).request()
                    .post(Entity.entity(XML_CONTENT, "application/xml"));
            Assertions.assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            response.close();
        }
        {
            Response response = client.target(generateURL("/storeXML/abstract")).request()
                    .post(Entity.entity(XML_CONTENT, "application/xml"));
            Assertions.assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            response.close();
        }
    }
}
