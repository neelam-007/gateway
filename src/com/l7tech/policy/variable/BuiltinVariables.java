/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Lists built-in system variables and their metadata
 */
public class BuiltinVariables {
    public static final String PREFIX_REQUEST_HTTP_HEADER = "request.http.header";
    public static final String PREFIX_REQUEST_HTTP_HEADER_VALUES = "request.http.headerValues";

    public static final String PREFIX_RESPONSE_HTTP_HEADER = "response.http.header";
    public static final String PREFIX_RESPONSE_HTTP_HEADER_VALUES = "response.http.headerValues";

    public static final String PREFIX_GATEWAY_TIME = "gateway.time";
    public static final String PREFIX_REQUEST_TIME = "request.time";
    public static final String PREFIX_SERVICE_URL = "service.url";
    public static final String PREFIX_REQUEST_URL = "request.url";
    public static final String PREFIX_CLUSTER_PROPERTY = "gateway"; // value of a variable in the cluster property table 

    private static final Map metadataByName = new HashMap();

    public static final String TIMESUFFIX_FORMAT_ISO8601 = "iso8601";
    public static final String TIMESUFFIX_ZONE_UTC = "utc";
    public static final String TIMESUFFIX_ZONE_LOCAL = "local";

    public static final String URLSUFFIX_HOST = "host";
    public static final String URLSUFFIX_PROTOCOL = "protocol";
    public static final String URLSUFFIX_PORT = "port";
    public static final String URLSUFFIX_FILE = "file";
    public static final String URLSUFFIX_PATH = "path";
    public static final String URLSUFFIX_QUERY = "query";
    public static final String URLSUFFIX_FRAGMENT = "fragment";

    public static boolean isSupported(String name) {
        return getMetadata(name) != null;
    }

    public static boolean isSettable(String name) {
        VariableMetadata metadata = getMetadata(name);
        if (metadata == null) return false;
        return metadata.isSettable();
    }

    public static boolean isMultivalued(String name) {
        VariableMetadata meta = getMetadata(name);
        if (meta == null) return false;
        return meta.isMultivalued();
    }

    public static VariableMetadata getMetadata(String name) {
        // Try simple name first
        final String lname = name.toLowerCase();
        VariableMetadata meta = (VariableMetadata)metadataByName.get(lname);
        if (meta == null) {
            // Try prefixed name
            int pos = lname.indexOf(".");
            while (pos > 0) {
                meta = (VariableMetadata)metadataByName.get(lname.substring(0,pos));
                if (meta != null) break;
                pos = lname.indexOf(".", pos+1);
            }
        }
        return meta;
    }

    public static Map getAllMetadata() {
        return Collections.unmodifiableMap(metadataByName);
    }

    private static final VariableMetadata[] VARS = {
        new VariableMetadata("request.tcp.remoteAddress", false, false, null, false),
        new VariableMetadata("request.tcp.remoteip", false, false, "request.tcp.remoteAddress", false),
        new VariableMetadata("request.tcp.remoteHost", false, false, null, false),
        new VariableMetadata("request.authenticateduser", false, false, null, false),
        new VariableMetadata("request.tcp.localPort", false, false, null, false),
        new VariableMetadata("request.http.method", false, false, null, false),
        new VariableMetadata("request.http.uri", false, false, null, false),
        new VariableMetadata(PREFIX_REQUEST_URL, true, false, null, false),
        new VariableMetadata("request.http.secure", false, false, null, false),
        new VariableMetadata("request.http.queryString", false, false, null, false),
        new VariableMetadata("auditLevel", false, false, null, true),
        new VariableMetadata("request.soap.operation", false, false, null, false),
        new VariableMetadata("request.soap.operationname", false, false, "request.soap.operation", false),
        new VariableMetadata("request.soap.namespace", false, false, null, false),
        new VariableMetadata("request.soap.urn", false, false, "request.soap.namespace", false),
        new VariableMetadata("requestId", false, false, null, false),
        new VariableMetadata("routingStatus", false, false, null, false),
        new VariableMetadata("request.elapsedTime", false, false, null, false),
        new VariableMetadata(PREFIX_GATEWAY_TIME, true, false, null, false),
        new VariableMetadata(PREFIX_REQUEST_TIME, true, false, null, false),
        new VariableMetadata(PREFIX_SERVICE_URL, true, false, null, false),

        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER, true, false, null, false),
        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER_VALUES, true, true, null, false),

        new VariableMetadata("response.http.status", false, false, null, false),
        new VariableMetadata(BuiltinVariables.PREFIX_CLUSTER_PROPERTY, true, true, null, true)
    };

    static {
        for (int i = 0; i < VARS.length; i++) {
            metadataByName.put(VARS[i].getName().toLowerCase(), VARS[i]);
        }
    }

    private static final String[] SINGLEOBJECT = {
        "request.http.clientCert",
        "ssg.cert",
        "protectedService.cert",
        "authenticatedUser",
        "service",
    };

    private static final String[] MULTIOBJECT = {
        "request.http.clientCert.chain",
        "ssg.cert.chain",
    };
}
