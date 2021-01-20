package io.quarkus.resteasy.reactive.server.runtime.cdi;

import javax.ws.rs.container.CompletionCallback;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;

public class ArcParamExtractor implements ParameterExtractor {

    public String beanIdentifier;
    private volatile InjectableBean<?> bean;
    private volatile ArcContainer container;

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        if (bean == null) {
            container = Arc.container();
            bean = container.bean(beanIdentifier);
        }
        InstanceHandle<?> instance = container.instance(bean);
        context.registerCompletionCallback(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                instance.close();
            }
        });
        return instance.get();
    }
}
