package io.quarkus.logback.runtime;

import java.net.URL;
import java.util.Optional;
import java.util.logging.Handler;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.slf4j.helpers.Util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusPrinter;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LogbackRecorder {

    private static volatile LoggerContext defaultLoggerContext;

    public void init() {
        if (defaultLoggerContext == null) {
            defaultLoggerContext = new LoggerContext();
            try {
                try {
                    new ContextInitializer(defaultLoggerContext) {
                        public URL findURLOfDefaultConfigurationFile(boolean updateStatus) {
                            String logbackConfigFile = OptionHelper.getSystemProperty(CONFIG_FILE_PROPERTY);
                            if (logbackConfigFile != null) {
                                return super.findURLOfDefaultConfigurationFile(updateStatus);
                            }

                            URL url = Loader.getResource(TEST_AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
                            if (url != null) {
                                return url;
                            }

                            url = Loader.getResource(GROOVY_AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
                            if (url != null) {
                                return url;
                            }
                            url = Loader.getResource(GROOVY_AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
                            if (url != null) {
                                return url;
                            }

                            return Loader.getResource(AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
                        }
                    }.autoConfig();
                } catch (JoranException je) {
                    Util.report("Failed to auto configure default logger context", je);
                }
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
