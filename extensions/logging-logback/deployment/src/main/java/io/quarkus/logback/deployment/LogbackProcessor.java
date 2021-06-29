package io.quarkus.logback.deployment;

import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.event.BodyEvent;
import ch.qos.logback.core.joran.event.EndEvent;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.StartEvent;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.Loader;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.logback.runtime.LogbackRecorder;
import io.quarkus.logback.runtime.events.BodySub;
import io.quarkus.logback.runtime.events.EndSub;
import io.quarkus.logback.runtime.events.EventSubstitution;
import io.quarkus.logback.runtime.events.StartSub;

public class LogbackProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void init(LogbackRecorder recorder, RecorderContext context) throws JoranException {
        context.registerSubstitution(StartEvent.class, StartSub.class, EventSubstitution.class);
        context.registerSubstitution(BodyEvent.class, BodySub.class, EventSubstitution.class);
        context.registerSubstitution(EndEvent.class, EndSub.class, EventSubstitution.class);
        final AtomicReference<List<SaxEvent>> events = new AtomicReference<>();

        JoranConfigurator configurator = new JoranConfigurator() {
            @Override
            public void doConfigure(List<SaxEvent> eventList) throws JoranException {
                events.set(eventList);
            }
        };
        configurator.setContext(new LoggerContext());
        configurator.doConfigure(getUrl());

        recorder.init(events.get());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem handler(LogbackRecorder recorder) {
        return new LogHandlerBuildItem(recorder.createHandler());
    }

    private URL getUrl() {
        URL url = Loader.getResource(ContextInitializer.TEST_AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
        if (url != null) {
            return url;
        }
        return Loader.getResource(ContextInitializer.AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
    }
}
