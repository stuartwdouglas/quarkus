package io.quarkus.rest.test.resource.path.resource;

public class LocatorSubResourceReturningThisParamEntityWithConstructor
        extends LocatorSubResourceReturningThisParamEntityPrototype {
    public LocatorSubResourceReturningThisParamEntityWithConstructor(final String arg) {
        value = arg;
    }
}
