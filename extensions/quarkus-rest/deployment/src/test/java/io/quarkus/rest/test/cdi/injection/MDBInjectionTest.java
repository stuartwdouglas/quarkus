package io.quarkus.rest.test.cdi.injection;

import java.net.URI;

import javax.annotation.Resource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionBook;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionBookBag;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionBookBagLocal;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionBookCollection;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionBookMDB;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionBookReader;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionBookResource;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionBookWriter;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionDependentScoped;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionNewBean;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionResourceProducer;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionScopeInheritingStereotype;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionScopeStereotype;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionStatefulEJB;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionStereotypedApplicationScope;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionStereotypedDependentScope;
import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionUnscopedResource;
import io.quarkus.rest.test.cdi.util.Constants;
import io.quarkus.rest.test.cdi.util.Counter;
import io.quarkus.rest.test.cdi.util.PersistenceUnitProducer;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails This class tests the use of MDBs with Resteasy, including the injection of a
 *                    JAX-RS resource into an MDB.
 * @tpSince RESTEasy 3.0.16
 */
public class MDBInjectionTest extends AbstractInjectionTestBase {
    protected static final Logger log = LogManager.getLogger(MDBInjectionTest.class.getName());

    static Client client;

    @SuppressWarnings(value = "unchecked")
    @Deployment
    public static Archive<?> createTestArchive() throws Exception {
        initQueue();
        WebArchive war = TestUtil.prepareArchive(MDBInjectionTest.class.getSimpleName());
        war.addClass(AbstractInjectionTestBase.class)
                .addClasses(CDIInjectionBook.class, CDIInjectionBookResource.class, Constants.class, UtilityProducer.class)
                .addClasses(Counter.class, CDIInjectionBookCollection.class, CDIInjectionBookReader.class,
                        CDIInjectionBookWriter.class)
                .addClasses(CDIInjectionDependentScoped.class, CDIInjectionStatefulEJB.class,
                        CDIInjectionUnscopedResource.class)
                .addClasses(CDIInjectionBookBagLocal.class, CDIInjectionBookBag.class)
                .addClasses(CDIInjectionBookMDB.class)
                .addClasses(CDIInjectionNewBean.class)
                .addClasses(CDIInjectionScopeStereotype.class, CDIInjectionScopeInheritingStereotype.class)
                .addClasses(CDIInjectionStereotypedApplicationScope.class, CDIInjectionStereotypedDependentScope.class)
                .addClasses(Resource.class, CDIInjectionResourceProducer.class, PersistenceUnitProducer.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(InjectionTest.class.getPackage(), "persistence.xml", "META-INF/persistence.xml");
        String host = PortProviderUtil.getHost();
        if (PortProviderUtil.isIpv6()) {
            host = String.format("[%s]", host);
        }

        return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
    }

    @ArquillianResource
    URI baseUri;

    @BeforeClass
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @Before
    public void preparePersistenceTest() throws Exception {
        log.trace("Dumping old records.");
        WebTarget base = client.target(baseUri.resolve("empty/"));
        Response response = base.request().post(Entity.text(""));
        response.close();
    }

    /**
     * @tpTestDetails Tests the injection of JMS Producers, Consumers, Queues, and MDBs using producer fields and methods.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testMDB() throws Exception {
        log.trace("starting testJMS()");

        // Send a book title.
        WebTarget base = client.target(baseUri.resolve("produceMessage/"));
        String title = "Dead Man Lounging";
        CDIInjectionBook book = new CDIInjectionBook(23, title);
        Response response = base.request().post(Entity.entity(book, Constants.MEDIA_TYPE_TEST_XML));
        log.trace("status: " + response.getStatus());
        log.trace(response.readEntity(String.class));
        Assert.assertEquals(Status.OK, response.getStatus());
        response.close();

        // Verify that the received book title is the one that was sent.
        base = client.target(baseUri.resolve("mdb/consumeMessage/"));
        response = base.request().get();
        log.trace("status: " + response.getStatus());
        Assert.assertEquals(Status.OK, response.getStatus());
        Assert.assertEquals("Wrong response", title, response.readEntity(String.class));
        response.close();
    }
}
