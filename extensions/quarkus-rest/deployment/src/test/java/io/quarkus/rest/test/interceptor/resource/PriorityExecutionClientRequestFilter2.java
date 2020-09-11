package io.quarkus.rest.test.interceptor.resource;

import io.quarkus.rest.test.interceptor.PriorityExecutionTest;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

@Priority(0)
public class PriorityExecutionClientRequestFilter2 implements ClientRequestFilter {
   @Override
   public void filter(ClientRequestContext requestContext) throws IOException {
      PriorityExecutionTest.logger.info(this);
      PriorityExecutionTest.interceptors.add("PriorityExecutionClientRequestFilter2");
   }
}
