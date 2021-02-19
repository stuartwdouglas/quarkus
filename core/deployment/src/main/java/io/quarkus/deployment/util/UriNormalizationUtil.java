package io.quarkus.deployment.util;

import java.net.URI;
import java.net.URISyntaxException;

public class UriNormalizationUtil {
    private UriNormalizationUtil() {
    }

    //TODO Javadoc
    public static URI toURI(String path, boolean trailingSlash) {
        try {

            path = path.replaceAll("//", "/");
            if (path.contains("..") || path.contains("%")) {
                // TODO Proper message
                throw new IllegalArgumentException();
            }
            URI uri = new URI(path).normalize();
            if (uri.getPath().equals("")) {
                return trailingSlash ? new URI("/") : new URI("");
            }
            if (trailingSlash && !path.endsWith("/")) {
                uri = new URI(uri.getPath() + "/");
            }
            return uri;
        } catch (URISyntaxException e) {
            //TODO better message
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    //TODO Javadoc
    public static URI normalizeWithBase(URI base, String segment, boolean trailingSlash) {
        if (segment == null || segment.trim().isEmpty()) {
            return base;
        }
        URI segmentUri = toURI(segment, trailingSlash);
        URI resolvedUri = base.resolve(segmentUri);
        //TODO Remove sysout
        System.out.println(base.getPath() + " + " + segment + " = " + resolvedUri.getPath());
        return resolvedUri;
    }
}
