package io.quarkus.rest.test.core.servlet;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;

import javax.ws.rs.core.Response.Status;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Configuration
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-903
 * @tpSince RESTEasy 3.0.16
 */

public class UndertowTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    // Arquillian in the deployment and use of PortProviderUtil

                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, "RESTEASY-903");
    }

    /**
     * @tpTestDetails Redirection in one servlet to other servlet.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testUndertow() throws Exception {
        URL url = new URL(generateURL("/test"));
        HttpURLConnection conn = HttpURLConnection.class.cast(url.openConnection());
        conn.connect();
        byte[] b = new byte[16];
        conn.getInputStream().read(b);
        Assert.assertThat("Wrong content of response", new String(b), CoreMatchers.startsWith("forward"));
        Assert.assertEquals(Status.OK, conn.getResponseCode());
        conn.disconnect();
    }
}
