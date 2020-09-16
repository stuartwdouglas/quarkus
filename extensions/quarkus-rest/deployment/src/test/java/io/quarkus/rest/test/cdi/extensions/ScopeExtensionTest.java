package io.quarkus.rest.test.cdi.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.extensions.resource.ScopeExtensionObsolescent;
import io.quarkus.rest.test.cdi.extensions.resource.ScopeExtensionObsolescentAfterThreeUses;
import io.quarkus.rest.test.cdi.extensions.resource.ScopeExtensionObsolescentAfterTwoUses;
import io.quarkus.rest.test.cdi.extensions.resource.ScopeExtensionPlannedObsolescenceContext;
import io.quarkus.rest.test.cdi.extensions.resource.ScopeExtensionPlannedObsolescenceExtension;
import io.quarkus.rest.test.cdi.extensions.resource.ScopeExtensionPlannedObsolescenceScope;
import io.quarkus.rest.test.cdi.extensions.resource.ScopeExtensionResource;
import io.quarkus.rest.test.cdi.util.Utilities;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails CDIScopeExtensionTest tests that Resteasy interacts well with beans in
 *                    a user defined scope.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Scope Extension Test")
public class ScopeExtensionTest {

    @Inject
    Logger log;

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(UtilityProducer.class, Utilities.class, PortProviderUtil.class)
                    .addClasses(ScopeExtensionPlannedObsolescenceExtension.class, ScopeExtensionPlannedObsolescenceScope.class)
                    .addClasses(ScopeExtensionPlannedObsolescenceContext.class, ScopeExtensionResource.class)
                    .addClasses(ScopeExtensionObsolescent.class, ScopeExtensionObsolescentAfterTwoUses.class,
                            ScopeExtensionObsolescentAfterThreeUses.class)
                    .addAsServiceProvider(Extension.class, ScopeExtensionPlannedObsolescenceExtension.class);
            // Arquillian in the deployment
            return war;
        }
    });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ScopeExtensionTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Beans in scope test.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Obsolescent Scope")
    public void testObsolescentScope() throws Exception {
        client = ClientBuilder.newClient();
        log.info("starting testScope()");
        WebTarget base = client.target(generateURL("/extension/setup/"));
        Response response = base.request().post(Entity.text(new String()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        base = client.target(generateURL("/extension/test1/"));
        response = base.request().post(Entity.text(new String()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        base = client.target(generateURL("/extension/test2/"));
        response = base.request().post(Entity.text(new String()));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }
}
