package io.quarkus.deltaspike.data.runtime;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

import org.apache.deltaspike.jpa.spi.entitymanager.ActiveEntityManagerHolder;

@Singleton
public class QuarkusActiveEntityManagerHolder implements ActiveEntityManagerHolder {

    @Inject
    EntityManager entityManager;

    @Override
    public void set(EntityManager entityManager) {

    }

    @Override
    public boolean isSet() {
        return true;
    }

    @Override
    public EntityManager get() {
        return entityManager;
    }

    @Override
    public void dispose() {

    }
}
