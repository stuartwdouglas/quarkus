package io.quarkus.rest.test.cdi.validation.resource;

import javax.ws.rs.QueryParam;

public class QueryBeanParamImpl implements QueryBeanParam
{
   @QueryParam("foo")
   private String param;

   @Override
   public String getParam()
   {
      return param;
   }
}
