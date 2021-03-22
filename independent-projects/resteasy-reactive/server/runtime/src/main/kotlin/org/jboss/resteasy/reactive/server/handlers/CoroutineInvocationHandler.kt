package org.jboss.resteasy.reactive.server.handlers

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import org.jboss.resteasy.reactive.server.spi.CoroutineEndpointInvoker
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler

class CoroutineInvocationHandler(private val invoker: CoroutineEndpointInvoker) : ServerRestHandler {
    override fun handle(requestContext: ResteasyReactiveRequestContext) {
        if (requestContext.result != null) {
            return
        }
        requestContext.requireCDIRequestScope()
        requestContext.suspend()
        GlobalScope.launch {
            try {
                requestContext.result = invoker.invoke(requestContext.endpointInstance, requestContext.parameters)
            } catch (t: Throwable) {
                // passing true since the target doesn't change and we want response filters to be able to know what the resource method was
                requestContext.handleException(t, true)
            }
            requestContext.resume()
        }
    }
}
