package io.quarkus.rest.test.asynch.resource;

@SuppressWarnings("serial")
public class AsyncInjectionException extends Exception
{

   public AsyncInjectionException(final String message)
   {
      super(message);
   }

}
