package io.quarkus.hibernate.orm.panache.deployment.test;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.Criteria;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;

import java.util.function.Consumer;

import static io.quarkus.hibernate.orm.panache.Criteria.and;
import static io.quarkus.hibernate.orm.panache.Criteria.eq;
import static io.quarkus.hibernate.orm.panache.Criteria.gt;

@Entity
public class Person extends PanacheEntity {
    public String name;
    public int age;


}
class  Foo {

    void doStuff() {
        PanacheQuery<Person> query = Person.findByCriteria(and(gt((s -> s.age = 18)), eq((s -> s.name = "Stuart"))));
    }

}
