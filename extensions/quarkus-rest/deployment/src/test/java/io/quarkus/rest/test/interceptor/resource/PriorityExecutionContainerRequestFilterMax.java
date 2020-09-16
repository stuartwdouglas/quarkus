package io.quarkus.rest.test.interceptor.resource;

import io.quarkus.rest.test.interceptor.PriorityExecutionTest;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

@Priority(Integer.MAX_VALUE)
public class PriorityExecutionContainerRequestFilterMax implements ContainerRequestFilter {
   @Override
   public void filter(ContainerRequestContext requestContext) throws IOException {
      PriorityExecutionTest.logger.info(this);
      PriorityExecutionTest.interceptors.add("PriorityExecutionContainerRequestFilterMax");
   }
}
