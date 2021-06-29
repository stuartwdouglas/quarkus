package io.quarkus.logback.runtime;

import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.slf4j.helpers.Util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LogbackRecorder {

    private static volatile LoggerContext defaultLoggerContext;

    public void init(List<SaxEvent> configEvents) {
        if (defaultLoggerContext == null) {
            defaultLoggerContext = new LoggerContext();
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(defaultLoggerContext);
                configurator.doConfigure(configEvents);
                // logback-292
                if (!StatusUtil.contextHasStatusListener(defaultLoggerContext)) {
                    StatusPrinter.printInCaseOfErrorsOrWarnings(defaultLoggerContext);
                }
            } catch (Exception t) { // see LOGBACK-1159
                Util.report("Failed to instantiate [" + LoggerContext.class.getName() + "]", t);
            }
        }
    }

    public RuntimeValue<Optional<Handler>> createHandler() {

        return new RuntimeValue<>(Optional.of(new ExtHandler() {

            @Override
            public final void doPublish(final ExtLogRecord record) {
                Logger logger = defaultLoggerContext.getLogger(record.getLoggerName());
                logger.callAppenders(new LoggingEventWrapper(record, getFormatter()));
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        }));

    }

}
