package io.quarkus.rest.test.core.basic.resource;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.junit.jupiter.api.Assertions;

@Provider
@Priority(20)
public class ContextEncoderInterceptor implements WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        final String HEADER_ERROR_MESSAGE = "MessageBodyWriterContext in ContextEncoderInterceptor don't have correct headers";
        Assertions.assertTrue(context.getHeaders().containsKey("before-encoder"), HEADER_ERROR_MESSAGE);
        Assertions.assertFalse(context.getHeaders().containsKey("after-encoder"), HEADER_ERROR_MESSAGE);
        Assertions.assertFalse(context.getHeaders().containsKey("end"), HEADER_ERROR_MESSAGE);
        context.getHeaders().add("encoder", "true");
        context.proceed();
    }
}
