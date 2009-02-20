package com.l7tech.server.ems.util;

import com.l7tech.server.ems.EsmMessages;
import com.l7tech.gateway.common.audit.AuditDetailMessage;

/**
 *
 */
public class MessagesUtil {
    static {
        registerWellKnownMessages();
    }

    // Make sure these always get loaded, so the static intializers run (1.5 safe)
    private static void registerWellKnownMessages() {
        new EsmMessages();
    }

    public static AuditDetailMessage getAuditDetailMessageById(int id) {
        return com.l7tech.gateway.common.audit.MessagesUtil.getAuditDetailMessageById(id);
    }
}