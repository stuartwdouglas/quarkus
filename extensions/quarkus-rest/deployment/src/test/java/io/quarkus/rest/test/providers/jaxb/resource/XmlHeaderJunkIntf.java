package io.quarkus.rest.test.providers.jaxb.resource;

import javax.xml.bind.Marshaller;

import org.jboss.resteasy.annotations.Decorator;

/**
 * Test correct type (Marshaller), but incorrect media type
 */
@Decorator(processor = XmlHeaderDecorator.class, target = Marshaller.class)
public @interface XmlHeaderJunkIntf {
}
