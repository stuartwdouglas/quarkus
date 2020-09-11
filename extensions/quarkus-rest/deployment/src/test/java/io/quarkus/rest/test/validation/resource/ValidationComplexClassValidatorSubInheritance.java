package io.quarkus.rest.test.validation.resource;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.jboss.logging.Logger;

public class ValidationComplexClassValidatorSubInheritance
        implements ConstraintValidator<ValidationComplexClassInheritanceSubConstraint, ValidationComplexInterfaceSub> {
    private static Logger logger = Logger.getLogger(ValidationComplexClassValidatorSubInheritance.class);
    String pattern;

    public void initialize(ValidationComplexClassInheritanceSubConstraint constraintAnnotation) {
        pattern = constraintAnnotation.value();
    }

    public boolean isValid(ValidationComplexInterfaceSub value, ConstraintValidatorContext context) {
        logger.info(this + "u: " + value.u);
        logger.info("pattern: " + pattern + ", matches: " + value.u.matches(pattern));
        return value.u.matches(pattern);
    }
}
