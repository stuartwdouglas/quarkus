package io.quarkus.rest.test.client;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import io.quarkus.rest.test.client.resource.HeaderEmptyHostResource;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import io.quarkus.rest.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.rest.test.simple.TestUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.stream.Collectors;

/**
 * RESTEASY-2300 and UNDERTOW-1614
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class HeaderEmptyHostTest extends ClientTestBase {
    private static Logger logger = Logger.getLogger(HeaderEmptyHostTest.class);

     @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

        return TestUtil.finishContainerPrepare(war, null, HeaderEmptyHostResource.class);
    }});

    @ArquillianResource
    URL url;

    @Test
    public void testEmptyHost() throws Exception {
        try (Socket client = new Socket(url.getHost(), url.getPort())) {
            try (PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
                final String uri = "/HeaderEmptyHostTest/headeremptyhostresource";
                out.printf("GET %s HTTP/1.1\r\n", uri);
                out.print("Host: \r\n");
                out.print("Connection: close\r\n");
                out.print("\r\n");
                out.flush();
                String response = new BufferedReader(new InputStreamReader(client.getInputStream())).lines().collect(Collectors.joining("\n"));
                logger.info("response = " + response);
                Assert.assertNotNull(response);
                Assert.assertTrue(response.contains("HTTP/1.1 200 OK"));
                Assert.assertTrue(response.contains("uriInfo: http://" + url.getHost() + ":" + url.getPort() + uri));
            }
        }
    }

}
