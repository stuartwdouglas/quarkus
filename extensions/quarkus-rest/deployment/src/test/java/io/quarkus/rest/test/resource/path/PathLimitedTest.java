package io.quarkus.rest.test.resource.path;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.resource.path.resource.PathLimitedBasicResource;
import io.quarkus.rest.test.resource.path.resource.PathLimitedLocatorResource;
import io.quarkus.rest.test.resource.path.resource.PathLimitedLocatorUriResource;
import io.quarkus.rest.test.resource.path.resource.PathLimitedUnlimitedOnPathResource;
import io.quarkus.rest.test.resource.path.resource.PathLimitedUnlimitedResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for limited and unlimited path
 * @tpSince RESTEasy 3.0.16
 */
public class PathLimitedTest {

    static Client client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, PathLimitedUnlimitedOnPathResource.class,
                            PathLimitedUnlimitedResource.class,
                            PathLimitedLocatorResource.class, PathLimitedLocatorUriResource.class,
                            PathLimitedBasicResource.class);
                }
            });

    @BeforeClass
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    private void basicTest(String path) {
        Response response = client.target(PortProviderUtil.generateURL(path, PathLimitedTest.class.getSimpleName())).request()
                .get();
        Assert.assertEquals(Status.OK, response.getStatus());
        response.close();
    }

    /**
     * @tpTestDetails Check unlimited behaviour on class
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testUnlimitedOnClass() {
        basicTest("/unlimited");
        basicTest("/unlimited/on/and/on");
    }

    /**
     * @tpTestDetails Check unlimited behaviour on method
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testUnlimitedOnMethod() {
        basicTest("/unlimited2/on/and/on");
        basicTest("/unlimited2/runtime/org.jbpm:HR:1.0/process/hiring/start");
        basicTest("/uriparam/on/and/on?expected=on%2Fand%2Fon");
    }

    /**
     * @tpTestDetails Check location of resources
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testLocator() {
        basicTest("/locator");
        basicTest("/locator/on/and/on");
        basicTest("/locator2/on/and/on?expected=on%2Fand%2Fon");
        basicTest("/locator3/unlimited/unlimited2/on/and/on");
        basicTest("/locator3/unlimited/uriparam/on/and/on?expected=on%2Fand%2Fon");
        basicTest("/locator3/uriparam/1/uriparam/on/and/on?firstExpected=1&expected=on%2Fand%2Fon");

    }

}
