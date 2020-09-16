package io.quarkus.rest.test.providers.file;

import java.io.File;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.file.resource.TempFileDeletionResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter File provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.1.3.Final
 */
@DisplayName("Temp File Deletion Test")
public class TempFileDeletionTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, TempFileDeletionResource.class);
        }
    });

    /**
     * @tpTestDetails Resource method contains parameter of the type File. This triggers File provider, which creates
     *                temporary file on the server side, which is automatically deleted in the end of the resource method
     *                invocation.
     * @tpInfo Regression test for RESTEASY-1464
     * @tpSince RESTEasy 3.1.3.Final
     */
    @Test
    @DisplayName("Test Delete On Server")
    public void testDeleteOnServer() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(PortProviderUtil.generateURL("/test/post", TempFileDeletionTest.class.getSimpleName()));
        Response response = base.request().post(Entity.entity("hello", "text/plain"));
        Assertions.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        String path = response.readEntity(String.class);
        File file = new File(path);
        Assertions.assertFalse(file.exists());
        client.close();
    }
}
