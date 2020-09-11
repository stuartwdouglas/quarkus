package io.quarkus.rest.test.response;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.response.resource.HttpRequestParameterInjectorClassicParam;
import io.quarkus.rest.test.response.resource.HttpRequestParameterInjectorParamFactoryImpl;
import io.quarkus.rest.test.response.resource.HttpRequestParameterInjectorResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Localization
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for InjectorFactoryImpl. It is used for new type of parameters in resource.
 * @tpSince RESTEasy 3.0.16
 */
public class HttpRequestParameterInjectorTest {

    private static final String DEPLOYMENT_NAME = "app";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(HttpRequestParameterInjectorClassicParam.class);
                    return TestUtil.finishContainerPrepare(war, null, HttpRequestParameterInjectorResource.class,
                            HttpRequestParameterInjectorParamFactoryImpl.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, DEPLOYMENT_NAME);
    }

    /**
     * @tpTestDetails New Client usage.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testCustomInjectorFactory() throws Exception {
        Client client = ClientBuilder.newClient();

        String getResult = client.target(generateURL("/foo")).queryParam("param", "getValue").request()
                .accept("text/plain").get().readEntity(String.class);
        Assert.assertEquals("getValue, getValue, ", getResult);

        Form form = new Form().param("param", "postValue");
        String postResult = client.target(generateURL("/foo")).request()
                .accept("text/plain").post(Entity.form(form)).readEntity(String.class);
        Assert.assertEquals("postValue, , postValue", postResult);

        client.close();
    }

}
