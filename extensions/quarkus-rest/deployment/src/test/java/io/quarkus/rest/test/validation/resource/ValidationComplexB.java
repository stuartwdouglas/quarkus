package io.quarkus.rest.test.validation.resource;

import javax.validation.Valid;

public class ValidationComplexB {
    @Valid
    ValidationComplexA a;

    public ValidationComplexB(final ValidationComplexA a) {
        this.a = a;
    }
}
