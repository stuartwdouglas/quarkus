package io.quarkus.rest.test.cdi.inheritence;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceBook;
import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceBookSpecialized;
import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceInheritanceResource;
import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceSelectBook;
import io.quarkus.rest.test.cdi.inheritence.resource.CDIInheritenceStereotypeAlternative;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails This class tests CDI inheritance (BookSpecialized extends Book)
 * @tpSince RESTEasy 3.0.16
 */
public class SpecializedInheritanceTest {
    protected static final Logger log = LogManager.getLogger(SpecializedInheritanceTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(UtilityProducer.class)
                            .addClasses(CDIInheritenceSelectBook.class, CDIInheritenceStereotypeAlternative.class)
                            .addClasses(CDIInheritenceBook.class, CDIInheritenceBookSpecialized.class,
                                    CDIInheritenceInheritanceResource.class)
                            .addAsWebInfResource(SpecializedInheritanceTest.class.getPackage(), "specializedBeans.xml",
                                    "beans.xml");
                    return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
                }
            });

    /**
     * @tpTestDetails Client get request. Resource check inheritance bean on server.
     * @tpPassCrit Response status should not contain error.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testAlternative() throws Exception {
        Client client = ClientBuilder.newClient();
        log.info("starting testAlternative()");
        WebTarget base = client
                .target(PortProviderUtil.generateURL("/specialized/", SpecializedInheritanceTest.class.getSimpleName()));
        Response response = base.request().get();
        log.info("Response status: " + response.getStatus());
        assertEquals(Status.OK, response.getStatus());
        response.close();
        client.close();
    }
}
