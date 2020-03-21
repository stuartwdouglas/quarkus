package io.quarkus.deltaspike.partialbean.test;

import java.util.function.Supplier;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SimplePartialBeanTestCase {

    @RegisterExtension
    static QuarkusUnitTest extension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(TestInterface.class, TestBinding.class,
                            TestInvocationHandler.class);
                }
            });

    @Inject
    TestInterface testInterface;

    @Test
    public void testPartialBean() {
        Assertions.assertEquals("method1", testInterface.method1());
    }
}
