package io.quarkus.bootstrap.workspace;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class DefaultProcessedSources implements ProcessedSources, Serializable {

    private final Path srcDir;
    private final Path destinationDir;
    private final Map<Object, Object> data;

    public DefaultProcessedSources(Path srcDir, Path destinationDir) {
        this(srcDir, destinationDir, Collections.emptyMap());
    }

    public DefaultProcessedSources(Path srcDir, Path destinationDir, Map<Object, Object> data) {
        this.srcDir = srcDir;
        this.destinationDir = destinationDir;
        this.data = data;
    }

    @Override
    public Path getSourceDir() {
        return srcDir;
    }

    @Override
    public Path getDestinationDir() {
        return destinationDir;
    }

    public <T> T getValue(Object key, Class<T> type) {
        final Object o = data.get(key);
        return o == null ? null : type.cast(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, destinationDir, srcDir);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultProcessedSources other = (DefaultProcessedSources) obj;
        return Objects.equals(data, other.data) && Objects.equals(destinationDir, other.destinationDir)
                && Objects.equals(srcDir, other.srcDir);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(srcDir).append(" -> ").append(destinationDir);
        if (!data.isEmpty()) {
            final Iterator<Map.Entry<Object, Object>> i = data.entrySet().iterator();
            Map.Entry<Object, Object> e = i.next();
            buf.append(" ").append(e.getKey()).append("=").append(e.getValue());
            while (i.hasNext()) {
                e = i.next();
                buf.append(",").append(e.getKey()).append("=").append(e.getValue());
            }
        }
        return buf.toString();
    }
}
