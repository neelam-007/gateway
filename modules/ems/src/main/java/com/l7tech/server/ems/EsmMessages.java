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

    public static final M CHANGE_SYSTEM_MONITORING_SETUP_MESSAGE   = m(100004, Level.INFO, "Change system monitoring setup: {0} is changed to {1} {2}");
    public static final M CHANGE_NOTIFICATION_SETUP_MESSAGE        = m(100005, Level.INFO, "Change notification setup: the notification rule {0} is {1}");
    public static final M CHANGE_ENTITY_PROPERTY_SETUP_MESSAGE     = m(100006, Level.INFO, "Change entity property setup: {0} is changed to {1} {2}");
    public static final M CHANGE_ENTITY_NOTIFICATION_SETUP_MESSAGE = m(100007, Level.INFO, "Change entity notification setup: the notification rule {0} is {1}");

    public static final M AUDIT_ALERT_TRIGGERED_MESSAGE   = m(100008, Level.WARNING, "Status from normal to alert message: {0} entered trigger condition.");
    public static final M AUDIT_ALERT_NOT_APPLIED_MESSAGE = m(100009, Level.INFO, "Status from alert to normal message: {0} cleared trigger condition.");

    // Highest ID reserved for EsmMessages = 99999
}
