package io.quarkus.rest.test.core.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.core.basic.resource.AcceptLanguagesResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Localization
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class AcceptLanguagesTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, AcceptLanguagesResource.class);
                }
            });

    /**
     * @tpTestDetails Check some languages for accepting
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testLanguages() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(PortProviderUtil.generateURL("/lang", AcceptLanguagesTest.class.getSimpleName()));
        Response response = base.request().header("Accept-Language", "en-US;q=0,en;q=0.8,de-AT,de;q=0.9").get();

        Assert.assertEquals(response.getStatus(), Status.OK.getStatusCode());

        response.close();
        client.close();
    }

}
