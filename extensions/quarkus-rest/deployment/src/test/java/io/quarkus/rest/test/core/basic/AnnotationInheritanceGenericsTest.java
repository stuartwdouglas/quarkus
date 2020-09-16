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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("Annotation Inheritance Generics Test")
public class AnnotationInheritanceGenericsTest {

    private static final String TEST_NAME = AnnotationInheritanceGenericsTest.class.getSimpleName();

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(AnnotationInheritanceGenericsEntity.class, AnnotationInheritanceGenericsInterface.class,
                    AnnotationInheritanceGenericsAbstract.class);
            return TestUtil.finishContainerPrepare(war, null, AnnotationInheritanceGenericsImpl.class);
        }
    });

    protected Client client;

    @BeforeEach
    public void beforeTest() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void afterTest() {
        client.close();
        client = null;
    }

    @Test
    @DisplayName("Test Get Collection Interface")
    public void testGetCollectionInterface() {
        final Collection<AnnotationInheritanceGenericsEntity> entityList = invokeRequest(
                AnnotationInheritanceGenericsImpl.class, null, HttpMethod.GET, null,
                new GenericType<Collection<AnnotationInheritanceGenericsEntity>>() {
                });
        Assertions.assertNotNull(entityList, "Response entity list must not be null");
        Assertions.assertEquals(1, entityList.size(), "Response entity list must contain exactly one element");
        final AnnotationInheritanceGenericsEntity entity = entityList.iterator().next();
        Assertions.assertEquals(AnnotationInheritanceGenericsImpl.METHOD_ID_INTERFACE_GET_COLLECTION, entity.getId(),
                "Response entity ID must match method id");
    }

    @Test
    @DisplayName("Test Get Single Interface")
    public void testGetSingleInterface() {
        final AnnotationInheritanceGenericsEntity entity = invokeRequest(AnnotationInheritanceGenericsImpl.class, "1",
                HttpMethod.GET, null, new GenericType<AnnotationInheritanceGenericsEntity>() {
                });
        Assertions.assertNotNull(entity, "Response entity must not be null");
        Assertions.assertEquals(AnnotationInheritanceGenericsImpl.METHOD_ID_INTERFACE_GET_SINGLE, entity.getId(),
                "Response entity ID must match method id");
    }

    @Test
    @DisplayName("Test Post Interface")
    public void testPostInterface() {
        final AnnotationInheritanceGenericsEntity requestEntity = new AnnotationInheritanceGenericsEntity();
        final AnnotationInheritanceGenericsEntity entity = invokeRequest(AnnotationInheritanceGenericsImpl.class, null,
                HttpMethod.POST, requestEntity, new GenericType<AnnotationInheritanceGenericsEntity>() {
                });
        Assertions.assertNotNull(entity, "Response entity must not be null");
        Assertions.assertEquals(AnnotationInheritanceGenericsImpl.METHOD_ID_INTERFACE_POST, entity.getId(),
                "Response entity ID must match method id");
    }

    @Test
    @DisplayName("Test Put Interface")
    public void testPutInterface() {
        final AnnotationInheritanceGenericsEntity requestEntity = new AnnotationInheritanceGenericsEntity();
        final AnnotationInheritanceGenericsEntity entity = invokeRequest(AnnotationInheritanceGenericsImpl.class, "1",
                HttpMethod.PUT, requestEntity, new GenericType<AnnotationInheritanceGenericsEntity>() {
                });
        Assertions.assertNotNull(entity, "Response entity must not be null");
        Assertions.assertEquals(AnnotationInheritanceGenericsImpl.METHOD_ID_INTERFACE_PUT, entity.getId(),
                "Response entity ID must match method id");
    }

    @Test
    @DisplayName("Test Put Abstract")
    public void testPutAbstract() {
        final AnnotationInheritanceGenericsEntity requestEntity = new AnnotationInheritanceGenericsEntity();
        final AnnotationInheritanceGenericsEntity entity = invokeRequest(AnnotationInheritanceGenericsImpl.class, null,
                HttpMethod.PUT, requestEntity, new GenericType<AnnotationInheritanceGenericsEntity>() {
                });
        Assertions.assertNotNull(entity, "Response entity must not be null");
        Assertions.assertEquals(AnnotationInheritanceGenericsImpl.METHOD_ID_ABSTRACT_PUT, entity.getId(),
                "Response entity ID must match method id");
    }

    private <T> T invokeRequest(final Class<?> resourceClass, final String pathArgument, final String httpMethod,
            final Object requestEntity, final GenericType<T> responseClass) {
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
