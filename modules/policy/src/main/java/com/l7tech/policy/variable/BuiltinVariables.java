/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
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
    public static final String PREFIX_SERVICE = "service";

    public static final String PREFIX_REQUEST_URL = "request.url";
    public static final String PREFIX_CLUSTER_PROPERTY = "gateway"; // value of a variable in the cluster property table

    public static final String PREFIX_AUTHENTICATED_USER = "request.authenticateduser";
    public static final String PREFIX_AUTHENTICATED_USERS = "request.authenticatedusers";

    private static final Map metadataByName = new HashMap();
    private static final Map metadataPresetByName = new HashMap();

    public static final String TIMESUFFIX_FORMAT_ISO8601 = "iso8601";
    public static final String TIMESUFFIX_ZONE_UTC = "utc";
    public static final String TIMESUFFIX_ZONE_LOCAL = "local";

    public static final String SERVICE_SUFFIX_NAME = "name";
    public static final String SERVICE_SUFFIX_OID = "oid";

    @Deprecated
    public static final String SERVICE_SUFFIX_URL = "url";
    @Deprecated
    public static final String SERVICE_SUFFIX_HOST = "host";
    @Deprecated
    public static final String SERVICE_SUFFIX_PROTOCOL = "protocol";
    @Deprecated
    public static final String SERVICE_SUFFIX_PORT = "port";
    @Deprecated
    public static final String SERVICE_SUFFIX_FILE = "file";
    @Deprecated
    public static final String SERVICE_SUFFIX_PATH = "path";
    @Deprecated
    public static final String SERVICE_SUFFIX_QUERY = "query";
    @Deprecated
    public static final String SERVICE_SUFFIX_FRAGMENT = "fragment";

    public static boolean isSupported(String name) {
        return getMetadata(name) != null;
    }

    public static boolean isPredefined(String name) {
        return Syntax.getMatchingName(name, metadataPresetByName.keySet()) != null;
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
        new VariableMetadata(PREFIX_AUTHENTICATED_USERS, false, true, null, false),
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
        new VariableMetadata(PREFIX_SERVICE+"."+SERVICE_SUFFIX_URL, true, false, null, false),
        //service.name and service.oid have no suffixes => have no need to be stored as a variable
        //which can have suffixes attached to it, unlike service.url which can have service.url.host etc...
        new VariableMetadata(PREFIX_SERVICE+"."+SERVICE_SUFFIX_NAME, false, false, null, false),
        new VariableMetadata(PREFIX_SERVICE+"."+SERVICE_SUFFIX_OID, false, false, null, false),

        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER, true, false, null, false),
        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER_VALUES, true, true, null, false),

        new VariableMetadata(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER, true, false, null, false),
        new VariableMetadata(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER_VALUES, true, true, null, false),

        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAM, true, false, null, false),

        new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_JMS_MSG_PROP, true, false, null, false),
        new VariableMetadata(BuiltinVariables.PREFIX_RESPONSE_JMS_MSG_PROP, true, false, null, false),

        new VariableMetadata("response.http.status", false, false, null, false, DataType.INTEGER),
        new VariableMetadata(BuiltinVariables.PREFIX_CLUSTER_PROPERTY, true, true, null, false),
        new VariableMetadata("request.username", false, false, null, false),
        new VariableMetadata("request.password", false, false, null, false),

        new VariableMetadata("request.ssl.clientcertificate", false, false, null, false, DataType.CERTIFICATE),
        new VariableMetadata("request.ssl.clientcertificate.base64", false, false, null, false, DataType.STRING),
        new VariableMetadata("request.ssl.clientcertificate.pem", false, false, null, false, DataType.STRING),

        new VariableMetadata("request.wss.signingcertificate", false, false, null, false, DataType.CERTIFICATE),
        new VariableMetadata("request.wss.signingcertificate.base64", false, false, null, false, DataType.STRING),
        new VariableMetadata("request.wss.signingcertificate.pem", false, false, null, false, DataType.STRING),
    };

    static {
        for (int i = 0; i < VARS.length; i++) {
            metadataByName.put(VARS[i].getName().toLowerCase(), VARS[i]);
            // builtin variables that are not set at beginning of context
            if (!VARS[i].getName().equals(PREFIX_SERVICE+"."+SERVICE_SUFFIX_URL)) { // bugzilla 3208, add other non-preset variables as needed
                metadataPresetByName.put(VARS[i].getName().toLowerCase(), VARS[i]);
            }
        }
    }

    public static VariableMetadata getMetadata(String name) {
        String newname = Syntax.getMatchingName(name, metadataByName.keySet());
        if (newname == null) return null;
        return (VariableMetadata) metadataByName.get(newname);
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
