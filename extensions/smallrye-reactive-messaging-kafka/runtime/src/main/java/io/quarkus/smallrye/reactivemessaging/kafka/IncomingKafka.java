package io.quarkus.smallrye.reactivemessaging.kafka;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.kafka.common.serialization.Deserializer;

@Retention(RetentionPolicy.RUNTIME)
public @interface IncomingKafka {

    String topic() default "";

    Class<? extends Deserializer> deserializer() default Deserializer.class;

    AutoOffset autoOffsetReset() default AutoOffset.earliest;

    enum AutoOffset {
        latest,
        earliest,
        none;
    }
}
