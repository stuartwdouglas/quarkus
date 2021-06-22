package io.quarkus.logback.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.logback.runtime.LogbackRecorder;

public class LogbackProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void init(LogbackRecorder recorder) {
        recorder.init();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem handler(LogbackRecorder recorder) {
        return new LogHandlerBuildItem(recorder.createHandler());
    }

}
