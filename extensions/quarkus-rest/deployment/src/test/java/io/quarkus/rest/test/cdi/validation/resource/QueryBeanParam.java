package io.quarkus.rest.test.cdi.validation.resource;

import javax.validation.constraints.Size;

public interface QueryBeanParam {
    @Size(min = 2)
    String getParam();
}
