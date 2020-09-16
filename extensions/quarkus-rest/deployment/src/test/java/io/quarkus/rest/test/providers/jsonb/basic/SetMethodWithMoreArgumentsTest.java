package io.quarkus.rest.test.providers.jsonb.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.jsonb.basic.resource.Dog;
import io.quarkus.rest.test.providers.jsonb.basic.resource.SetMethodWithMoreArgumentsResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Json-binding provider
 * @tpChapter Integration test
 * @tpSince RESTEasy 3.6.2.Final
 */
@DisplayName("Set Method With More Arguments Test")
public class SetMethodWithMoreArgumentsTest {

    protected static final Logger LOG = Logger.getLogger(SetMethodWithMoreArgumentsTest.class.getName());

    static Client client;

    private static final String DEFAULT = "war_default";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(Dog.class);
            return TestUtil.finishContainerPrepare(war, null, SetMethodWithMoreArgumentsResource.class);
        }
    });

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Entity class Dog has setNameAndSort method with two arguments which causes
     *                'javax.json.bind.JsonbException: Invalid count of arguments for setter' with yasson-1.0.1.
     *                This test checks that this behavior is no longer present in newer versions of yasson.
     * @tpSince RESTEasy 3.6.2.Final
     */
    @Test
    @DisplayName("Test")
    public void test() {
        WebTarget target = client.target(PortProviderUtil.generateURL("/dog", DEFAULT));
        Entity<Dog> entity = Entity.entity(new Dog("Rex", "german shepherd"),
                MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8"));
        Dog dog = target.request().post(entity, Dog.class);
        LOG.info(dog);
        Assertions.assertTrue(dog.getName().equals("Jethro"));
        Assertions.assertTrue(dog.getSort().equals("stafford"));
    }
}
