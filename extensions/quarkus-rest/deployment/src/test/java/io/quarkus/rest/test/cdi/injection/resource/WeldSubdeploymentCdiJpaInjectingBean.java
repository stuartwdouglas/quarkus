package io.quarkus.rest.test.cdi.injection.resource;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

public class WeldSubdeploymentCdiJpaInjectingBean {

   @Produces
   @PersistenceUnit(unitName = "cdiPu")
   EntityManagerFactory emf;

   public EntityManagerFactory entityManagerFactory() {
      return emf;
   }


}
