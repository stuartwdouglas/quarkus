package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbElementEntityMessageReader;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbElementEntityMessageWriter;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbElementReadableWritableEntity;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbElementResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Jaxb Element Test")
public class JaxbElementTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(JaxbCollectionTest.class);
            return TestUtil.finishContainerPrepare(war, null, JaxbElementEntityMessageReader.class,
                    JaxbElementEntityMessageWriter.class, JaxbElementResource.class, JaxbElementReadableWritableEntity.class);
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
        return PortProviderUtil.generateURL(path, JaxbElementTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Creates entity type JAXBElement and sends it to the server, user defined Writer and Reader implementing
     *                custom type is used
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Writer")
    public void testWriter() {
        JAXBElement<String> element = new JAXBElement<String>(new QName(""), String.class, JaxbElementResource.class.getName());
        Response response = client.target(generateURL("/resource/standardwriter")).request().post(Entity.xml(element));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
    }
}
