package com.l7tech.gateway.common.log;

import com.l7tech.common.log.HybridDiagnosticContextKeys;
import static com.l7tech.util.CollectionUtils.list;

import java.util.Collection;

/**
 * HybridDiagnosticContextKeys for the Gateway
 */
public interface GatewayDiagnosticContextKeys extends HybridDiagnosticContextKeys {
    String SERVICE_ID = "service-id";
    String LISTEN_PORT_ID = "listen-port-id";
    String EMAIL_LISTENER_ID = "email-listener-id";
    String USER_ID = "user-id";
    String JMS_LISTENER_ID = "jms-listener-id";
    String POLICY_ID = "policy-id";
    String CLIENT_IP = "client-ip";
    String FOLDER_ID = "folder-id";

    Collection<String> PREFIX_MATCH_PROPERTIES = list( LOGGER_NAME );
}
