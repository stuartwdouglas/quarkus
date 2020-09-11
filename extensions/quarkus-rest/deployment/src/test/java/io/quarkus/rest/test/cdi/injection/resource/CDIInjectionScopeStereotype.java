package io.quarkus.rest.test.cdi.injection.resource;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Stereotype;

@ApplicationScoped
@Stereotype
@Target(TYPE)
@Retention(RUNTIME)
public @interface CDIInjectionScopeStereotype {
}
