package io.quarkus.rest.test.validation.resource;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ValidationComplexClassValidator.class)
@Target({TYPE})
@Retention(RUNTIME)
public @interface ValidationComplexClassConstraint {
   String message() default "Concatenation of s and t must have length > {value}";

   Class<?>[] groups() default {};

   Class<? extends Payload>[] payload() default {};

   int value();
}
