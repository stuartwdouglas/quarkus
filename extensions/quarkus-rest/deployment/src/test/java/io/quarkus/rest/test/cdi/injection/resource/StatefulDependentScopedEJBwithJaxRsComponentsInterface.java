package io.quarkus.rest.test.cdi.injection.resource;

import javax.ejb.Local;

@Local
public interface StatefulDependentScopedEJBwithJaxRsComponentsInterface extends ReverseInjectionEJBInterface {
}
