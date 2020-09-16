package io.quarkus.rest.test.interceptor.gzip;

import java.net.URL;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @tpSubChapter Gzip
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1735
 * @tpSince RESTEasy 3.6
 */
@DisplayName("Allow Gzip On Server Not Allow Gzip On Client Test")
public class AllowGzipOnServerNotAllowGzipOnClientTest extends AllowGzipOnServerAbstractTestBase {

    /**
     * @tpTestDetails gzip is allowed on server by resteasy.allowGzip system property,
     *                gzip is allowed on client by manual import of gzip interceptors
     * @tpSince RESTEasy 3.6
     */
    @Test
    @OperateOnDeployment(WAR_WITHOUT_PROVIDERS_FILE)
    @DisplayName("Manually Import On Client")
    public void manuallyImportOnClient() throws Exception {
        testNormalClient(new URL(gzipServerBaseUrl + "/" + WAR_WITHOUT_PROVIDERS_FILE), true, "true", true, true);
    }

    /**
     * @tpTestDetails gzip is allowed on server by resteasy.allowGzip system property
     *                gzip is disabled on client
     * @tpSince RESTEasy 3.6
     */
    @Test
    @OperateOnDeployment(WAR_WITH_PROVIDERS_FILE)
    @DisplayName("No Gzip On Client")
    public void noGzipOnClient() throws Exception {
        testNormalClient(new URL(gzipServerBaseUrl + "/" + WAR_WITH_PROVIDERS_FILE), false, "true", false, false);
    }
}
