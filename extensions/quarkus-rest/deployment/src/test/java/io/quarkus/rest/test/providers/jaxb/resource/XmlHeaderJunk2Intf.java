package io.quarkus.rest.test.providers.jaxb.resource;

import org.jboss.resteasy.annotations.Decorator;

import io.quarkus.rest.test.Assert;

/**
 * Test correct media type, but incorrect type
 */
@Decorator(processor = XmlHeaderDecorator.class, target = Assert.class)
public @interface XmlHeaderJunk2Intf {
}
