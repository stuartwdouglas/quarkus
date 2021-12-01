package io.quarkus.hibernate.orm.panache.deployment.test;

import io.quarkus.hibernate.orm.panache.Aggregate;
import io.quarkus.hibernate.orm.panache.Criteria;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;

import javax.persistence.Entity;

import static io.quarkus.hibernate.orm.panache.Criteria.and;
import static io.quarkus.hibernate.orm.panache.Criteria.eq;
import static io.quarkus.hibernate.orm.panache.Criteria.in;
import static io.quarkus.hibernate.orm.panache.Criteria.join;

@Entity
public class Person extends PanacheEntity {
    public String firstName;
    public String lastName;
    int age;
    public Address address;

}

@Entity
class Address extends PanacheEntity {
    public String street;

}

class Foo {

    void doStuff() {
        PanacheQuery<Person> query = Person.findByCriteria(and(eq(s -> {
            s.firstName = "Loïc";
            s.lastName = "Mathieu";
        }), join(s -> s.address, eq(a -> a.street = "Fake St"))));
    }

    void inQuery() {
        PanacheQuery<Person> query = Person.findByCriteria(in(s -> s.firstName, "Loïc", "Stuart"));
    }

    void sumQuery() {
        PanacheQuery<Integer> query = Person.<Person, Integer>aggregateByCriteria(Aggregate.sum(s -> s.age),in(s -> s.firstName, "Loïc", "Stuart"));
    }
}
