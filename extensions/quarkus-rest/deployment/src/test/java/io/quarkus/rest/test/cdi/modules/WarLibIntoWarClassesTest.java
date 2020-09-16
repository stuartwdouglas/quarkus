package io.quarkus.rest.test.cdi.modules;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

import io.quarkus.rest.test.cdi.modules.resource.CDIModulesInjectable;
import io.quarkus.rest.test.cdi.modules.resource.CDIModulesInjectableBinder;
import io.quarkus.rest.test.cdi.modules.resource.CDIModulesInjectableIntf;
import io.quarkus.rest.test.cdi.modules.resource.CDIModulesModulesResource;
import io.quarkus.rest.test.cdi.modules.resource.CDIModulesModulesResourceIntf;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test bean injection in lib of war.
 * @tpSince RESTEasy 3.0.16
 */
public class WarLibIntoWarClassesTest {
    protected static final Logger log = Logger.getLogger(WarLibIntoWarClassesTest.class.getName());

    @Deployment
    public static Archive<?> createTestArchive() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(CDIModulesInjectableBinder.class, CDIModulesInjectableIntf.class, CDIModulesInjectable.class)
                .add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        WebArchive war = TestUtil.prepareArchive(WarLibIntoWarClassesTest.class.getSimpleName())
                .addClasses(UtilityProducer.class)
                .addClasses(CDIModulesModulesResourceIntf.class, CDIModulesModulesResource.class)
                .addAsLibrary(jar);
        return war;
    }

    /**
     * @tpTestDetails Test bean injection in lib of war.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testModules() throws Exception {
        log.info("starting testModules()");
        Client client = ClientBuilder.newClient();
        WebTarget base = client
                .target(PortProviderUtil.generateURL("/modules/test/", WarLibIntoWarClassesTest.class.getSimpleName()));
        Response response = base.request().get();
        log.info("Status: " + response.getStatus());
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }
}
