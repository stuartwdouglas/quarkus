package io.quarkus.rest.test.validation.resource;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = ValidationXMLClassValidator.class)
@Target({ TYPE })
@Retention(RUNTIME)
public @interface ValidationXMLClassConstraint {
    String message() default "Concatenation of s and u must have length > {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int value();
}
