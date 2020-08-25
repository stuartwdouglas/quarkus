package io.quarkus.hibernate.orm.lazyloading;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LazyLoadingTestCase {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addAsResource("application.properties")
            .addClasses(Manager.class, Ceo.class, Worker.class, Setup.class, Supervisor.class));

    @Inject
    EntityManager em;

    @Test
    @Transactional
    public void testLazyLoading() {
        Ceo ceo = em.find(Ceo.class, 1L);
        Assertions.assertEquals("Jill", ceo.getName());

        Worker worker = em.find(Worker.class, 1L);
        Assertions.assertEquals(worker.getName(), "James");

        Assertions.assertEquals("Jill", worker.getSupervisor().getManagers().get(0).getCeo().getName());
    }

}
