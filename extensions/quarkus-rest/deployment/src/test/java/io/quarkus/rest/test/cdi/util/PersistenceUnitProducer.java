package io.quarkus.rest.test.cdi.util;

import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionResourceBinding;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class PersistenceUnitProducer {
   @Produces
   @CDIInjectionResourceBinding
   @PersistenceContext(unitName = "test")
   EntityManager persistenceContext;
}
