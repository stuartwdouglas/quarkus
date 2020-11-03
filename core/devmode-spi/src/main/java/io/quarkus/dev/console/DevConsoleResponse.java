package io.quarkus.dev.console;

import java.util.List;
import java.util.Map;

public class DevConsoleResponse {

    private final int status;
    private final Map<String, List<String>> headers;
    private final byte[] body;

    public DevConsoleResponse(int status, Map<String, List<String>> headers, byte[] body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }
}
