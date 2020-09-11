package io.quarkus.rest.test.core.basic;

import java.util.Collection;
import java.util.function.Supplier;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.basic.resource.AnnotationInheritanceGenericsAbstract;
import io.quarkus.rest.test.core.basic.resource.AnnotationInheritanceGenericsEntity;
import io.quarkus.rest.test.core.basic.resource.AnnotationInheritanceGenericsImpl;
import io.quarkus.rest.test.core.basic.resource.AnnotationInheritanceGenericsInterface;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for JAX-RS annotation inheritance with generics.
 * @tpSince RESTEasy 4.0.0
 */
public class AnnotationInheritanceGenericsTest {

    private static final String TEST_NAME = AnnotationInheritanceGenericsTest.class.getSimpleName();

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(
                            AnnotationInheritanceGenericsEntity.class,
                            AnnotationInheritanceGenericsInterface.class,
                            AnnotationInheritanceGenericsAbstract.class);

                    return TestUtil.finishContainerPrepare(war, null, AnnotationInheritanceGenericsImpl.class);
                }
            });

    protected Client client;

    @Before
    public void beforeTest() {
        client = ClientBuilder.newClient();
    }

    @After
    public void afterTest() {
        client.close();
        client = null;
    }

    @Test
    public void testGetCollectionInterface() {
        final Collection<AnnotationInheritanceGenericsEntity> entityList = invokeRequest(
                AnnotationInheritanceGenericsImpl.class,
                null,
                HttpMethod.GET,
                null,
                new GenericType<Collection<AnnotationInheritanceGenericsEntity>>() {
                });

        Assert.assertNotNull("Response entity list must not be null", entityList);
        Assert.assertThat("Response entity list must contain exactly one element", entityList, Matchers.hasSize(1));

        final AnnotationInheritanceGenericsEntity entity = entityList.iterator().next();
        Assert.assertEquals(
                "Response entity ID must match method id",
                AnnotationInheritanceGenericsImpl.METHOD_ID_INTERFACE_GET_COLLECTION,
                entity.getId());
    }

    @Test
    public void testGetSingleInterface() {
        final AnnotationInheritanceGenericsEntity entity = invokeRequest(
                AnnotationInheritanceGenericsImpl.class,
                "1",
                HttpMethod.GET,
                null,
                new GenericType<AnnotationInheritanceGenericsEntity>() {
                });

        Assert.assertNotNull("Response entity must not be null", entity);
        Assert.assertEquals(
                "Response entity ID must match method id",
                AnnotationInheritanceGenericsImpl.METHOD_ID_INTERFACE_GET_SINGLE,
                entity.getId());
    }

    @Test
    public void testPostInterface() {
        final AnnotationInheritanceGenericsEntity requestEntity = new AnnotationInheritanceGenericsEntity();

        final AnnotationInheritanceGenericsEntity entity = invokeRequest(
                AnnotationInheritanceGenericsImpl.class,
                null,
                HttpMethod.POST,
                requestEntity,
                new GenericType<AnnotationInheritanceGenericsEntity>() {
                });

        Assert.assertNotNull("Response entity must not be null", entity);
        Assert.assertEquals(
                "Response entity ID must match method id",
                AnnotationInheritanceGenericsImpl.METHOD_ID_INTERFACE_POST,
                entity.getId());
    }

    @Test
    public void testPutInterface() {
        final AnnotationInheritanceGenericsEntity requestEntity = new AnnotationInheritanceGenericsEntity();

        final AnnotationInheritanceGenericsEntity entity = invokeRequest(
                AnnotationInheritanceGenericsImpl.class,
                "1",
                HttpMethod.PUT,
                requestEntity,
                new GenericType<AnnotationInheritanceGenericsEntity>() {
                });

        Assert.assertNotNull("Response entity must not be null", entity);
        Assert.assertEquals(
                "Response entity ID must match method id",
                AnnotationInheritanceGenericsImpl.METHOD_ID_INTERFACE_PUT,
                entity.getId());
    }

    @Test
    public void testPutAbstract() {
        final AnnotationInheritanceGenericsEntity requestEntity = new AnnotationInheritanceGenericsEntity();

        final AnnotationInheritanceGenericsEntity entity = invokeRequest(
                AnnotationInheritanceGenericsImpl.class,
                null,
                HttpMethod.PUT,
                requestEntity,
                new GenericType<AnnotationInheritanceGenericsEntity>() {
                });

        Assert.assertNotNull("Response entity must not be null", entity);
        Assert.assertEquals(
                "Response entity ID must match method id",
                AnnotationInheritanceGenericsImpl.METHOD_ID_ABSTRACT_PUT,
                entity.getId());
    }

    private <T> T invokeRequest(
            final Class<?> resourceClass,
            final String pathArgument,
            final String httpMethod,
            final Object requestEntity,
            final GenericType<T> responseClass) {
        final Path resourcePathAnnotation = resourceClass.getAnnotation(Path.class);
        final String resourcePath = resourcePathAnnotation.value().startsWith("/") ? resourcePathAnnotation.value()
                : '/' + resourcePathAnnotation.value();
        final String resourceUrl = PortProviderUtil.generateURL(resourcePath, TEST_NAME);

        final WebTarget target = client.target(resourceUrl).path((pathArgument != null) ? pathArgument : "");

        final Invocation requestInvocation;
        if (requestEntity == null) {
            requestInvocation = target.request().build(httpMethod);
        } else {
            requestInvocation = target.request(MediaType.APPLICATION_XML_TYPE).build(httpMethod, Entity.xml(requestEntity));
        }

        if (responseClass != null) {
            final T responseEntity = requestInvocation.invoke(responseClass);
            return responseEntity;
        } else {
            requestInvocation.invoke();
            return null;
        }
    }

}
