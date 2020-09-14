package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.HomecontrolApplication;
import io.quarkus.rest.test.providers.jaxb.resource.HomecontrolCustomJAXBContext;
import io.quarkus.rest.test.providers.jaxb.resource.HomecontrolJaxbProvider;
import io.quarkus.rest.test.providers.jaxb.resource.HomecontrolService;
import io.quarkus.rest.test.providers.jaxb.resource.homecontrol.Base64Binary;
import io.quarkus.rest.test.providers.jaxb.resource.homecontrol.BinaryType;
import io.quarkus.rest.test.providers.jaxb.resource.homecontrol.ErrorDomainType;
import io.quarkus.rest.test.providers.jaxb.resource.homecontrol.ErrorMessageType;
import io.quarkus.rest.test.providers.jaxb.resource.homecontrol.ErrorType;
import io.quarkus.rest.test.providers.jaxb.resource.homecontrol.IDType;
import io.quarkus.rest.test.providers.jaxb.resource.homecontrol.ObjectFactory;
import io.quarkus.rest.test.providers.jaxb.resource.homecontrol.RoleType;
import io.quarkus.rest.test.providers.jaxb.resource.homecontrol.UserType;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0.0
 */
public class HomecontrolCustomJAXBContextTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(HomecontrolCustomJAXBContext.class,
                            HomecontrolApplication.class,
                            HomecontrolService.class,
                            HomecontrolJaxbProvider.class,
                            ObjectFactory.class,
                            ErrorDomainType.class,
                            BinaryType.class,
                            ErrorType.class,
                            Base64Binary.class,
                            RoleType.class,
                            UserType.class,
                            IDType.class,
                            ErrorMessageType.class);

                    war.addAsWebInfResource(HomecontrolCustomJAXBContextTest.class.getPackage(), "homecontrol/web.xml");
                    return TestUtil.finishContainerPrepare(war, null, HomecontrolCustomJAXBContextTest.class);
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
        return PortProviderUtil.generateURL(path, HomecontrolCustomJAXBContextTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test that a user provided JAXBContext implementation is use.
     * @tpInfo RESTEASY-1754
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testMarshallering() throws Exception {

        String xmlStr = "<user xmlns=\"http://creaity.de/homecontrol/rest/types/v1\"> <id>id</id>"
                + " <credentials> <loginId>test</loginId> </credentials>"
                + " <roles><role>USER</role></roles></user>";

        QuarkusRestWebTarget target = client.target(generateURL("/service/users"));
        Response response = target.request().accept("application/xml").post(Entity.xml(xmlStr));
        UserType entity = response.readEntity(UserType.class);
        Assert.assertNotNull(entity);
        Assert.assertTrue("id DemoService_visited".equals(entity.getId()));
        response.close();
    }
}
