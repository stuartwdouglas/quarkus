package io.quarkus.rest.test.interceptor.resource;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import io.quarkus.rest.test.interceptor.PriorityExecutionTest;

@Priority(0)
public class PriorityExecutionClientResponseFilter2 implements ClientResponseFilter {
    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        PriorityExecutionTest.logger.info(this);
        PriorityExecutionTest.interceptors.add("PriorityExecutionClientResponseFilter2");
    }
}
