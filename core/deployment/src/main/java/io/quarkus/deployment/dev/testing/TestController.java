package io.quarkus.deployment.dev.testing;

public interface TestController {

    TestState currentState();

    void runAllTests();

}
