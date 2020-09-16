package io.quarkus.rest.test.cdi.interceptors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.function.Supplier;

import javax.swing.text.Utilities;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorBook;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorBookReader;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorBookReaderInterceptor;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorBookReaderInterceptorInterceptor;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorBookWriter;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorBookWriterInterceptor;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorBookWriterInterceptorInterceptor;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorClassBinding;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorClassInterceptorStereotype;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorClassMethodInterceptorStereotype;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorFilterBinding;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorFour;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorLifecycleBinding;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorMethodBinding;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorOne;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorPostConstructInterceptor;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorPreDestroyInterceptor;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorReaderBinding;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorRequestFilter;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorRequestFilterInterceptor;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorRequestFilterInterceptorBinding;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorResource;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorResponseFilter;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorResponseFilterInterceptor;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorResponseFilterInterceptorBinding;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorStereotyped;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorThree;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorTwo;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorVisitList;
import io.quarkus.rest.test.cdi.interceptors.resource.InterceptorWriterBinding;
import io.quarkus.rest.test.cdi.util.Constants;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Interceptors test.
 * @tpSince RESTEasy 3.0.16
 */
public class InterceptorTest {
    protected static final Logger log = Logger.getLogger(InterceptorTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(Constants.class, UtilityProducer.class, Utilities.class, InterceptorVisitList.class)
                            .addClasses(InterceptorResource.class, InterceptorOne.class, InterceptorTwo.class)
                            .addClasses(InterceptorClassBinding.class, InterceptorMethodBinding.class, InterceptorThree.class,
                                    InterceptorFour.class)
                            .addClasses(InterceptorFilterBinding.class, InterceptorRequestFilterInterceptorBinding.class)
                            .addClasses(InterceptorResponseFilterInterceptorBinding.class)
                            .addClasses(InterceptorRequestFilterInterceptor.class, InterceptorResponseFilterInterceptor.class,
                                    InterceptorRequestFilter.class, InterceptorResponseFilter.class)
                            .addClasses(InterceptorReaderBinding.class, InterceptorWriterBinding.class)
                            .addClasses(InterceptorBook.class, InterceptorBookReader.class, InterceptorBookWriter.class)
                            .addClasses(InterceptorBookReaderInterceptor.class, InterceptorBookWriterInterceptor.class)
                            .addClasses(InterceptorBookReaderInterceptorInterceptor.class,
                                    InterceptorBookWriterInterceptorInterceptor.class)
                            .addClasses(InterceptorClassInterceptorStereotype.class,
                                    InterceptorClassMethodInterceptorStereotype.class, InterceptorStereotyped.class)
                            .addClasses(InterceptorLifecycleBinding.class, InterceptorPostConstructInterceptor.class,
                                    InterceptorPreDestroyInterceptor.class)
//                            .addAsWebInfResource(InterceptorTest.class.getPackage(), "interceptorBeans.xml", "beans.xml");
                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, InterceptorTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails One item is stored and load to collection in resources.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testInterceptors() throws Exception {
        Client client = ClientBuilder.newClient();

        // Create book.
        InterceptorBook book = new InterceptorBook("RESTEasy: the Sequel");
        WebTarget base = client.target(generateURL("/create/"));
        Response response = base.request().post(Entity.entity(book, Constants.MEDIA_TYPE_TEST_XML));
        assertEquals(200, response.getStatus());
        int id = response.readEntity(int.class);
        assertThat("Id of stored book is wrong.", 0, is(id));

        // Retrieve book.
        base = client.target(generateURL("/book/" + id));
        response = base.request().accept(Constants.MEDIA_TYPE_TEST_XML).get();
        assertEquals(200, response.getStatus());
        InterceptorBook result = response.readEntity(InterceptorBook.class);
        assertEquals("Wrong book is received.", book, result);

        // check interceptors
        base = client.target(generateURL("/test/"));
        response = base.request().post(Entity.text(new String()));
        assertEquals(200, response.getStatus());

        client.close();
    }
}
