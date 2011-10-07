package com.l7tech.common.log;

/**
 * TODO [steve] these are probably server specific
 */
public interface HybridDiagnosticContextKeys {

    public static final String LOGGER_NAME = "logger-name"; //TODO [steve] match by logger name prefix?
    public static final String SERVICE_ID = "service-id";
    public static final String LISTEN_PORT_ID = "listen-port-id";
    public static final String EMAIL_LISTENER_ID = "email-listener-id";
    public static final String USER_ID = "user-id";
    public static final String JMS_LISTENER_ID = "jms-listener-id";
    public static final String POLICY_ID = "policy-id";
    public static final String CLIENT_IP = "client-ip"; //TODO [steve] match by IP pattern?

}
