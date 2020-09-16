package io.quarkus.rest.test.client;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.container.DynamicFeature;

import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.Assert;
import io.quarkus.rest.test.client.resource.ClientDynamicFeaturesClientFeature1;
import io.quarkus.rest.test.client.resource.ClientDynamicFeaturesClientFeature2;
import io.quarkus.rest.test.client.resource.ClientDynamicFeaturesDualFeature1;
import io.quarkus.rest.test.client.resource.ClientDynamicFeaturesDualFeature2;
import io.quarkus.rest.test.client.resource.ClientDynamicFeaturesServerFeature1;
import io.quarkus.rest.test.client.resource.ClientDynamicFeaturesServerFeature2;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1083
 * @tpSince RESTEasy 3.0.16
 */

public class ClientDynamicFeaturesTest {
    private static final String CLIENT_FEATURE_ERROR_MSG = "Wrong count of client features";
    private static final String SERVER_FEATURE_ERROR_MSG = "Wrong count of server features";

    /**
     * Test needs to be run on deployment.
     * 
     * @return
     */
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(ClientDynamicFeaturesClientFeature1.class,
                            ClientDynamicFeaturesClientFeature2.class,
                            ClientDynamicFeaturesDualFeature2.class,
                            ClientDynamicFeaturesDualFeature1.class,
                            ClientDynamicFeaturesServerFeature2.class,
                            ClientDynamicFeaturesServerFeature1.class);
                    // Arquillian in the deployment

                    return TestUtil.finishContainerPrepare(war, null, (Class<?>[]) null);
                }
            });

    /**
     * @tpTestDetails Check dynamic feature counts.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testDynamicFeatures() throws Exception {
        ResteasyProviderFactory factory = ResteasyProviderFactory.newInstance();
        RegisterBuiltin.register(factory);

        factory.registerProvider(ClientDynamicFeaturesClientFeature1.class, 0, false, null);
        factory.registerProvider(ClientDynamicFeaturesServerFeature1.class, 0, false, null);
        factory.registerProvider(ClientDynamicFeaturesDualFeature1.class, 0, false, null);
        ClientDynamicFeaturesClientFeature2 clientFeature = new ClientDynamicFeaturesClientFeature2();
        ClientDynamicFeaturesServerFeature2 serverFeature = new ClientDynamicFeaturesServerFeature2();
        ClientDynamicFeaturesDualFeature2 feature = new ClientDynamicFeaturesDualFeature2();
        factory.registerProviderInstance(clientFeature, null, 0, false);
        factory.registerProviderInstance(serverFeature, null, 0, false);
        factory.registerProviderInstance(feature, null, 0, false);
        Set<DynamicFeature> clientFeatureSet = factory.getClientDynamicFeatures();
        Set<DynamicFeature> serverFeatureSet = factory.getServerDynamicFeatures();

        Assert.assertEquals(CLIENT_FEATURE_ERROR_MSG, 1,
                countFeatures(clientFeatureSet, "ClientDynamicFeaturesClientFeature1"));
        Assert.assertEquals(CLIENT_FEATURE_ERROR_MSG, 1,
                countFeatures(clientFeatureSet, "ClientDynamicFeaturesClientFeature2"));
        Assert.assertEquals(CLIENT_FEATURE_ERROR_MSG, 1, countFeatures(clientFeatureSet, "ClientDynamicFeaturesDualFeature1"));
        Assert.assertEquals(CLIENT_FEATURE_ERROR_MSG, 1, countFeatures(clientFeatureSet, "ClientDynamicFeaturesDualFeature2"));
        Assert.assertEquals(SERVER_FEATURE_ERROR_MSG, 1,
                countFeatures(serverFeatureSet, "ClientDynamicFeaturesServerFeature1"));
        Assert.assertEquals(SERVER_FEATURE_ERROR_MSG, 1,
                countFeatures(serverFeatureSet, "ClientDynamicFeaturesServerFeature2"));
        Assert.assertEquals(SERVER_FEATURE_ERROR_MSG, 1, countFeatures(serverFeatureSet, "ClientDynamicFeaturesDualFeature1"));
        Assert.assertEquals(SERVER_FEATURE_ERROR_MSG, 1, countFeatures(serverFeatureSet, "ClientDynamicFeaturesDualFeature2"));
    }

    private int countFeatures(Set<DynamicFeature> featureSet, String feature) {
        int count = 0;
        for (Iterator<DynamicFeature> it = featureSet.iterator(); it.hasNext();) {
            Class<?> clazz = it.next().getClass();
            if (clazz.getName().contains(feature)) {
                count++;
            }
        }
        return count;
    }
}
