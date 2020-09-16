package io.quarkus.rest.test.form;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.form.resource.FormBodyResourceClient;
import io.quarkus.rest.test.form.resource.FormBodyResourceForm;
import io.quarkus.rest.test.form.resource.FormBodyResourceResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class FormBodyResourceTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(FormBodyResourceClient.class);
                    war.addClasses(FormBodyResourceForm.class);
                    return TestUtil.finishContainerPrepare(war, null, FormBodyResourceResource.class);
                }
            });

    /**
     * @tpTestDetails Check body of form.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void test() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        FormBodyResourceClient proxy = client.target(
                PortProviderUtil.generateBaseUrl(FormParameterTest.class.getSimpleName()))
                .proxyBuilder(FormBodyResourceClient.class).build();
        Assert.assertEquals("foo.gotIt", proxy.put("foo"));
        client.close();
    }
}
