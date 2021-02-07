package io.quarkus.hibernate.orm.lazyloading;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;

@Transactional
@ApplicationScoped
public class LazyLoadingBean {

    @Inject
    EntityManager entityManager;

    public void setup() {
        Dog fido = new Dog();
        fido.setName("Fido");

        Person stuart = new Person();
        stuart.setName("Stuart");
        stuart.setDog(fido);

        entityManager.persist(fido);
        entityManager.persist(stuart);

    }

    public void doLoad() {
        entityManager.getReference(Dog.class, 1L);
    }

    public void verify() {
        Dog fido = entityManager.getReference(Dog.class, 1L);
        Assertions.assertEquals("Fido", fido.getName());
    }
}
