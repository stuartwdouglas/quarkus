package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.xml.bind.JAXBContext;

import org.jboss.logging.Logger;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBContextFinder;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbCacheChild;
import io.quarkus.rest.test.providers.jaxb.resource.JaxbCacheParent;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Jaxb Cache Test")
public class JaxbCacheTest {

    static QuarkusRestClient client;

    private static Logger logger = Logger.getLogger(JaxbCacheTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(JaxbCacheTest.class);
            // Arquillian in the deployment
            return TestUtil.finishContainerPrepare(war, null, JaxbCacheParent.class, JaxbCacheChild.class);
        }
    });

    /**
     * @tpTestDetails Gets contextResolver for JAXBContextFinder class and mediatype "APPLICATION_XML_TYPE" or
     *                "APPLICATION_ATOM_XML_TYPE",
     *                then gets calls findCachedContext() twice to get JAXBContext and ensures that the result is the same
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Cache")
    public void testCache() throws Exception {
        ResteasyProviderFactory factory = ResteasyProviderFactory.getInstance();
        ResteasyContext.pushContext(Providers.class, factory);
        {
            ContextResolver<JAXBContextFinder> resolver = factory.getContextResolver(JAXBContextFinder.class,
                    MediaType.APPLICATION_XML_TYPE);
            JAXBContextFinder finder = resolver.getContext(JaxbCacheChild.class);
            JAXBContext ctx = finder.findCachedContext(JaxbCacheChild.class, MediaType.APPLICATION_XML_TYPE, null);
            JAXBContext ctx2 = finder.findCachedContext(JaxbCacheChild.class, MediaType.APPLICATION_XML_TYPE, null);
            Assertions.assertTrue(ctx == ctx2);
        }
        {
            ContextResolver<JAXBContextFinder> resolver = factory.getContextResolver(JAXBContextFinder.class,
                    MediaType.APPLICATION_ATOM_XML_TYPE);
            JAXBContextFinder finder = resolver.getContext(JaxbCacheChild.class);
            Assertions.assertNotNull(finder);
            JAXBContext ctx = finder.findCachedContext(JaxbCacheChild.class, MediaType.APPLICATION_ATOM_XML_TYPE, null);
            JAXBContext ctx2 = finder.findCachedContext(JaxbCacheChild.class, MediaType.APPLICATION_ATOM_XML_TYPE, null);
            Assertions.assertTrue(ctx == ctx2);
        }
    }

    /**
     * @tpTestDetails Gets contextResolver for JAXBContextFinder class and mediatype "APPLICATION_XML_TYPE" or
     *                "APPLICATION_ATOM_XML_TYPE",
     *                thrn gets calls findCacheContext() twice to get JAXBContext and ensures that the result is the same
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Cache 2")
    public void testCache2() throws Exception {
        ResteasyProviderFactory factory = ResteasyProviderFactory.getInstance();
        ResteasyContext.pushContext(Providers.class, factory);
        {
            ContextResolver<JAXBContextFinder> resolver = factory.getContextResolver(JAXBContextFinder.class,
                    MediaType.APPLICATION_XML_TYPE);
            JAXBContextFinder finder = resolver.getContext(JaxbCacheChild.class);
            JAXBContext ctx = finder.findCacheContext(MediaType.APPLICATION_XML_TYPE, null, JaxbCacheChild.class,
                    JaxbCacheParent.class);
            JAXBContext ctx2 = finder.findCacheContext(MediaType.APPLICATION_XML_TYPE, null, JaxbCacheChild.class,
                    JaxbCacheParent.class);
            Assertions.assertTrue(ctx == ctx2);
        }
        {
            ContextResolver<JAXBContextFinder> resolver = factory.getContextResolver(JAXBContextFinder.class,
                    MediaType.APPLICATION_ATOM_XML_TYPE);
            JAXBContextFinder finder = resolver.getContext(JaxbCacheChild.class);
            JAXBContext ctx = finder.findCacheContext(MediaType.APPLICATION_ATOM_XML_TYPE, null, JaxbCacheChild.class,
                    JaxbCacheParent.class);
            JAXBContext ctx2 = finder.findCacheContext(MediaType.APPLICATION_ATOM_XML_TYPE, null, JaxbCacheChild.class,
                    JaxbCacheParent.class);
            Assertions.assertTrue(ctx == ctx2);
        }
    }
}
