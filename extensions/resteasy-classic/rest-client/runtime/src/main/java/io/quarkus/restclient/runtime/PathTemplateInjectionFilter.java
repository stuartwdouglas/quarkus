package io.quarkus.restclient.runtime;

import static org.jboss.resteasy.microprofile.client.utils.ClientRequestContextUtils.getDeclaringClass;
import static org.jboss.resteasy.microprofile.client.utils.ClientRequestContextUtils.getMethod;

import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

@Priority(Integer.MIN_VALUE)
public class PathTemplateInjectionFilter implements ClientRequestFilter {
    private final Map<String, String> pathTemplates;

    public PathTemplateInjectionFilter(Map<String, String> methodToPath) {
        pathTemplates = methodToPath;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.setProperty("UrlPathTemplate", pathTemplates.get(lookupName(requestContext)));
    }

    private String lookupName(ClientRequestContext requestContext) {
        return getDeclaringClass(requestContext).getName() + "." + getMethod(requestContext).getName();
    }
}
