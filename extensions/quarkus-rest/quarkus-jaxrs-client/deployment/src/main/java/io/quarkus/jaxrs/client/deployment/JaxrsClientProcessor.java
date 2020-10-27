package io.quarkus.jaxrs.client.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.jaxrs.client.runtime.JaxrsClientRecorder;
import io.quarkus.rest.common.runtime.core.Serialisers;
import io.quarkus.rest.spi.ClientProxiesBuildItem;

public class JaxrsClientProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupClientProxies(ClientProxiesBuildItem clientProxiesBuildItem, JaxrsClientRecorder recorder) {
        recorder.setupClientProxies(clientProxiesBuildItem.getClientProxies());

        Serialisers serialisers = recorder.createSerializers();

    }
}
