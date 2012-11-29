package com.l7tech.policy.variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Lists built-in system variables and their metadata
 */
public class BuiltinVariables {
    public static final String PREFIX_REQUEST = "request";
    public static final String PREFIX_RESPONSE = "response";

    public static final String PREFIX_REQUEST_HTTP_PARAM = "request.http.parameter";
    public static final String PREFIX_REQUEST_HTTP_PARAMS = "request.http.parameters";

    public static final String PREFIX_REQUEST_JMS_MSG_PROP = "request.jms.property";
    public static final String PREFIX_RESPONSE_JMS_MSG_PROP = "response.jms.property";

    public static final String REQUEST_JMS_MSG_PROP_NAMES = "request.jms.propertynames";
    public static final String RESPONSE_JMS_MSG_PROP_NAMES = "response.jms.propertynames";

    public static final String REQUEST_JMS_MSG_ALL_PROP_VALS = "request.jms.allpropertyvalues";
    public static final String RESPONSE_JMS_MSG_ALL_PROP_VALS = "response.jms.allpropertyvalues";

    public static final String SSGNODE_NAME = "ssgnode.name";
    public static final String SSGNODE_ID = "ssgnode.id";
    public static final String SSGNODE_IP = "ssgnode.ip";
    public static final String SSGNODE_BUILD = "ssgnode.build";

    public static final String PREFIX_GATEWAY_TIME = "gateway.time";
    public static final String PREFIX_REQUEST_TIME = "request.time";
    public static final String PREFIX_SERVICE = "service";
    public static final String PREFIX_POLICY = "policy";
    public static final String PREFIX_ASSERTION = "assertion";

    public static final String PREFIX_REQUEST_URL = "request.url";
    public static final String PREFIX_CLUSTER_PROPERTY = "gateway"; // value of a variable in the cluster property table

    public static final String PREFIX_SECURE_PASSWORD = "secpass"; // used to retrieve a secure password that is marked as variable-interpolatable

    public static final String PREFIX_AUDIT = "audit"; // Only actually available at runtime when running an audit sink policy

    public static final String PREFIX_TRACE = "trace"; // Only actually available at runtime when running a debug trace policy
    public static final String TRACE_OUT = "trace.out";// Only actually available at runtime when running a debug trace policy

    private static final Map<String,VariableMetadata> metadataByName = new HashMap<String,VariableMetadata>();
    private static final Map<String,VariableMetadata> metadataPresetByName = new HashMap<String,VariableMetadata>();

    public static final String TIMESUFFIX_FORMAT_ISO8601 = "iso8601";
    public static final String TIMESUFFIX_ZONE_UTC = "utc";
    public static final String TIMESUFFIX_ZONE_LOCAL = "local";

    public static final String SERVICE_SUFFIX_NAME = "name";
    public static final String SERVICE_SUFFIX_OID = "oid";
    public static final String SERVICE_SUFFIX_ROUTINGURL = "defaultRoutingURL";
    public static final String SERVICE_SUFFIX_POLICY_GUID = "policy.guid";
    public static final String SERVICE_SUFFIX_POLICY_VERSION = "policy.version";
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

    public static final String POLICY_SUFFIX_NAME = SERVICE_SUFFIX_NAME;
    public static final String POLICY_SUFFIX_OID = SERVICE_SUFFIX_OID;
    public static final String POLICY_SUFFIX_GUID = "guid";
    public static final String POLICY_SUFFIX_VERSION = "version";

    public static final String ASSERTION_SUFFIX_NUMBER = "number";
    public static final String ASSERTION_SUFFIX_NUMBERSTR = "numberstr";

    public static final String ASSERTION_LATENCY = PREFIX_ASSERTION + ".latency";
    public static final String ASSERTION_LATENCY_MS = ASSERTION_LATENCY + ".ms";
    public static final String ASSERTION_LATENCY_NS = ASSERTION_LATENCY + ".ns";

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

    public static boolean isDeprecated(String name) {
        VariableMetadata meta = getMetadata(name);
        return meta != null && meta.isDeprecated();
    }

    /**
     * Get the unmatched name, including any leading ".", "|", etc
     *
     * @param name The variable name
     * @return The unmatched part of the name (which is the whole name if not a built-in variable)
     */
    public static String getUnmatchedName(final String name) {
        String unmatched = name;
        String newname = Syntax.getMatchingName(name, metadataByName.keySet());
        if (newname != null) {
            unmatched = name.substring(newname.length());
        }
        return unmatched;
    }

    public static Map getAllMetadata() {
        return Collections.unmodifiableMap(metadataByName);
    }

