package io.quarkus.test.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * meta-annotation that indicates that the annotation is used by a
 * {@link io.quarkus.test.junit.callback.QuarkusTestMethodParametersInterceptor} to
 * provide parameter values.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ParameterProvider {
}
