package io.quarkus.deployment.dev.testing;

import java.util.List;
import java.util.logging.LogRecord;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;

public class TestResult {

    final String displayName;
    final UniqueId uniqueId;
    final TestExecutionResult testExecutionResult;
    final List<LogRecord> logOutput;

    public TestResult(String displayName, UniqueId uniqueId, TestExecutionResult testExecutionResult,
            List<LogRecord> logOutput) {
        this.displayName = displayName;
        this.uniqueId = uniqueId;
        this.testExecutionResult = testExecutionResult;
        this.logOutput = logOutput;
    }

    public TestExecutionResult getTestExecutionResult() {
        return testExecutionResult;
    }

    public List<LogRecord> getLogOutput() {
        return logOutput;
    }

    public String getDisplayName() {
        return displayName;
    }
}
