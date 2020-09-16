package io.quarkus.rest.test.resource.path.resource;

import java.util.List;

import io.quarkus.rest.test.Assert;

public class ResourceLocatorWithBaseNoExpressionSubresource3 implements ResourceLocatorWithBaseExpressionSubresource3Interface {
    @Override
    public String get(List<Double> params) {
        Assert.assertNotNull(ResourceLocatorWithBaseNoExpressionResource.ERROR_MSG, params);
        Assert.assertEquals(ResourceLocatorWithBaseNoExpressionResource.ERROR_MSG, 2, params.size());
        params.get(0);
        params.get(1);
        return "ResourceLocatorWithBaseNoExpressionSubresource3";
    }
}
