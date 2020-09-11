package io.quarkus.rest.test.providers.plain.resource;

public class CharsetFoo {
   private String s;

   public CharsetFoo(final String s) {
      this.s = s;
   }

   public String valueOf() {
      return s;
   }

   public String toString() {
      return s;
   }
}
