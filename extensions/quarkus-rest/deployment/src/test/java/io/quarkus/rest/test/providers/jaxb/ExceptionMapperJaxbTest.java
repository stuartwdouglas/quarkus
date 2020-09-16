package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.AbstractJaxbClassPerson;
import io.quarkus.rest.test.providers.jaxb.resource.ExceptionMapperJaxbMapper;
import io.quarkus.rest.test.providers.jaxb.resource.ExceptionMapperJaxbResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class ExceptionMapperJaxbTest {

    private static Logger logger = Logger.getLogger(ExceptionMapperJaxbTest.class.getName());
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, ExceptionMapperJaxbMapper.class,
                            ExceptionMapperJaxbResource.class,
                            AbstractJaxbClassPerson.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ExceptionMapperJaxbTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test for custom JAXBUnmarshalException excetion mapper
     * @tpInfo RESTEASY-519
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testFailure() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/test"));
        Response response = target.request().post(Entity.entity("<person", "application/xml"));
        Assert.assertEquals(400, response.getStatus());
        String output = response.readEntity(String.class);
        logger.info(output);
    }

}
