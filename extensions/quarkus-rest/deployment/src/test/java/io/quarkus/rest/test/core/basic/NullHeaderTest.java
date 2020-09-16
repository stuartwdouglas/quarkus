package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.basic.resource.NullHeaderFilter;
import io.quarkus.rest.test.core.basic.resource.NullHeaderResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter RESTEASY-1565
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.1.Final
 */
@DisplayName("Null Header Test")
public class NullHeaderTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(NullHeaderFilter.class);
            return TestUtil.finishContainerPrepare(war, null, NullHeaderResource.class);
        }
    });

    @Test
    @DisplayName("Test Null Header")
    public void testNullHeader() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(PortProviderUtil.generateURL("/test", NullHeaderTest.class.getSimpleName()));
        Response response = base.register(NullHeaderFilter.class).request().header("X-Auth-User", null).get();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatus());
        String serverHeader = response.getHeaderString("X-Server-Header");
        Assertions.assertTrue(serverHeader == null || "".equals(serverHeader));
        client.close();
    }
}
