package io.quarkus.deployment.dev.testing;

import io.quarkus.deployment.dev.testing.runner.TestState;

public interface TestController {

    TestState currentState();

    void runAllTests();

}
