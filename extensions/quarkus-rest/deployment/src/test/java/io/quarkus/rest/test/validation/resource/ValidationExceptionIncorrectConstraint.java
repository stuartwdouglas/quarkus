package io.quarkus.rest.test.validation.resource;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;

@Documented
@Constraint(validatedBy = ValidationExceptionClassValidator.class)
@Target({ TYPE })
@Retention(RUNTIME)
public @interface ValidationExceptionIncorrectConstraint {
}
