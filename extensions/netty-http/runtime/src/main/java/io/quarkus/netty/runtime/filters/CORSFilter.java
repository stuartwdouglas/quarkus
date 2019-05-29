/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.netty.runtime.filters;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.quarkus.netty.runtime.CORSConfig;
import io.quarkus.router.RouterFilter;

public class CORSFilter implements RouterFilter {

    final CORSConfig corsConfig;

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ORIGIN = "Origin";
    private static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    public CORSFilter(CORSConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    @Override
    public void handleResponse(HttpRequest request, HttpResponse response) {
        String origin = request.headers().get(ORIGIN);
        if (origin != null) {
            String requestedMethods = request.headers().get(ACCESS_CONTROL_REQUEST_METHOD);
            if (requestedMethods != null) {
                processMethods(response, requestedMethods, corsConfig.methods.orElse(requestedMethods));
            }
            String requestedHeaders = request.headers().get(ACCESS_CONTROL_REQUEST_HEADERS);
            if (requestedHeaders != null) {
                processHeaders(response, requestedHeaders, corsConfig.headers.orElse(requestedHeaders));
            }
            String allowedOrigins = corsConfig.origins.orElse(null);
            boolean allowsOrigin = allowedOrigins == null || allowedOrigins.contains(origin);
            if (allowsOrigin) {
                response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }
            response.headers().set(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            corsConfig.exposedHeaders.ifPresent(exposed -> request.headers().get(ACCESS_CONTROL_EXPOSE_HEADERS, exposed));
        }
    }

    private void processHeaders(HttpResponse response, String requestedHeaders, String allowedHeaders) {
        String validHeaders = Arrays.stream(requestedHeaders.split(","))
                .filter(allowedHeaders::contains)
                .collect(Collectors.joining(","));
        if (!validHeaders.isEmpty())
            response.headers().set(ACCESS_CONTROL_ALLOW_HEADERS, validHeaders);
    }

    private void processMethods(HttpResponse response, String requestedMethods, String allowedMethods) {
        String validMethods = Arrays.stream(requestedMethods.split(","))
                .filter(allowedMethods::contains)
                .collect(Collectors.joining(","));
        if (!validMethods.isEmpty())
            response.headers().set(ACCESS_CONTROL_ALLOW_METHODS, validMethods);
    }

    @Override
    public void handleRequest(HttpRequest request) {
        //TODO: if this is an options request we should short circuit processing
    }
}
