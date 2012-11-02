package com.l7tech.gateway.common.audit;

/**
 */
public class AuditRecordUtils {
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_SYSTEM = "system";
    public static final String TYPE_ADMIN = "admin";

    public static String AuditRecordType(AuditRecord record){
        if(record instanceof AdminAuditRecord)
            return TYPE_ADMIN;
        else if (record instanceof MessageSummaryAuditRecord)
            return TYPE_MESSAGE;
        else if (record instanceof SystemAuditRecord)
            return TYPE_SYSTEM;
        return "";
    }
}
