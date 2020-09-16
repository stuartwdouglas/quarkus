package io.quarkus.rest.test.sourceProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.transform.stream.StreamSource;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.sourceProvider.resource.Book;
import io.quarkus.rest.test.sourceProvider.resource.BookResource;
import io.quarkus.rest.test.sourceProvider.resource.SourceProviderApp;
import io.quarkus.test.QuarkusUnitTest;

@DisplayName("Source Provider Test")
public class SourceProviderTest {

    private static Client client;

    private String book = "<book><title>Monkey kingdom</title></book>";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, SourceProviderApp.class, BookResource.class, Book.class);
        }
    });

    @BeforeEach
    public void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void close() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SourceProviderTest.class.getSimpleName());
    }

    @Test
    @DisplayName("Test Source With String Reader")
    public void testSourceWithStringReader() throws Exception {
        Response response = client.target(generateURL("/test")).request()
                .post(Entity.entity(new StreamSource(new StringReader(book)), "application/*+xml"));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        Assertions.assertTrue(
                entity.contentEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><book><title>Monkey kingdom</title></book>"));
    }

    @Test
    @DisplayName("Test Source With Input Stream")
    public void testSourceWithInputStream() throws Exception {
        InputStream stream = new ByteArrayInputStream(book.getBytes(StandardCharsets.UTF_8));
        Response response = client.target(generateURL("/test")).request()
                .post(Entity.entity(new StreamSource(stream), "application/*+xml"));
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        Assertions.assertTrue(
                entity.contentEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><book><title>Monkey kingdom</title></book>"));
    }
}
