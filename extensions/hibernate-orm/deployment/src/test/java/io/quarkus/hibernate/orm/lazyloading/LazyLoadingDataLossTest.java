package io.quarkus.hibernate.orm.lazyloading;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LazyLoadingDataLossTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Dog.class, Person.class, LazyLoadingBean.class, Toy.class)
                    .addAsResource("application.properties"));

    @Inject
    LazyLoadingBean bean;

    @Test
    public void test() {
        bean.setup();
        bean.doLoad();
        bean.verify();
    }

}
