/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import org.apache.commons.httpclient.Header;

import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Holds Http headers.
 *
 * User: mike
 * Date: Sep 19, 2003
 * Time: 5:07:17 PM
 */
public class HttpHeaders {
    private final MyHeader[] headers;

    private static class MyHeader {
        private final String value;
        private final  String name;

        private MyHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }
    };

    public static interface ValueProvider {
        String getHeaderValue(String headerName);
    }

    public HttpHeaders(Enumeration headerNames, ValueProvider allValues) {
        ArrayList accum = new ArrayList();
        while (headerNames.hasMoreElements()) {
            String name = (String) headerNames.nextElement();
            String value = allValues.getHeaderValue(name);
            accum.add(new MyHeader(name, value));
        }
        this.headers = (MyHeader[]) accum.toArray(new MyHeader[0]);
    }

    public HttpHeaders(Header[] headers) {
        ArrayList accum = new ArrayList();
        for (int i = 0; i < headers.length; i++) {
            Header header = headers[i];
            accum.add(new MyHeader(header.getName(), header.getValue()));
        }
        this.headers = (MyHeader[]) accum.toArray(new MyHeader[0]);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < headers.length; i++) {
            MyHeader header = headers[i];
            sb.append(header.name).append(": ").append(header.value).append("\n");
        }
        return sb.toString();
    }
}
