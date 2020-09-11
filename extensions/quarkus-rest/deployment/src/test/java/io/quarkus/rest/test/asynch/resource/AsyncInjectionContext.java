package io.quarkus.rest.test.asynch.resource;

public class AsyncInjectionContext implements AsyncInjectionContextInterface
{

   @Override
   public int foo()
   {
      return 42;
   }

}
