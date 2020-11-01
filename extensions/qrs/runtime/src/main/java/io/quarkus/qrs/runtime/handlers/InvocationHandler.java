package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.spi.EndpointInvoker;

public class InvocationHandler implements RestHandler {
    private final EndpointInvoker invoker;

    public InvocationHandler(EndpointInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        if (requestContext.getResult() != null) {
            //processing was aborted
            //but we still follow through with the handler chain
            return;
        }
        //suspend processing
        //need to do it here to avoid a race
        boolean async = requestContext.getAsyncResponse() != null;
        if (async) {
            requestContext.suspend();
        }
        try {
            Object result = invoker.invoke(requestContext.getEndpointInstance(), requestContext.getParameters());
            if (!async) {
                requestContext.setResult(result);
            }
        } catch (Throwable t) {
            requestContext.setThrowable(t);
            if (async) {
                requestContext.resume();
            }
        }
    }
}
