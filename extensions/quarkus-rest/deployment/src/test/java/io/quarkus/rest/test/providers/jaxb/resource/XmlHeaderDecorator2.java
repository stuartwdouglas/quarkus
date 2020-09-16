package io.quarkus.rest.test.providers.jaxb.resource;

import java.lang.annotation.Annotation;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.DecorateTypes;
import org.jboss.resteasy.spi.DecoratorProcessor;

import io.quarkus.rest.test.Assert;

@DecorateTypes("application/xml")
public class XmlHeaderDecorator2 implements DecoratorProcessor<Assert, XmlHeaderJunk2Intf> {
    public Assert decorate(Assert target, XmlHeaderJunk2Intf annotation, Class type, Annotation[] annotations,
            MediaType mediaType) {
        throw new RuntimeException("FAILURE!!!!");
    }
}
