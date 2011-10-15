package com.l7tech.common.log;

import static com.l7tech.util.CollectionUtils.list;

import java.util.Collection;

/**
 * TODO [steve] these are server specific, move/rename after development in logging branch is complete
 */
public interface HybridDiagnosticContextKeys {

    public static final String LOGGER_NAME = "logger-name";
    public static final String SERVICE_ID = "service-id";
    public static final String LISTEN_PORT_ID = "listen-port-id";
    public static final String EMAIL_LISTENER_ID = "email-listener-id";
    public static final String USER_ID = "user-id";
    public static final String JMS_LISTENER_ID = "jms-listener-id";
    public static final String POLICY_ID = "policy-id";
    public static final String CLIENT_IP = "client-ip";
    public static final String FOLDER_ID = "folder-id";

    public static final Collection<String> PREFIX_MATCH_PROPERTIES = list( LOGGER_NAME );
}
