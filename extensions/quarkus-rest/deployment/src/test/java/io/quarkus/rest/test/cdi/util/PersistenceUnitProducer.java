package io.quarkus.rest.test.cdi.util;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import io.quarkus.rest.test.cdi.injection.resource.CDIInjectionResourceBinding;

public class PersistenceUnitProducer {
    @Produces
    @CDIInjectionResourceBinding
    @PersistenceContext(unitName = "test")
    EntityManager persistenceContext;
}
