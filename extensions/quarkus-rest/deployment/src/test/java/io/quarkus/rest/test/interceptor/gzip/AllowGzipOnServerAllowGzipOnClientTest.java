package io.quarkus.rest.test.interceptor.gzip;

import java.net.URL;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @tpSubChapter Gzip
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1735
 * @tpSince RESTEasy 3.6
 */
@DisplayName("Allow Gzip On Server Allow Gzip On Client Test")
public class AllowGzipOnServerAllowGzipOnClientTest extends AllowGzipOnServerAbstractTestBase {

    @BeforeAll
    public static void init() {
        System.setProperty(PROPERTY_NAME, Boolean.TRUE.toString());
    }

    @AfterAll
    public static void clean() {
        System.clearProperty(PROPERTY_NAME);
    }

    /**
     * @tpTestDetails gzip is allowed on both server and client by resteasy.allowGzip system property
     * @tpSince RESTEasy 3.6
     */
    @Test
    @OperateOnDeployment(WAR_WITHOUT_PROVIDERS_FILE)
    @DisplayName("Allow Gzip On Server Allow Gzip On Client Test")
    public void allowGzipOnServerAllowGzipOnClientTest() throws Exception {
        testNormalClient(new URL(gzipServerBaseUrl + "/" + WAR_WITHOUT_PROVIDERS_FILE), false, "true", true, true);
    }
}
