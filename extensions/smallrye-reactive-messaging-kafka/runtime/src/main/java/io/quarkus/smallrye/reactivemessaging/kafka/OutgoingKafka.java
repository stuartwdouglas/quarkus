package io.quarkus.smallrye.reactivemessaging.kafka;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.kafka.common.serialization.Serializer;

@Retention(RetentionPolicy.RUNTIME)
public @interface OutgoingKafka {

    String topic() default "";

    Class<? extends Serializer> serializer() default Serializer.class;

}
