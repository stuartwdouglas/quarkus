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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.sourceProvider.resource.Book;
import io.quarkus.rest.test.sourceProvider.resource.BookResource;
import io.quarkus.rest.test.sourceProvider.resource.SourceProviderApp;
import io.quarkus.test.QuarkusUnitTest;

public class SourceProviderTest {

    private static Client client;
    private String book = "<book><title>Monkey kingdom</title></book>";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, SourceProviderApp.class,
                            BookResource.class, Book.class);
                }
            });

    @Before
    public void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @After
    public void close() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, SourceProviderTest.class.getSimpleName());
    }

    @Test
    public void testSourceWithStringReader() throws Exception {
        Response response = client.target(generateURL("/test")).request()
                .post(Entity.entity(new StreamSource(new StringReader(book)), "application/*+xml"));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        Assert.assertTrue(
                entity.contentEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><book><title>Monkey kingdom</title></book>"));
    }

    @Test
    public void testSourceWithInputStream() throws Exception {
        InputStream stream = new ByteArrayInputStream(book.getBytes(StandardCharsets.UTF_8));
        Response response = client.target(generateURL("/test")).request()
                .post(Entity.entity(new StreamSource(stream), "application/*+xml"));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        Assert.assertTrue(
                entity.contentEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><book><title>Monkey kingdom</title></book>"));
    }
}
