package io.quarkus.it.jgit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import org.arquillian.smart.testing.rules.git.server.EmbeddedHttpGitServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JGitTest {

    private final EmbeddedHttpGitServer gitServer = EmbeddedHttpGitServer
            .fromBundle("booster-catalog", "repos/booster-catalog.bundle").usingAnyFreePort().create();

    @BeforeEach
    public void startGitServer() throws Exception {
        gitServer.start();
    }

    @Test
    public void shouldClone() {
        String url = String.format("http://localhost:%s/booster-catalog", gitServer.getPort());
        given().queryParam("url", url).get("/jgit/clone").then().body(is("master"));
    }

    @AfterEach
    public void stopGitServer() throws Exception {
        gitServer.stop();
    }

}
