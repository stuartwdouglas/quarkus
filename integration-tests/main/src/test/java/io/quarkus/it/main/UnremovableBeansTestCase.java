package io.quarkus.it.main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.it.arc.ProducerOfUnusedUnremovableBean;
import io.quarkus.it.arc.UnusedRemovableBean;
import io.quarkus.it.arc.UnusedUnremovableBean;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class UnremovableBeansTestCase {

    @Test
    public void unusedBeanMarkedAsUnremoveableShouldBeAccessible() {
        Assertions.assertNotNull(Arc.container().instance(UnusedUnremovableBean.class).get());
    }

    @Test
    public void unusedBeanWhoseProducerMarkedAsUnemoveableShouldBeAccessible() {
        Assertions.assertNotNull(Arc.container().instance(ProducerOfUnusedUnremovableBean.Bean.class).get());
    }

    @Test
    public void unusedBeanNotMarkedAsUnremoveableShouldBeAccessible() {
        Assertions.assertNull(Arc.container().instance(UnusedRemovableBean.class).get());
    }
}
