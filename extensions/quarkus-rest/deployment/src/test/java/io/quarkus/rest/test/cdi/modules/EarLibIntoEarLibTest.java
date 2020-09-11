package io.quarkus.rest.test.cdi.modules;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.jboss.arquillian.container.test.api.Deployment;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestApplication;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;

import io.quarkus.rest.test.cdi.modules.resource.CDIModulesInjectable;
import io.quarkus.rest.test.cdi.modules.resource.CDIModulesInjectableBinder;
import io.quarkus.rest.test.cdi.modules.resource.CDIModulesInjectableIntf;
import io.quarkus.rest.test.cdi.modules.resource.CDIModulesModulesResource;
import io.quarkus.rest.test.cdi.modules.resource.CDIModulesModulesResourceIntf;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import io.quarkus.rest.test.simple.PortProviderUtil;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test bean injection from lib to lib in ear.
 * @tpSince RESTEasy 3.0.16
 */
public class EarLibIntoEarLibTest {
    protected static final org.apache.logging.log4j.Logger log = LogManager.getLogger(EarLibIntoEarLibTest.class.getName());

    @Deployment
    public static Archive<?> createTestArchive() {
        JavaArchive fromJar = ShrinkWrap.create(JavaArchive.class, "from.jar")
                .addClasses(CDIModulesInjectableBinder.class, CDIModulesInjectableIntf.class, CDIModulesInjectable.class)
                .add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        JavaArchive toJar = ShrinkWrap.create(JavaArchive.class, "to.jar")
                .addClasses(UtilityProducer.class)
                .addClasses(CDIModulesModulesResourceIntf.class, CDIModulesModulesResource.class)
                .addClasses(TestApplication.class, PortProviderUtil.class)
                .add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
                .addAsLibrary(fromJar)
                .addAsLibrary(toJar);
        // This is needed, because we don't use TestUtil.finishContainerPrepare(...) so TestApplication calls getContextClassLoader() directly.
        ear.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(
                new RuntimePermission("getClassLoader")), "permissions.xml");
        return ear;
    }

    /**
     * @tpTestDetails Test bean injection from lib to lib in ear.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testModules() throws Exception {
        log.info("starting testModules()");

        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(PortProviderUtil.generateURL("/modules/test/", "test"));
        Response response = base.request().get();
        log.info("Status: " + response.getStatus());
        assertEquals(Status.OK, response.getStatus());
        response.close();
        client.close();
    }
}
