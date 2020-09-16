package io.quarkus.rest.test.security;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @tpSubChapter Security
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test that Bearer token is correctly loaded from ClientConfigProvider impl and used for outgoing requests.
 */
@DisplayName("Client Config Provider Bearer Token Test")
public class ClientConfigProviderBearerTokenTest {

    @Test
    @DisplayName("Test Client Config Provider Bearer Token")
    public void testClientConfigProviderBearerToken() throws IOException {
        String jarPath = ClientConfigProviderTestJarHelper.createClientConfigProviderTestJarWithBearerToken();
        Process process = ClientConfigProviderTestJarHelper.runClientConfigProviderBearerTestJar(
                ClientConfigProviderTestJarHelper.TestType.TEST_BEARER_TOKEN_IS_USED, jarPath);
        String line = ClientConfigProviderTestJarHelper.getResultOfProcess(process);
        Assertions.assertEquals(line, "200");
        process.destroy();
        process = ClientConfigProviderTestJarHelper.runClientConfigProviderBearerTestJar(
                ClientConfigProviderTestJarHelper.TestType.TEST_BEARER_TOKEN_IGNORED_IF_BASIC_SET_BY_USER, jarPath);
        line = ClientConfigProviderTestJarHelper.getResultOfProcess(process);
        Assertions.assertEquals(line, "Credentials set by user had precedence");
        process.destroy();
        Assertions.assertTrue(new File(jarPath).delete());
    }
}
