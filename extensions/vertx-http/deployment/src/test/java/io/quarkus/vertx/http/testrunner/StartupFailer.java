package io.quarkus.vertx.http.testrunner;

import io.quarkus.runtime.LaunchMode;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class StartupFailer {

    public void route(@Observes Router router) {
        //fail();
    }

    void fail() {
        if (LaunchMode.current() == LaunchMode.TEST) {
            throw new RuntimeException("FAIL");
        }
    }
}
