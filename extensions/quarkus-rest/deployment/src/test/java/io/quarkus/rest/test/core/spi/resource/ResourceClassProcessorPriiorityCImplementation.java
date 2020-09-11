package io.quarkus.rest.test.core.spi.resource;

import javax.annotation.Priority;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceClassProcessor;

import io.quarkus.rest.test.core.spi.ResourceClassProcessorPriorityTest;

@Provider
@Priority(30)
public class ResourceClassProcessorPriiorityCImplementation implements ResourceClassProcessor {
    protected static final Logger logger = Logger.getLogger(ResourceClassProcessorPriiorityCImplementation.class.getName());

    @Override
    public ResourceClass process(ResourceClass clazz) {
        logger.info("ResourceClassProcessorPriiorityCImplementation visited on server");
        ResourceClassProcessorPriorityTest.addToVisitedProcessors("C");
        return clazz;
    }
}
