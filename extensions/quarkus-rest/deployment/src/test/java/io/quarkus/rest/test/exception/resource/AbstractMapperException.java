package io.quarkus.rest.test.exception.resource;

public class AbstractMapperException extends RuntimeException {
   public AbstractMapperException(final String message) {
      super(message);
   }
}
