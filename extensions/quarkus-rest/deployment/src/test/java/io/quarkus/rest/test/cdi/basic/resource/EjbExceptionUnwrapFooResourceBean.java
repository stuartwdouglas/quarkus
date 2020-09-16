package io.quarkus.rest.test.cdi.basic.resource;

import javax.ejb.Stateless;

@Stateless
public class EjbExceptionUnwrapFooResourceBean implements EjbExceptionUnwrapFooResource {
    public void testException() {
        throw new EjbExceptionUnwrapFooException();
    }
}
