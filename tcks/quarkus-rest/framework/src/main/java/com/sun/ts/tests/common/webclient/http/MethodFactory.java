/*
 * Copyright (c) 2007, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

/*
 * $Id$
 */

package com.sun.ts.tests.common.webclient.http;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * Simple factory class which returns HttpMethod implementations based on a
 * request line.
 * <p>
 * For example, a request line of <tt>GET /index.jsp HTTP/1.0</tt> would return
 * an HttpMethod implementation that handles GET requests using HTTP/1.0.
 * </p>
 */

public class MethodFactory {

    /**
     * HTTP GET
     */
    private static final String GET_METHOD = "GET";

    /**
     * HTTP POST
     */
    private static final String POST_METHOD = "POST";

    /**
     * HTTP HEAD
     */
    private static final String HEAD_METHOD = "HEAD";

    /**
     * HTTP PUT
     */
    private static final String PUT_METHOD = "PUT";

    /**
     * HTTP DELETE
     */
    private static final String DELETE_METHOD = "DELETE";

    /**
     * HTTP OPTIONS
     */
    private static final String OPTIONS_METHOD = "OPTIONS";

    /**
     * Private constructor as all interaction with this class is through the
     * getInstance() method.
     */
    private MethodFactory() {
    }

    /*
     * public methods
     * ========================================================================
     */

    /**
     * Returns the approriate request method based on the provided request string.
     * The request must be in the format of METHOD URI_PATH HTTP_VERSION, i.e. GET
     * /index.jsp HTTP/1.1.
     *
     * @return HttpMethod based in request.
     */
    public static HttpRequestBase getInstance(String request) {
        StringTokenizer st = new StringTokenizer(request);
        String method;
        String query = null;
        String uri;
        String version;
        try {
            method = st.nextToken();
            uri = st.nextToken();
            version = st.nextToken();
        } catch (NoSuchElementException nsee) {
            throw new IllegalArgumentException(
                    "Request provided: " + request + " is malformed.");
        }

        HttpRequestBase req;

        if (method.equals(GET_METHOD)) {
            req = new HttpGet(uri);
        } else if (method.equals(POST_METHOD)) {
            req = new HttpPost(uri);
        } else if (method.equals(PUT_METHOD)) {
            req = new HttpPut(uri);
        } else if (method.equals(DELETE_METHOD)) {
            req = new HttpDelete(uri);
        } else if (method.equals(HEAD_METHOD)) {
            req = new HttpHead(uri);
        } else if (method.equals(OPTIONS_METHOD)) {
            req = new HttpOptions(uri);
        } else {
            throw new IllegalArgumentException("Invalid method: " + method);
        }

        setHttpVersion(version, req);

        return req;
    }

    /*
     * private methods
     * ========================================================================
     */

    /**
     * Sets the HTTP version for the method in question.
     *
     * @param version
     *        HTTP version to use for this request
     * @param method
     *        method to adjust HTTP version
     */
    private static void setHttpVersion(String version, HttpRequestBase method) {
        final String oneOne = "HTTP/1.1";
        method.setProtocolVersion(
                version.equals(oneOne) ? new ProtocolVersion("HTTP", 1, 1) : new ProtocolVersion("HTTP", 1, 0));
    }
}
