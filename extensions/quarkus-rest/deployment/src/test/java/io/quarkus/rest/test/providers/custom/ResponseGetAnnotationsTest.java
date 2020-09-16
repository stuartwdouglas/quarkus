package io.quarkus.rest.test.providers.custom;

import java.lang.annotation.Annotation;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.custom.resource.ResponseGetAnnotationsAnnotatedClass;
import io.quarkus.rest.test.providers.custom.resource.ResponseGetAnnotationsDateClientReaderWriter;
import io.quarkus.rest.test.providers.custom.resource.ResponseGetAnnotationsDateContainerReaderWriter;
import io.quarkus.rest.test.providers.custom.resource.ResponseGetAnnotationsResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Response Get Annotations Test")
public class ResponseGetAnnotationsTest {

    static Client client;

    @BeforeAll
    public static void setup() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(ResponseGetAnnotationsAnnotatedClass.class);
            return TestUtil.finishContainerPrepare(war, null, ResponseGetAnnotationsResource.class,
                    ResponseGetAnnotationsDateContainerReaderWriter.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ResponseGetAnnotationsTest.class.getSimpleName());
    }

    @AfterAll
    public static void close() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Client registers it's own instance of Date MessageBodyReader and MessageBodyWriter. Server gets
     *                registered provider to Read and Write responses with Date and Annotations objects. Client sends POST
     *                request with
     *                Date entity and expects response with Date and Annotations from a test class.
     * @tpPassCrit The date and annotations are present in the response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Annotations")
    public void testGetAnnotations() {
        Date date = Calendar.getInstance().getTime();
        String entity = ResponseGetAnnotationsDateContainerReaderWriter.dateToString(date);
        StringBuilder sb = new StringBuilder();
        ResponseGetAnnotationsDateClientReaderWriter rw = new ResponseGetAnnotationsDateClientReaderWriter(sb);
        Response response = client.target(generateURL("/entity")).register(rw).request().post(Entity.text(entity));
        Date responseDate = response.readEntity(Date.class);
        Assertions.assertTrue(date.equals(responseDate), "The date in the response doesn't match the expected one");
        Annotation[] annotations = ResponseGetAnnotationsAnnotatedClass.class.getAnnotations();
        for (Annotation annotation : annotations) {
            String name = annotation.annotationType().getName();
            Assertions.assertTrue(sb.toString().contains(name), "The response doesn't contain the expected annotation");
        }
    }
}
