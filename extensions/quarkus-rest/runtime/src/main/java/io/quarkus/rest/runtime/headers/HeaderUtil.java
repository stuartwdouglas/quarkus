package io.quarkus.rest.runtime.headers;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate;

import io.quarkus.rest.runtime.util.DateUtil;

/**
 * These work for MultivaluedMap with String and Object
 */
public class HeaderUtil {

    public static String headerToString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else {
            // TODO: we probably want a more direct way to get the delegate instead of going through all the indirection
            return RuntimeDelegate.getInstance().createHeaderDelegate((Class<Object>) obj.getClass()).toString(obj);
        }
    }

    public static Set<String> getAllowedMethods(MultivaluedMap<String, ? extends Object> headers) {
        List<? extends Object> allowed = headers.get(HttpHeaders.ALLOW);
        if ((allowed == null) || allowed.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> allowedMethods = new HashSet<>();
        for (Object header : allowed) {
            if (header instanceof String) {
                String[] list = ((String) header).split(",");
                for (String str : list) {
                    String trimmed = str.trim();
                    if (!trimmed.isEmpty()) {
                        allowedMethods.add(trimmed.toUpperCase());
                    }
                }
            } else {
                allowedMethods.add(HeaderUtil.headerToString(header).toUpperCase());
            }
        }
        return allowedMethods;
    }

    public static Date getDate(MultivaluedMap<String, ? extends Object> headers) {
        return firstHeaderToDate(HttpHeaders.DATE, headers);
    }

    public static Date getLastModified(MultivaluedMap<String, ? extends Object> headers) {
        return firstHeaderToDate(HttpHeaders.LAST_MODIFIED, headers);
    }

    private static Date firstHeaderToDate(String date, MultivaluedMap<String, ? extends Object> headers) {
        Object d = headers.getFirst(date);
        if (d == null)
            return null;
        if (d instanceof Date)
            return (Date) d;
        return DateUtil.parseDate(d.toString());
    }

    public static URI getLocation(MultivaluedMap<String, ? extends Object> headers) {
        Object uri = headers.getFirst(HttpHeaders.LOCATION);
        if (uri == null) {
            return null;
        }
        if (uri instanceof URI) {
            return (URI) uri;
        }
        String str = null;
        if (uri instanceof String) {
            str = (String) uri;
        } else {
            str = HeaderUtil.headerToString(uri);
        }
        return URI.create(str);
    }

    public static MediaType getMediaType(MultivaluedMap<String, ? extends Object> headers) {
        Object first = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (first instanceof String) {
            String contentType = (String) first;
            return MediaType.valueOf(contentType);
        } else {
            return (MediaType) first;
        }
    }

    public static Locale getLanguage(MultivaluedMap<String, ? extends Object> headers) {
        Object obj = headers.getFirst(HttpHeaders.CONTENT_LANGUAGE);
        if (obj == null) {
            return null;
        }
        if (obj instanceof Locale) {
            return (Locale) obj;
        }
        return Locale.forLanguageTag(HeaderUtil.headerToString(obj));
    }

    public static int getLength(MultivaluedMap<String, ? extends Object> headers) {
        Object obj = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
        if (obj == null) {
            return -1;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        return Integer.parseInt(HeaderUtil.headerToString(obj));
    }

    public static Map<String, NewCookie> getCookies(MultivaluedMap<String, ? extends Object> headers) {
        List<?> list = headers.get(HttpHeaders.SET_COOKIE);
        if ((list == null) || list.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, NewCookie> cookies = new HashMap<>();
        for (Object obj : list) {
            if (obj instanceof NewCookie) {
                NewCookie cookie = (NewCookie) obj;
                cookies.put(cookie.getName(), cookie);
            } else {
                String str = HeaderUtil.headerToString(obj);
                NewCookie cookie = NewCookie.valueOf(str);
                cookies.put(cookie.getName(), cookie);
            }
        }
        return cookies;
    }

    public static EntityTag getEntityTag(MultivaluedMap<String, ? extends Object> headers) {
        Object d = headers.getFirst(HttpHeaders.ETAG);
        if (d == null) {
            return null;
        }
        if (d instanceof EntityTag) {
            return (EntityTag) d;
        }
        return EntityTag.valueOf(HeaderUtil.headerToString(d));
    }

    public static String getHeaderString(MultivaluedMap<String, String> headers, String name) {
        List<String> list = headers.get(name);
        if (list == null) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s);
        }
        return sb.toString();
    }

}
