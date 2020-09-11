package io.quarkus.rest.test.core.basic.resource;

import javax.ws.rs.Path;

public interface AnnotationInheritanceSomeOtherInterface {
   @Path("superint")
   AnnotationInheritanceSuperInt getSuperInt();

   @Path("failure")
   AnnotationInheritanceNotAResource getFailure();
}
