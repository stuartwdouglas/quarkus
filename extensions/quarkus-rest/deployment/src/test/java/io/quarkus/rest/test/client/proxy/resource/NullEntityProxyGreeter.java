package io.quarkus.rest.test.client.proxy.resource;

public class NullEntityProxyGreeter {
   String greeting;

   public void setGreeting(String greeting) {
      this.greeting = greeting;
   }

   public String getGreeting() {
      return greeting;
   }
}
