package io.quarkus.test.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.junit.ParameterProvider;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ParameterProvider
public @interface WebTest {

    Method method() default Method.GET;

    String value() default "";

    int status() default 200;

    String body() default "";

    enum Method {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH,
        OPTIONS,
        HEAD
    }
}
