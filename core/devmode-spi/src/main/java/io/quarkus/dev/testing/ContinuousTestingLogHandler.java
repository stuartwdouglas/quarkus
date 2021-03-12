package io.quarkus.dev.testing;

import java.util.function.Predicate;
import java.util.logging.LogRecord;

public class ContinuousTestingLogHandler {

    private static volatile Predicate<LogRecord> logHandler;

    public static Predicate<LogRecord> getLogHandler() {
        return logHandler;
    }

    public static void setLogHandler(Predicate<LogRecord> logHandler) {
        ContinuousTestingLogHandler.logHandler = logHandler;
    }
}