    @SuppressWarnings({ "deprecation" })
    private static final VariableMetadata[] VARS = {
            new VariableMetadata("request.clientid", false, false, null, false),
            new VariableMetadata("request.http.method", false, false, null, false),
            new VariableMetadata("request.http.uri", false, false, null, false),
            new VariableMetadata(PREFIX_REQUEST_URL, true, false, null, false),
            new VariableMetadata("request.http.secure", false, false, null, false, DataType.BOOLEAN),
            new VariableMetadata("request.http.queryString", false, false, null, false),
            new VariableMetadata("request.ftp.path", false, false, null, false),
            new VariableMetadata("request.ftp.file", false, false, null, false),
            new VariableMetadata("request.ftp.unique", false, false, null, false),
            new VariableMetadata("request.ftp.secure", false, false, null, false),
            new VariableMetadata("request.ssh.path", false, false, null, false),
            new VariableMetadata("request.ssh.file", false, false, null, false),
            new VariableMetadata("auditLevel", false, false, null, true),
            new VariableMetadata("request.soap.operation", false, false, null, false),
            new VariableMetadata("request.soap.operationname", false, false, "request.soap.operation", false),
            new VariableMetadata("request.soap.namespace", false, false, null, false),
            new VariableMetadata("request.soap.urn", false, false, "request.soap.namespace", false),
            new VariableMetadata("request.soap.envelopens", false, false, null, false, DataType.STRING),
            new VariableMetadata("request.soap.version", false, false, null, false, DataType.STRING),
            new VariableMetadata("requestId", false, false, null, false),
            new VariableMetadata("routingStatus", false, false, null, false),
            new VariableMetadata("request.elapsedTime", false, false, null, false, DataType.INTEGER),
            new VariableMetadata(PREFIX_GATEWAY_TIME, true, false, null, false),
            new VariableMetadata(PREFIX_REQUEST_TIME, true, false, null, false),
            new VariableMetadata(PREFIX_SERVICE + "." + SERVICE_SUFFIX_URL, true, false, null, false),
            //service.name and service.oid have no suffixes => have no need to be stored as a variable
            //which can have suffixes attached to it, unlike service.url which can have service.url.host etc...
            new VariableMetadata(PREFIX_SERVICE + "." + SERVICE_SUFFIX_NAME, false, false, null, false),
            new VariableMetadata(PREFIX_SERVICE + "." + SERVICE_SUFFIX_OID, false, false, null, false),
            new VariableMetadata(PREFIX_SERVICE + "." + SERVICE_SUFFIX_ROUTINGURL, false, false, null, false),
            new VariableMetadata(PREFIX_SERVICE + "." + SERVICE_SUFFIX_POLICY_GUID, false, false, null, false),
            new VariableMetadata(PREFIX_SERVICE + "." + SERVICE_SUFFIX_POLICY_VERSION, false, false, null, false, DataType.INTEGER),

            new VariableMetadata(PREFIX_POLICY + "." + POLICY_SUFFIX_NAME, false, false, null, false),
            new VariableMetadata(PREFIX_POLICY + "." + POLICY_SUFFIX_OID, false, false, null, false),
            new VariableMetadata(PREFIX_POLICY + "." + POLICY_SUFFIX_GUID, false, false, null, false),
            new VariableMetadata(PREFIX_POLICY + "." + POLICY_SUFFIX_VERSION, false, false, null, false, DataType.INTEGER),

            new VariableMetadata(PREFIX_ASSERTION + "." + ASSERTION_SUFFIX_NUMBER, false, true, null, false, DataType.INTEGER),
            new VariableMetadata(PREFIX_ASSERTION + "." + ASSERTION_SUFFIX_NUMBERSTR, false, false, null, false),
            new VariableMetadata(ASSERTION_LATENCY, true, false, null, false),
            new VariableMetadata(ASSERTION_LATENCY_MS, false, false, null, false, DataType.INTEGER),
            new VariableMetadata(ASSERTION_LATENCY_NS, false, false, null, false, DataType.INTEGER),

            new VariableMetadata(SSGNODE_NAME, false, false, null, false),
            new VariableMetadata(SSGNODE_ID, false, false, null, false),
            new VariableMetadata(SSGNODE_IP, false, false, null, false),
            new VariableMetadata(SSGNODE_BUILD, true, false, null, false, DataType.UNKNOWN),

            new VariableMetadata(BuiltinVariables.PREFIX_REQUEST, true, false, null, true, DataType.MESSAGE),
            new VariableMetadata(BuiltinVariables.PREFIX_RESPONSE, true, false, null, true, DataType.MESSAGE),

            new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAMS, true, true, null, false),
            new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAM, true, false, null, false),

            new VariableMetadata(BuiltinVariables.PREFIX_REQUEST_JMS_MSG_PROP, true, false, null, false),
            new VariableMetadata(BuiltinVariables.PREFIX_RESPONSE_JMS_MSG_PROP, true, false, null, false),

            new VariableMetadata(BuiltinVariables.REQUEST_JMS_MSG_PROP_NAMES, true, false, null, false),
            new VariableMetadata(BuiltinVariables.RESPONSE_JMS_MSG_PROP_NAMES, true, false, null, false),

            new VariableMetadata("response.http.status", false, false, null, false, DataType.INTEGER),
            new VariableMetadata(BuiltinVariables.PREFIX_CLUSTER_PROPERTY, true, true, null, false),

            new VariableMetadata("request.ssl.clientcertificate", true, false, null, false, DataType.CERTIFICATE),
            new VariableMetadata("request.wss.signingcertificate", true, false, null, false, DataType.CERTIFICATE, "request.wss.signingcertificates.value.1"),

            new VariableMetadata("request.http", true, false, null, false),
            new VariableMetadata("request.mainpart", false, false, null, false),
            new VariableMetadata("request.parts", true, true, null, false),
            new VariableMetadata("request.wss", true, false, null, false),
            new VariableMetadata("request.username", false, false, null, false),
            new VariableMetadata("request.password", false, false, null, false),
            new VariableMetadata("request.authenticateduser", true, false, null, false, DataType.UNKNOWN),
            new VariableMetadata("request.authenticatedusers", true, true, null, false, DataType.UNKNOWN),
            new VariableMetadata("request.authenticateddn", true, false, null, false),
            new VariableMetadata("request.authenticateddns", true, false, null, false),

            new VariableMetadata("response.http", true, false, null, false),
            new VariableMetadata("response.mainpart", false, false, null, false),
            new VariableMetadata("response.parts", true, true, null, false),
            new VariableMetadata("response.wss", true, false, null, false),
            new VariableMetadata("response.password", false, false, null, false),
            new VariableMetadata("response.username", false, false, null, false),
            new VariableMetadata("response.authenticateduser", true, false, null, false),
            new VariableMetadata("response.authenticatedusers", true, false, null, false),
            new VariableMetadata("response.authenticateddn", true, false, null, false),
            new VariableMetadata("response.authenticateddns", true, false, null, false),
            new VariableMetadata("response.soap", true, false, null, false),

            new VariableMetadata("request.tcp.remoteaddress", true, false, null, false),
            new VariableMetadata("request.tcp.remoteip", true, false, null, false),
            new VariableMetadata("request.tcp.remotehost", true, false, null, false),
            new VariableMetadata("request.tcp.remoteport", true, false, null, false),
            new VariableMetadata("request.tcp.localaddress", true, false, null, false),
            new VariableMetadata("request.tcp.localip", true, false, null, false),
            new VariableMetadata("request.tcp.localhost", true, false, null, false),
            new VariableMetadata("request.tcp.localport", true, false, null, false),

            new VariableMetadata("request.compression.gzip.found", false, false, null, true, DataType.BOOLEAN),

            new VariableMetadata(PREFIX_SECURE_PASSWORD, true, false, null, false),

            new VariableMetadata(PREFIX_AUDIT, true, false, null, false),

            new VariableMetadata(PREFIX_TRACE, true, false, null, false),
            new VariableMetadata(TRACE_OUT, false, false, TRACE_OUT, true),
    };

    static {
        for ( final VariableMetadata variableMetadata : VARS ) {
            metadataByName.put( variableMetadata.getName().toLowerCase(), variableMetadata );
            // builtin variables that are not set at beginning of context
            //noinspection deprecation
            if ( !variableMetadata.getName().equals( PREFIX_SERVICE + "." + SERVICE_SUFFIX_URL ) ) { // bugzilla 3208, add other non-preset variables as needed
                metadataPresetByName.put( variableMetadata.getName().toLowerCase(), variableMetadata );
            }
        }
    }

    /**
     * Get meta data for the variable.
     *
     * @param name Should not include any variable syntax e.g. ${}.
     * @return meta data if found, null otherwise
     */
    public static VariableMetadata getMetadata(String name) {
        String newname = Syntax.getMatchingName(name, metadataByName.keySet());
        if (newname == null) return null;
        return metadataByName.get(newname);
    }
}
