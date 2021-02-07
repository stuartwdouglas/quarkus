package io.quarkus.hibernate.orm.lazyloading;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Toy {
    private long id;
    private String name;
    private Dog dog;

    public Toy() {
    }

    public Toy(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "defaultSeq")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDog(Dog dog) {
        this.dog = dog;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    public Dog getDog() {
        return dog;
    }
}
