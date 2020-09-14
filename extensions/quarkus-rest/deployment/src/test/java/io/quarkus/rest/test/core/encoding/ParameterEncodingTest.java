package io.quarkus.rest.test.core.encoding;

import static junit.framework.TestCase.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.core.encoding.resource.ParameterEncodingResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Encoding
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-737
 * @tpSince RESTEasy 3.0.16
 */
public class ParameterEncodingTest {

    protected QuarkusRestClient client;

    @Before
    public void setup() throws Exception {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ParameterEncodingTest.class.getSimpleName());
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, ParameterEncodingResource.class);
                }
            });

    @After
    public void shutdown() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Check space encoding in URL
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testResteasy734() throws Exception {
        QuarkusRestWebTarget target = null;
        Response response = null;

        target = client.target(generateURL("/encoded/pathparam/bee bop"));
        response = target.request().get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee%20bop", entity);
        response.close();

        target = client.target(generateURL("/decoded/pathparam/bee bop"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee bop", entity);
        response.close();

        target = client.target(generateURL("/encoded/matrix;m=bee bop"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee%20bop", entity);
        response.close();

        target = client.target(generateURL("/decoded/matrix;m=bee bop"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee bop", entity);
        response.close();

        target = client.target(generateURL("/encoded/query?m=bee bop"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee%20bop", entity);
        response.close();

        target = client.target(generateURL("/decoded/query?m=bee bop"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee bop", entity);
        response.close();

        target = client.target(generateURL("/encoded/form"));
        Form form = new Form();
        form.param("f", "bee bop");
        response = target.request().post(Entity.form(form));
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee+bop", entity);
        response.close();

        target = client.target(generateURL("/decoded/form"));
        form = new Form();
        form.param("f", "bee bop");
        response = target.request().post(Entity.form(form));
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee bop", entity);
        response.close();

        target = client.target(generateURL("/encoded/segment/bee bop"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee%20bop", entity);
        response.close();

        target = client.target(generateURL("/decoded/segment/bee bop"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee bop", entity);
        response.close();

        target = client.target(generateURL("/encoded/segment/matrix/params;m=bee bop"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee%20bop", entity);
        response.close();

        target = client.target(generateURL("/decoded/segment/matrix/params;m=bee bop"));
        response = target.request().get();
        entity = response.readEntity(String.class);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("bee bop", entity);
        response.close();
    }
}
