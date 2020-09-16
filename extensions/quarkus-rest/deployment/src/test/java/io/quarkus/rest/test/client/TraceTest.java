package io.quarkus.rest.test.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.quarkus.rest.test.client.resource.TraceResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Client tests
 * @tpSince RESTEasy 3.0.16
 */
public class TraceTest extends ClientTestBase {

    private static ModelNode origDisallowedMethodsValue;
    private static Address address = Address.subsystem("undertow").and("server", "default-server").and("http-listener",
            "default");
    protected static final Logger logger = Logger.getLogger(TraceTest.class.getName());
    private static Client client;

    @HttpMethod("TRACE")
    @Target(value = ElementType.METHOD)
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface TRACE {
    }

    @BeforeClass
    public static void setMaxPostSize() throws Exception {
        OnlineManagementClient client = TestUtil.clientInit();
        Administration admin = new Administration(client);
        Operations ops = new Operations(client);

        // get original 'disallowed methods' value
        origDisallowedMethodsValue = ops.readAttribute(address, "disallowed-methods").value();
        // set 'disallowed methods' to empty list to allow TRACE
        ops.writeAttribute(address, "disallowed-methods", new ModelNode().setEmptyList());

        // reload server
        admin.reload();
        client.close();
    }

    @AfterClass
    public static void resetToDefault() throws Exception {
        OnlineManagementClient client = TestUtil.clientInit();
        Administration admin = new Administration(client);
        Operations ops = new Operations(client);

        // write original 'disallowed methods' value
        ops.writeAttribute(address, "disallowed-methods", origDisallowedMethodsValue);

        // reload server
        admin.reload();
        client.close();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(TraceTest.class);
                    return TestUtil.finishContainerPrepare(war, null, TraceResource.class);
                }
            });

    @Before
    public void init() {
        client = ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Client sends request for custom defined http method 'TRACE'
     * @tpPassCrit Successful response is returned
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void TraceTest() {
        Response response = client.target(generateURL("/resource/trace")).request().trace(Response.class);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
}
