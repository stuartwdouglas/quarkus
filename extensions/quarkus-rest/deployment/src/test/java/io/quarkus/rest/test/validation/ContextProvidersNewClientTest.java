package io.quarkus.rest.test.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.resource.ContextProvidersResource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1119. Test for new client.
 * @tpSince RESTEasy 3.0.16
 */
public class ContextProvidersNewClientTest extends ContextProvidersTestBase {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, ContextProvidersResource.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ContextProvidersNewClientTest.class.getSimpleName());
    }

    protected Client client;

    @Before
    public void beforeTest() {
        client = ClientBuilder.newClient();
    }

    @After
    public void afterTest() {
        client.close();
    }

    @Override
    <T> T get(String path, Class<T> clazz, Annotation[] annotations) throws Exception {
        WebTarget target = client.target(generateURL(path));
        ClientResponse response = (ClientResponse) target.request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        T entity = response.readEntity(clazz, null, annotations);
        return entity;
    }

    @Override
    <S, T> T post(String path, S payload, MediaType mediaType,
            Class<T> returnType, Type genericReturnType, Annotation[] annotations) throws Exception {
        WebTarget target = client.target(generateURL(path));
        Entity<S> entity = Entity.entity(payload, mediaType, annotations);
        ClientResponse response = (ClientResponse) target.request().post(entity);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        T result;
        if (genericReturnType != null) {
            result = response.readEntity(returnType, genericReturnType, null);
        } else {
            result = response.readEntity(returnType);
        }
        return result;
    }

}
