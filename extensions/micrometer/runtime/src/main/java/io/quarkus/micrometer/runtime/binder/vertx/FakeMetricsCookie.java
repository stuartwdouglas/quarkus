package io.quarkus.micrometer.runtime.binder.vertx;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;

public class FakeMetricsCookie implements Cookie {

    public static final String NAME = FakeMetricsCookie.class.getName();

    MetricsContext metricsContext;

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public Cookie setValue(String value) {
        return null;
    }

    @Override
    public Cookie setDomain(String domain) {
        return null;
    }

    @Override
    public String getDomain() {
        return null;
    }

    @Override
    public Cookie setPath(String path) {
        return null;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public Cookie setMaxAge(long maxAge) {
        return null;
    }

    @Override
    public Cookie setSecure(boolean secure) {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public Cookie setHttpOnly(boolean httpOnly) {
        return null;
    }

    @Override
    public boolean isHttpOnly() {
        return false;
    }

    @Override
    public Cookie setSameSite(CookieSameSite policy) {
        return null;
    }

    @Override
    public CookieSameSite getSameSite() {
        return null;
    }

    @Override
    public String encode() {
        return null;
    }
}
