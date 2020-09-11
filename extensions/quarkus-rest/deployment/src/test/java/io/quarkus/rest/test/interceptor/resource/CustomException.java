package io.quarkus.rest.test.interceptor.resource;

public class CustomException extends RuntimeException {

   public CustomException() {
      super("This is a custom Exception");
   }
}
