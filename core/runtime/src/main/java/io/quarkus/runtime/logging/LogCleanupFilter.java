package io.quarkus.runtime.logging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logging.Logger;

import io.quarkus.dev.testing.ContinuousTestingLogHandler;

public class LogCleanupFilter implements Filter {

    private Map<String, LogCleanupFilterElement> filterElements = new HashMap<>();

    public LogCleanupFilter(List<LogCleanupFilterElement> filterElements) {
        for (LogCleanupFilterElement element : filterElements) {
            this.filterElements.put(element.getLoggerName(), element);
        }
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        // Only allow filtering messages of warning level and lower
        if (record.getLevel().intValue() > Level.WARNING.intValue()) {
            return true;
        }
        //handle log messages for continous testing
        Predicate<LogRecord> handler = ContinuousTestingLogHandler.getLogHandler();
        if (handler != null) {
            if (!handler.test(record)) {
                return false;
            }
        }

        LogCleanupFilterElement filterElement = filterElements.get(record.getLoggerName());
        if (filterElement != null) {
            for (String messageStart : filterElement.getMessageStarts()) {
                if (record.getMessage().startsWith(messageStart)) {
                    record.setLevel(filterElement.getTargetLevel());
                    if (filterElement.getTargetLevel().intValue() <= org.jboss.logmanager.Level.TRACE.intValue()) {
                        return Logger.getLogger(record.getLoggerName()).isTraceEnabled();
                    } else {
                        return Logger.getLogger(record.getLoggerName()).isDebugEnabled();
                    }
                }
            }
        }
        return true;
    }

}
