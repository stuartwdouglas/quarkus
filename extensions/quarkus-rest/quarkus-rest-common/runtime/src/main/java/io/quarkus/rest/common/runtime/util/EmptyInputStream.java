package io.quarkus.rest.common.runtime.util;

import java.io.IOException;
import java.io.InputStream;

public class EmptyInputStream extends InputStream {

    public static final EmptyInputStream INSTANCE = new EmptyInputStream();

    @Override
    public int read() throws IOException {
        return -1;
    }
}
