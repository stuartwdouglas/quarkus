package io.quarkus.rest.test.validation.cdi.resource;

import javax.ejb.Stateless;

@Stateless
public class SessionResourceImpl implements SessionResourceLocal, SessionResourceRemote {
   public String test(String param) {
      return param;
   }
}
