package io.quarkus.rest.test.interceptor.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

public class ReaderInterceptorContextInterceptor implements ReaderInterceptor {

   @Override
   public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
      List<String> list = new ArrayList<String>();
      list.add("123");
      list.add("789");
      context.getHeaders().put("header", list);
      return context.proceed();
   }
}
