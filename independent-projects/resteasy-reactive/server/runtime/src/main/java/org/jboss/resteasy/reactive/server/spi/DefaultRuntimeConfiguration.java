package org.jboss.resteasy.reactive.server.spi;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;

public class DefaultRuntimeConfiguration implements RuntimeConfiguration {
    final Duration readTimeout;
    final boolean deleteUploadedFilesOnEnd;
    final String uploadsDirectory;
    final Charset defaultCharset;
    final Optional<Long> maxBodySize;
    final long maxFormAttributeSize;

    public DefaultRuntimeConfiguration(Duration readTimeout, boolean deleteUploadedFilesOnEnd, String uploadsDirectory,
            Charset defaultCharset, Optional<Long> maxBodySize, long maxFormAttributeSize) {
        this.readTimeout = readTimeout;
        this.deleteUploadedFilesOnEnd = deleteUploadedFilesOnEnd;
        this.uploadsDirectory = uploadsDirectory;
        this.defaultCharset = defaultCharset;
        this.maxBodySize = maxBodySize;
        this.maxFormAttributeSize = maxFormAttributeSize;
    }

    @Override
    public Duration readTimeout() {
        return readTimeout;
    }

    @Override
    public Body body() {
        return new Body() {
            @Override
            public boolean deleteUploadedFilesOnEnd() {
                return deleteUploadedFilesOnEnd;
            }

            @Override
            public String uploadsDirectory() {
                return uploadsDirectory;
            }

            @Override
            public Charset defaultCharset() {
                return defaultCharset;
            }
        };
    }

    @Override
    public Limits limits() {
        return new Limits() {
            @Override
            public Optional<Long> maxBodySize() {
                return maxBodySize;
            }

            @Override
            public long maxFormAttributeSize() {
                return maxFormAttributeSize;
            }
        };
    }
}
