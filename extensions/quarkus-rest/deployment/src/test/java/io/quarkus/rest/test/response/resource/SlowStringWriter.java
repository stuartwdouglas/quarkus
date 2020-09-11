package io.quarkus.rest.test.response.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.AsyncMessageBodyWriter;
import org.jboss.resteasy.spi.AsyncOutputStream;

@Provider
public class SlowStringWriter implements AsyncMessageBodyWriter<SlowString>
{
   @Override
   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return type == SlowString.class;
   }

   @Override
   public void writeTo(SlowString t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                       MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
         throws IOException, WebApplicationException
   {
      entityStream.write(t.string.getBytes(StandardCharsets.UTF_8));
   }

   @Override
   public CompletionStage<Void> asyncWriteTo(SlowString t, Class<?> type, Type genericType, Annotation[] annotations,
                                             MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                                             AsyncOutputStream entityStream)
   {
      return CompletableFuture.runAsync(() -> {
         try
         {
            Thread.sleep(100);
         } catch (InterruptedException e)
         {
         }
      }).thenCompose(v -> entityStream.asyncWrite(t.string.getBytes(StandardCharsets.UTF_8)));
   }

}
