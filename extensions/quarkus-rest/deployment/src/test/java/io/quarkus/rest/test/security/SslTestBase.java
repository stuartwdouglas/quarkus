package io.quarkus.rest.test.security;

import static org.jboss.resteasy.utils.PortProviderUtil.isIpv6;

import java.io.File;

import javax.ws.rs.client.Client;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.quarkus.rest.runtime.client.QuarkusRestClientBuilder;

/**
 * Base class for SSL tests. Contains utility methods used in all ssl tests.
 */
public abstract class SslTestBase {

    private static final Logger LOG = Logger.getLogger(SslTestBase.class);

    protected static Client client;
    protected static QuarkusRestClientBuilder QuarkusRestClientBuilder;

    protected static String RESOURCES = "src/test/resources/org/jboss/resteasy/test/security/";
    protected static final String PASSWORD = "123456";
    protected static final String DEPLOYMENT_NAME = "ssl-war";
    protected static final String HOSTNAME = "localhost";

    @ArquillianResource
    protected static ContainerController containerController;

    @ArquillianResource
    protected static Deployer deployer;

    /**
     * Generate a https URL
     *
     * @return a full https URL
     */
    protected static String generateHttpsURL(int offset, String hostname) {
        // ipv4
        if (!isIpv6()) {
            return String.format("https://%s:%d/%s%s", hostname, 8443 + offset, DEPLOYMENT_NAME, "/ssl/hello");
        }
        // ipv6
        return String.format("https://[%s]:%d/%s%s", hostname, 8443 + offset, DEPLOYMENT_NAME, "/ssl/hello");
    }

    /**
     * Generate a https URL, use localhost
     */
    protected static String generateHttpsURL(int offset) {
        return generateHttpsURL(offset, true);
    }

    /**
     * Generate a https URL, allow to use localhost or hostname from node property
     */
    protected static String generateHttpsURL(int offset, boolean forceLocalhost) {
        if (forceLocalhost) {
            return generateHttpsURL(offset, HOSTNAME);
        } else {
            return generateHttpsURL(offset, PortProviderUtil.getHost());
        }
    }

    /**
     * Set up ssl in jboss-cli so https endpoint can be accessed only if client trusts certificates in the server keystore.
     * 
     * @throws Exception
     */
    protected static void secureServer(String serverKeystorePath, int portOffset) throws Exception {
        File file = new File(serverKeystorePath);
        serverKeystorePath = file.getAbsolutePath();

        if (TestUtil.isWindows()) {
            serverKeystorePath = serverKeystorePath.replace("\\", "\\\\");
        }

        OnlineManagementClient client = TestUtil.clientInit(portOffset);

        TestUtil.runCmd(client,
                "/core-service=management/security-realm=ApplicationRealm/server-identity=ssl:remove(keystore-path, keystore-password, alias)");
        TestUtil.runCmd(client, String.format(
                "/core-service=management/security-realm=ApplicationRealm/server-identity=ssl:add(keystore-path=%s, keystore-password=%s)",
                serverKeystorePath, PASSWORD));

        // above changes request reload to take effect
        Administration admin = new Administration(client, 240);
        admin.reload();

        client.close();
    }

}
