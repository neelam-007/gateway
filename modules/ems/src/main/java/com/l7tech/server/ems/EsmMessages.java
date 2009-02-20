package com.l7tech.server.ems;

import com.l7tech.gateway.common.audit.Messages;

import java.util.logging.Level;

/**
 * Message catalog for messages audited by the Enterprise Service Manager.
 * The ID range 100000 - 109999 inclusive is reserved for these messages.
 */
public class EsmMessages extends Messages {

    public static final M NOTIFICATION_STATUS           = m(100001, Level.INFO, "Notification status: {0}");
    public static final M NOTIFICATION_TIME             = m(100002, Level.INFO, "Notification time: {0}");
    public static final M NOTIFICATION_MESSAGE          = m(100003, Level.INFO, "Notification message: {0}");

    // Highest ID reserved for EsmMessages = 99999
}
