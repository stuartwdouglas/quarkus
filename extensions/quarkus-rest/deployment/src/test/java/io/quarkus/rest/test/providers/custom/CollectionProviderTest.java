package io.quarkus.rest.test.providers.custom;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.custom.resource.CollectionProviderCollectionWriter;
import io.quarkus.rest.test.providers.custom.resource.CollectionProviderIncorrectCollectionWriter;
import io.quarkus.rest.test.providers.custom.resource.CollectionProviderResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class CollectionProviderTest {

    public static String getPathValue(Annotation[] annotations) {
        return getSpecifiedAnnotationValue(annotations, Path.class);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getSpecifiedAnnotation(
            Annotation[] annotations, Class<T> clazz) {
        T t = null;
        for (Annotation a : annotations) {
            if (a.annotationType() == clazz) {
                t = (T) a;
            }
        }
        return t != null ? t : null;
    }

    public static <T extends Annotation> String getSpecifiedAnnotationValue(
            Annotation[] annotations, Class<T> clazz) {
        T t = getSpecifiedAnnotation(annotations, clazz);
        try {
            Method m = clazz.getMethod("value");
            return (String) m.invoke(t);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean checkOther(Class<?> type, Type genericType) {
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType pType = (ParameterizedType) genericType;
        boolean ok = pType.getRawType().equals(LinkedList.class);
        ok &= pType.getActualTypeArguments()[0].equals(String.class);
        return ok;
    }

    public static boolean checkResponseNongeneric(Class<?> type,
            Type genericType) {
        boolean ok = genericType.equals(LinkedList.class);
        ok &= type.equals(LinkedList.class);
        return ok;
    }

    public static boolean checkGeneric(Class<?> type, Type genericType) {
        if (ParameterizedType.class.isInstance(genericType)) {
            genericType = ((ParameterizedType) genericType).getRawType();
        }
        boolean ok = genericType.getClass().equals(List.class)
                || genericType.equals(LinkedList.class);
        ok &= type.equals(LinkedList.class);
        return ok;
    }

    static Client client;

    @BeforeClass
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(CollectionProviderTest.class);
                    return TestUtil.finishContainerPrepare(war, null, CollectionProviderResource.class,
                            CollectionProviderIncorrectCollectionWriter.class, CollectionProviderCollectionWriter.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, CollectionProviderTest.class.getSimpleName());
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    /**
     * @tpTestDetails Client sends GET request to the server, server sends LinkedList with String items. Two message
     *                body readers are registered. One always returns isWritable false, so the other one has to be used.
     * @tpPassCrit Correct MessageBodyReader is used for writing response of the type LinkedList<String>
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testGenericTypeDefault() {
        Response response = client.target(generateURL("/resource/response/linkedlist")).request().get();
        String val = response.readEntity(String.class);
        Assert.assertEquals("OK", val);
    }

    /**
     * @tpTestDetails Client sends GET request to the server, server sends response with GenericEntity of the type
     *                LinkedList with String items. Two message body readers are registered. One always returns isWritable
     *                false,
     *                so the other one has to be used.
     * @tpPassCrit Correct MessageBodyReader is used for writing response with GenericEntity of the type LinkedList<String>
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testGenericTypeResponse() {
        Response response = client.target(generateURL("/resource/genericentity/linkedlist")).request().get();
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String val = response.readEntity(String.class);
        Assert.assertEquals("OK", val);
    }

}
