package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.InheritanceAnimal;
import io.quarkus.rest.test.providers.jaxb.resource.InheritanceCat;
import io.quarkus.rest.test.providers.jaxb.resource.InheritanceDog;
import io.quarkus.rest.test.providers.jaxb.resource.InheritanceResource;
import io.quarkus.rest.test.providers.jaxb.resource.InheritanceZoo;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Inheritance Test")
public class InheritanceTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, InheritanceAnimal.class, InheritanceCat.class,
                    InheritanceDog.class, InheritanceZoo.class, InheritanceResource.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, InheritanceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests Jaxb object with inheritance structure
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Inheritance")
    public void testInheritance() throws Exception {
        QuarkusRestWebTarget target = client.target(generateURL("/zoo"));
        Response response = target.request().get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        InheritanceZoo zoo = response.readEntity(InheritanceZoo.class);
        Assertions.assertEquals(2, zoo.getAnimals().size(),
                "The number of animals in the zoo doesn't match the expected count");
    }
}
