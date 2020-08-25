package io.quarkus.hibernate.orm.lazyloading;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import io.quarkus.runtime.Startup;

@ApplicationScoped
@Startup
@Transactional
public class Setup {

    @Inject
    EntityManager entityManager;

    @PostConstruct
    void setup() {

        Ceo ceo = new Ceo();
        ceo.setName("Jill");
        entityManager.persist(ceo);

        Manager m1 = new Manager();
        m1.setName("Jane");
        m1.setCeo(ceo);
        entityManager.persist(m1);

        Manager m2 = new Manager();
        m2.setName("Jannet");
        m2.setCeo(ceo);
        entityManager.persist(m2);

        Supervisor s1 = new Supervisor();
        s1.setName("Bob");
        s1.getManagers().add(m1);
        s1.getManagers().add(m2);
        s1.setCeo(ceo);
        entityManager.persist(s1);

        Worker worker = new Worker();
        worker.setName("James");
        worker.setSupervisor(s1);
        entityManager.persist(worker);
    }
}
