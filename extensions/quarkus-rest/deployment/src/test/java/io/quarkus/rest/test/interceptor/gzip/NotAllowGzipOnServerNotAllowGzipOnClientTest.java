package io.quarkus.rest.test.interceptor.gzip;

import java.net.URL;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @tpSubChapter Gzip
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1735
 * @tpSince RESTEasy 3.6
 */
@DisplayName("Not Allow Gzip On Server Not Allow Gzip On Client Test")
public class NotAllowGzipOnServerNotAllowGzipOnClientTest extends NotAllowGzipOnServerAbstractTestBase {

    @ArquillianResource
    private URL deploymentBaseUrl;

    /**
     * @tpTestDetails gzip is disabled on server
     *                gzip is disabled on client
     * @tpSince RESTEasy 3.6
     */
    @Test
    @OperateOnDeployment(WAR_WITHOUT_PROVIDERS_FILE)
    @DisplayName("No Providers File No Manual Import On Client")
    public void noProvidersFileNoManualImportOnClient() throws Exception {
        testNormalClient(deploymentBaseUrl, false, "null", false, false);
    }

    /**
     * @tpTestDetails gzip is enabled on server by javax.ws.rs.ext.Providers file in deployment
     *                gzip is allowed on client by manual import of gzip interceptors
     * @tpSince RESTEasy 3.6
     */
    @Test
    @OperateOnDeployment(WAR_WITH_PROVIDERS_FILE)
    @DisplayName("Providers File Manual Import On Client")
    public void providersFileManualImportOnClient() throws Exception {
        testNormalClient(deploymentBaseUrl, true, "null", true, true);
    }

    /**
     * @tpTestDetails gzip is enabled on server by javax.ws.rs.ext.Providers file in deployment
     *                gzip is disabled on client
     * @tpSince RESTEasy 3.6
     */
    @Test
    @OperateOnDeployment(WAR_WITH_PROVIDERS_FILE)
    @DisplayName("Providers File No Manual Import On Client")
    public void providersFileNoManualImportOnClient() throws Exception {
        testNormalClient(deploymentBaseUrl, false, "null", false, false);
    }

    /**
     * @tpTestDetails gzip is disabled on server
     *                gzip is allowed on client by manual import of gzip interceptors
     * @tpSince RESTEasy 3.6
     */
    @Test
    @OperateOnDeployment(WAR_WITHOUT_PROVIDERS_FILE)
    @DisplayName("No Providers File Manual Import On Client")
    public void noProvidersFileManualImportOnClient() throws Exception {
        testNormalClient(deploymentBaseUrl, true, "null", true, false);
    }
}
