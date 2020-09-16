package io.quarkus.rest.test.validation.resource;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

public class ValidationComplexListOfStrings {
    @Valid
    List<ValidationComplexOneString> strings;

    public ValidationComplexListOfStrings(final String s) {
        strings = new ArrayList<ValidationComplexOneString>();
        strings.add(new ValidationComplexOneString(s));
    }
}
