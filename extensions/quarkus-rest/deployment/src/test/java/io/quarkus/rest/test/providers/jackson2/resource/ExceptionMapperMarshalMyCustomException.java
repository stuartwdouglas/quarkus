package io.quarkus.rest.test.providers.jackson2.resource;

public class ExceptionMapperMarshalMyCustomException extends RuntimeException {
   public ExceptionMapperMarshalMyCustomException(final String message) {
      super(message);
   }
}
