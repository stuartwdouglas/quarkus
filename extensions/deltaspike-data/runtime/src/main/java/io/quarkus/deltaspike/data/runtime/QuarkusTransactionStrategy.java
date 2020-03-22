package io.quarkus.deltaspike.data.runtime;

import javax.interceptor.InvocationContext;

import org.apache.deltaspike.jpa.spi.transaction.TransactionStrategy;

public class QuarkusTransactionStrategy implements TransactionStrategy {
    @Override
    public Object execute(InvocationContext invocationContext) throws Exception {
        return invocationContext.proceed();
    }
}
