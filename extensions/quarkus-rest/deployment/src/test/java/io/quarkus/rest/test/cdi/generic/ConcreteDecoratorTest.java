package io.quarkus.rest.test.cdi.generic;

import static org.junit.Assert.assertEquals;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.cdi.generic.resource.Animal;
import io.quarkus.rest.test.cdi.generic.resource.Australopithecus;
import io.quarkus.rest.test.cdi.generic.resource.ConcreteDecorator;
import io.quarkus.rest.test.cdi.generic.resource.ConcreteResource;
import io.quarkus.rest.test.cdi.generic.resource.ConcreteResourceIntf;
import io.quarkus.rest.test.cdi.generic.resource.GenericsProducer;
import io.quarkus.rest.test.cdi.generic.resource.HierarchyHolder;
import io.quarkus.rest.test.cdi.generic.resource.HolderBinding;
import io.quarkus.rest.test.cdi.generic.resource.LowerBoundHierarchyHolder;
import io.quarkus.rest.test.cdi.generic.resource.NestedHierarchyHolder;
import io.quarkus.rest.test.cdi.generic.resource.ObjectHolder;
import io.quarkus.rest.test.cdi.generic.resource.Primate;
import io.quarkus.rest.test.cdi.generic.resource.UpperBoundHierarchyHolder;
import io.quarkus.rest.test.cdi.generic.resource.VisitList;
import io.quarkus.rest.test.cdi.util.UtilityProducer;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter CDI
 * @tpChapter Integration tests
 * @tpTestCaseDetails RESTEasy integration test for CDI && decorators
 * @tpSince RESTEasy 3.0.16
 */
public class ConcreteDecoratorTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(UtilityProducer.class, VisitList.class);
                    war.addClasses(ObjectHolder.class, ConcreteResourceIntf.class);
                    war.addClasses(HolderBinding.class, HierarchyHolder.class);
                    war.addClasses(GenericsProducer.class);
                    war.addClasses(ConcreteResource.class);
                    war.addClasses(NestedHierarchyHolder.class);
                    war.addClasses(UpperBoundHierarchyHolder.class, LowerBoundHierarchyHolder.class);
                    war.addClasses(Animal.class, Primate.class, Australopithecus.class);
                    war.addClasses(ConcreteDecorator.class);
                    war.addAsWebInfResource(ConcreteDecoratorTest.class.getPackage(), "concrete_beans.xml", "beans.xml");
                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, "resteasy-cdi-ejb-test");
    }

    /**
     * @tpTestDetails Run REST point method and check execution of decorators.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testConcreteConcreteDecorator() throws Exception {
        Client client = ClientBuilder.newClient();

        WebTarget base = client.target(generateURL("/concrete/decorators/clear"));
        Response response = base.request().get();
        assertEquals(Status.OK, response.getStatus());
        response.close();

        base = client.target(generateURL("/concrete/decorators/execute"));
        response = base.request().get();
        assertEquals(Status.OK, response.getStatus());
        response.close();

        base = client.target(generateURL("/concrete/decorators/test"));
        response = base.request().get();
        assertEquals(Status.OK, response.getStatus());
        response.close();

        client.close();
    }
}
