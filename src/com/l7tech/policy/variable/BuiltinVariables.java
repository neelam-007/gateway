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

    public static final String PREFIX_REQUEST_HTTP_PARAM = "request.http.parameter";

    public static final String PREFIX_REQUEST_JMS_MSG_PROP = "request.jms.property";
    public static final String PREFIX_RESPONSE_JMS_MSG_PROP = "response.jms.property";

    public static final String PREFIX_GATEWAY_TIME = "gateway.time";
    public static final String PREFIX_REQUEST_TIME = "request.time";
    public static final String PREFIX_SERVICE_URL = "service.url";
    public static final String PREFIX_REQUEST_URL = "request.url";
    public static final String PREFIX_CLUSTER_PROPERTY = "gateway"; // value of a variable in the cluster property table

    public static final String PREFIX_AUTHENTICATED_USER = "request.authenticateduser";

    private static final Map metadataByName = new HashMap();
    private static final Map metadataPresetByName = new HashMap();

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

    public static boolean isPredefined(String name) {
        return getMetadata(name, metadataPresetByName) != null;
    }

    public static boolean isSettable(String name) {
        VariableMetadata metadata = getMetadata(name);
        return metadata != null && metadata.isSettable();
    }

    public static boolean isMultivalued(String name) {
        VariableMetadata meta = getMetadata(name);
        return meta != null && meta.isMultivalued();
    }

    public static Map getAllMetadata() {
        return Collections.unmodifiableMap(metadataByName);
    }

    private static final VariableMetadata[] VARS = {
        new VariableMetadata("request.tcp.remoteAddress", false, false, null, false),
        new VariableMetadata("request.tcp.remoteip", false, false, "request.tcp.remoteAddress", false),
        new VariableMetadata("request.tcp.remoteHost", false, false, null, false),
        new VariableMetadata(PREFIX_AUTHENTICATED_USER, true, false, null, false),
        new VariableMetadata("request.clientid", false, false, null, false),
        new VariableMetadata("request.tcp.localPort", false, false, null, false, DataType.INTEGER),
        new VariableMetadata("request.http.method", false, false, null, false),
        new VariableMetadata("request.http.uri", false, false, null, false),
        new VariableMetadata(PREFIX_REQUEST_URL, true, false, null, false),
        new VariableMetadata("request.http.secure", false, false, null, false, DataType.BOOLEAN),
        new VariableMetadata("request.http.queryString", false, false, null, false),
        new VariableMetadata("request.ftp.path", false, false, null, false),
        new VariableMetadata("request.ftp.file", false, false, null, false),
        new VariableMetadata("request.ftp.unique", false, false, null, false),
        new VariableMetadata("request.ftp.secure", false, false, null, false),
        new VariableMetadata("auditLevel", false, false, null, true),
        new VariableMetadata("request.soap.operation", false, false, null, false),
        new VariableMetadata("request.soap.operationname", false, false, "request.soap.operation", false),
        new VariableMetadata("request.soap.namespace", false, false, null, false),
        new VariableMetadata("request.soap.urn", false, false, "request.soap.namespace", false),
        new VariableMetadata("requestId", false, false, null, false),
        new VariableMetadata("routingStatus", false, false, null, false),
        new VariableMetadata("request.elapsedTime", false, false, null, false, DataType.INTEGER),
        new VariableMetadata(PREFIX_GATEWAY_TIME, true, false, null, false),
        new VariableMetadata(PREFIX_REQUEST_TIME, true, false, null, false),
        new VariableMetadata(PREFIX_SERVICE_URL, true, false, null, false),

        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER, true, false, null, false),
        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER_VALUES, true, true, null, false),

        new VariableMetadata(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER, true, false, null, false),
        new VariableMetadata(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER_VALUES, true, true, null, false),

        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAM, true, false, null, false),

        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_JMS_MSG_PROP, true, false, null, false),
        new VariableMetadata(BuiltinVariables.PREFIX_RESPONSE_JMS_MSG_PROP, true, false, null, false),

        new VariableMetadata("response.http.status", false, false, null, false, DataType.INTEGER),
        new VariableMetadata(BuiltinVariables.PREFIX_CLUSTER_PROPERTY, true, true, null, false),
        new VariableMetadata("request.username", false, false, null, true),
        new VariableMetadata("request.password", false, false, null, true),
    };

    static {
        for (int i = 0; i < VARS.length; i++) {
            metadataByName.put(VARS[i].getName().toLowerCase(), VARS[i]);
            // builtin variables that are not set at beginning of context
            if (!VARS[i].getName().equals(PREFIX_SERVICE_URL)) { // bugzilla 3208, add other non-preset variables as needed
                metadataPresetByName.put(VARS[i].getName().toLowerCase(), VARS[i]);
            }
        }
    }

    public static VariableMetadata getMetadata(String name) {
        return getMetadata(name, metadataByName);
    }

    private static VariableMetadata getMetadata(String name, Map map) {
        final String lname = name.toLowerCase();
        VariableMetadata var = (VariableMetadata)map.get(lname);
        if (var != null) return var;

        int pos = lname.length();
        do {
            String tryname = lname.substring(0, pos);
            var = (VariableMetadata)map.get(tryname);
            if (var != null) return var;
            pos = lname.lastIndexOf(".", pos-1);
        } while (pos > 0);

        return null;
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
