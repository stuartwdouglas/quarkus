package io.quarkus.rest.test.core.interceptors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.interceptors.resource.ReaderContextArrayListEntityProvider;
import io.quarkus.rest.test.core.interceptors.resource.ReaderContextFirstReaderInterceptor;
import io.quarkus.rest.test.core.interceptors.resource.ReaderContextFirstWriterInterceptor;
import io.quarkus.rest.test.core.interceptors.resource.ReaderContextLinkedListEntityProvider;
import io.quarkus.rest.test.core.interceptors.resource.ReaderContextResource;
import io.quarkus.rest.test.core.interceptors.resource.ReaderContextSecondReaderInterceptor;
import io.quarkus.rest.test.core.interceptors.resource.ReaderContextSecondWriterInterceptor;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Interceptors
 * @tpChapter Integration tests
 * @tpTestCaseDetails Basic test for reated context
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Reader Context Test")
public class ReaderContextTest {

    public static final String readFromReader(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        String entity = br.readLine();
        br.close();
        return entity;
    }

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ReaderContextResource.class,
                    ReaderContextArrayListEntityProvider.class, ReaderContextLinkedListEntityProvider.class,
                    ReaderContextFirstReaderInterceptor.class, ReaderContextFirstWriterInterceptor.class,
                    ReaderContextSecondReaderInterceptor.class, ReaderContextSecondWriterInterceptor.class);
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ReaderContextTest.class.getSimpleName());
    }

    @AfterAll
    public static void cleanup() {
        client.close();
    }

    /**
     * @tpTestDetails Check post request.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Reader Context On Client Test")
    public void readerContextOnClientTest() {
        client = ClientBuilder.newClient();
        WebTarget target = client.target(generateURL("/resource/poststring"));
        target.register(ReaderContextFirstReaderInterceptor.class);
        target.register(ReaderContextSecondReaderInterceptor.class);
        target.register(ReaderContextArrayListEntityProvider.class);
        target.register(ReaderContextLinkedListEntityProvider.class);
        Response response = target.request().post(Entity.text("plaintext"));
        response.getHeaders().add(ReaderContextResource.HEADERNAME, ReaderContextFirstReaderInterceptor.class.getName());
        @SuppressWarnings("unchecked")
        List<String> list = response.readEntity(List.class);
        Assertions.assertTrue(ArrayList.class.isInstance(list), "Returned list in not instance of ArrayList");
        String entity = list.get(0);
        Assertions.assertTrue(entity.contains(ReaderContextSecondReaderInterceptor.class.getName()),
                "Wrong interceptor type in response");
        Assertions.assertTrue(
                entity.contains(ReaderContextSecondReaderInterceptor.class.getAnnotations()[0].annotationType().getName()),
                "Wrong interceptor annotation in response");
        client.close();
    }
}
