package com.l7tech.gateway.common.audit;

/**
 *
 */
public class MessagesUtil {
    static {
        registerWellKnownMessages();
    }

    // Make sure these always get loaded, so the static intializers run (1.5 safe)
    private static void registerWellKnownMessages() {
        new CommonMessages();
        new AssertionMessages();
        new BootMessages();
        new MessageProcessingMessages();
        new SystemMessages();
        new ServiceMessages();
        new EsmMessages();
    }

    public static AuditDetailMessage getAuditDetailMessageById(int id) {
        return Messages.messagesById.get(id);
    }
}
