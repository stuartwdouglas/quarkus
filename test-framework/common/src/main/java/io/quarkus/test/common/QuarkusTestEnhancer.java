package io.quarkus.test.common;

import org.jboss.jandex.IndexView;

public interface QuarkusTestEnhancer {

    void beforeStart(IndexView indexView);

    void afterShutdown();

}
