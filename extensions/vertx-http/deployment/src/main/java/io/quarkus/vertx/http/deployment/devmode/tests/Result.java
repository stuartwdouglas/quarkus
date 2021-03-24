package io.quarkus.vertx.http.deployment.devmode.tests;

import org.junit.platform.engine.TestExecutionResult;

public class Result {

    private String name;

    private TestExecutionResult.Status status;

    private String exceptionType;

    private String exceptionMessage;

    public Result() {
    }

    public Result(String name, TestExecutionResult.Status status, String exceptionType, String exceptionMessage) {
        this.name = name;
        this.status = status;
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
    }

    public String getName() {
        return name;
    }

    public Result setName(String name) {
        this.name = name;
        return this;
    }

    public TestExecutionResult.Status getStatus() {
        return status;
    }

    public Result setStatus(TestExecutionResult.Status status) {
        this.status = status;
        return this;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public Result setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
        return this;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public Result setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
        return this;
    }
}
