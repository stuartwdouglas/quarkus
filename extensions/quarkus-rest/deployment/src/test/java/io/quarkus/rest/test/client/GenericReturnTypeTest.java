package io.quarkus.rest.test.client;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.client.resource.GenericReturnTypeInterface;
import io.quarkus.rest.test.client.resource.GenericReturnTypeReader;
import io.quarkus.rest.test.client.resource.GenericReturnTypeResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.17
 * @tpTestCaseDetails Regression for JBEAP-4699
 */
public class GenericReturnTypeTest extends ClientTestBase {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(GenericReturnTypeInterface.class);
                    return TestUtil.finishContainerPrepare(war, null, GenericReturnTypeResource.class,
                            GenericReturnTypeReader.class);
                }
            });

    /**
     * @tpTestDetails Test generic type of proxy
     * @tpSince RESTEasy 3.0.17
     */
    @Test
    public void testGenericReturnType() {
        Client client = QuarkusRestClientBuilder.newClient();
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateURL(""))
                .register(GenericReturnTypeReader.class);
        GenericReturnTypeInterface<?> server = ProxyBuilder.builder(GenericReturnTypeInterface.class, target).build();
        Object result = server.t();
        Assert.assertEquals("abc", result);
        client.close();
    }
}
