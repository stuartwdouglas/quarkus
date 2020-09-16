package io.quarkus.rest.test.resource.param;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.resource.param.resource.ParamInterfaceResource;
import io.quarkus.rest.test.resource.param.resource.ParamResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-423 and RESTEASY-522
 * @tpSince RESTEasy 3.0.16
 */
public class ParamTest {

    @Deployment
    public static Archive<?> deploy() throws Exception {
        WebArchive war = TestUtil.prepareArchive(ParamTest.class.getSimpleName());
        war.addClass(ParamInterfaceResource.class);
        return TestUtil.finishContainerPrepare(war, null, ParamResource.class);
    }

    private String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(ParamTest.class.getSimpleName());
    }

    protected Client client;

    @BeforeEach
    public void beforeTest() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void afterTest() {
        client.close();
    }

    /**
     * @tpTestDetails Null matrix parameters should be accepted by the reasteasy client library (RESTEASY-423)
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNullMatrixParam() throws Exception {
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateBaseUrl());
        ParamInterfaceResource proxy = target.proxy(ParamInterfaceResource.class);
        String rtn = proxy.getMatrix(null);
        Assert.assertEquals("null", rtn);
    }

    /**
     * @tpTestDetails RestEasy Client Framework should not throw null point exception when
     *                the @CookieParam() is null (RESTEASY-522)
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNullCookieParam() throws Exception {
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateBaseUrl());
        ParamInterfaceResource proxy = target.proxy(ParamInterfaceResource.class);
        String rtn = proxy.getCookie(null);
        Assert.assertEquals("null", rtn);
    }

    /**
     * @tpTestDetails RestEasy Client Framework should not throw null point exception when
     *                the @HeaderParam() is null (RESTEASY-522)
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testNullHeaderParam() throws Exception {
        QuarkusRestWebTarget target = (QuarkusRestWebTarget) client.target(generateBaseUrl());
        ParamInterfaceResource proxy = target.proxy(ParamInterfaceResource.class);
        String rtn = proxy.getHeader(null);
        Assert.assertEquals("null", rtn);
    }

}
