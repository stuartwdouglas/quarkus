package io.quarkus.rest.test.providers.jaxb;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.spi.util.Types;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceAbstractBackendCollectionResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceAbstractBackendResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceAbstractBackendSubResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceAction;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceAssignedPermissionsResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceBackendDataCenterResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceBackendDataCentersResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceBackendResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceBaseBackendResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceBaseResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceBaseResources;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceBusinessEntity;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceDataCenter;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceDataCenterResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceDataCenters;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceDataCentersResource;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceGuid;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceINotifyPropertyChanged;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceIVdcQueryable;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceStoragePool;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceTop;
import io.quarkus.rest.test.providers.jaxb.resource.GenericSuperInterfaceUpdatableResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-636
 * @tpSince RESTEasy 3.0.16
 */
@RunWith(Arquillian.class)
public class GenericSuperInterfaceTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(GenericSuperInterfaceBackendDataCentersResource.class,
                            GenericSuperInterfaceAbstractBackendCollectionResource.class,
                            GenericSuperInterfaceAbstractBackendResource.class,
                            GenericSuperInterfaceAbstractBackendSubResource.class,
                            GenericSuperInterfaceAction.class, GenericSuperInterfaceAssignedPermissionsResource.class,
                            GenericSuperInterfaceBackendDataCenterResource.class,
                            GenericSuperInterfaceBackendDataCentersResource.class,
                            GenericSuperInterfaceBackendResource.class,
                            GenericSuperInterfaceBaseResource.class, GenericSuperInterfaceBaseResources.class,
                            GenericSuperInterfaceBusinessEntity.class,
                            GenericSuperInterfaceDataCenter.class, GenericSuperInterfaceDataCenterResource.class,
                            GenericSuperInterfaceDataCenters.class,
                            GenericSuperInterfaceDataCentersResource.class, GenericSuperInterfaceGuid.class,
                            GenericSuperInterfaceINotifyPropertyChanged.class, GenericSuperInterfaceIVdcQueryable.class,
                            GenericSuperInterfaceStoragePool.class, GenericSuperInterfaceUpdatableResource.class,
                            GenericSuperInterfaceBaseBackendResource.class,
                            TestUtil.class);
                    // Arquillian in the deployment

                    return TestUtil.finishContainerPrepare(war, null, GenericSuperInterfaceTop.class);
                }
            });

    /**
     * @tpTestDetails Test on server.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testGetImplementationReflection() throws Exception {
        Class updatableResource = GenericSuperInterfaceBackendDataCenterResource.class.getInterfaces()[0].getInterfaces()[0];
        Assert.assertEquals(updatableResource, GenericSuperInterfaceUpdatableResource.class);
        Method update = null;
        for (Method method : updatableResource.getMethods()) {
            if (method.getName().equals("update")) {
                update = method;
            }
        }
        Assert.assertNotNull("Updated method was not found", update);

        Method implemented = Types.getImplementingMethod(GenericSuperInterfaceBackendDataCenterResource.class, update);

        Method actual = null;
        for (Method method : GenericSuperInterfaceBackendDataCenterResource.class.getMethods()) {
            if (method.getName().equals("update") && !method.isSynthetic()) {
                actual = method;
            }
        }
        Assert.assertEquals("Interface was not detected", implemented, actual);
    }
}
