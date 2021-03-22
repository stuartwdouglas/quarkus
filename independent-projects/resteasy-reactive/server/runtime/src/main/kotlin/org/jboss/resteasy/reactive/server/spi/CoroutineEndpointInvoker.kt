package org.jboss.resteasy.reactive.server.spi

interface CoroutineEndpointInvoker: EndpointInvoker {
    suspend fun invoke(instance: Any, parameters: Array<out Any>): Any?
}
