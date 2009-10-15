package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.ExceptionUtils;

import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class AuditRecordSelector implements ExpandVariables.Selector<AuditRecord>{
    private static final Logger logger = Logger.getLogger(AuditRecordSelector.class.getName());

    @Override
    public Selection select(AuditRecord auditRecord, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if (auditRecord == null) {
            // Probably can't happen
            logger.warning("AuditRecordSelector: audit record is null");
            return null;
        }

        // Check for a simple field
        FieldGetter<AuditRecord> finder = baseFields.get(name);
        if (finder != null) {
            return finder.getFieldValue(auditRecord, name);
        }

        if (auditRecord instanceof MessageSummaryAuditRecord) {
            MessageSummaryAuditRecord messageRec = (MessageSummaryAuditRecord) auditRecord;
            FieldGetter<MessageSummaryAuditRecord> messageFieldFinder = messageFields.get(name);
            if (messageFieldFinder != null) {
                return messageFieldFinder.getFieldValue(messageRec, name);
            }
        }

        if (auditRecord instanceof AdminAuditRecord) {
            AdminAuditRecord adminRec = (AdminAuditRecord) auditRecord;
            FieldGetter<AdminAuditRecord> adminFieldFinder = adminFields.get(name);
            if (adminFieldFinder != null) {
                return adminFieldFinder.getFieldValue(adminRec, name);
            }
        }

        // TODO handle audit.details.*

        return null;
    }

    @Override
    public Class<AuditRecord> getContextObjectClass() {
        return AuditRecord.class;
    }

    static interface FieldGetter<T extends AuditRecord> {
        // We'll pass in the entire name the base selector got, because stripping the prefix isn't free, and the common case is for our fields to ignore the remainingName completely
        Selection getFieldValue(T rec, String baseAndRemainingName);
    }

    static Map<String, FieldGetter<AuditRecord>> baseFields = new TreeMap<String, FieldGetter<AuditRecord>>(String.CASE_INSENSITIVE_ORDER);
    static {
        baseFields.put("type", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                String type;
                if (auditRecord instanceof MessageSummaryAuditRecord) {
                    type = "message";
                } else if (auditRecord instanceof SystemAuditRecord) {
                    type = "system";
                } else if (auditRecord instanceof AdminAuditRecord) {
                    type = "admin";
                } else {
                    type = "unknown";
                }
                return new Selection(type);
            }
        });

        baseFields.put("id", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getId());
            }
        });

        baseFields.put("level", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                Level level = auditRecord.getLevel();
                return new Selection(level == null ? "" : String.valueOf(level.intValue()));
            }
        });

        baseFields.put("levelStr", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(String.valueOf(auditRecord.getLevel()));
            }
        });

        baseFields.put("name", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getName());
            }
        });

        baseFields.put("sequenceNumber", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getSequenceNumber());
            }
        });

        baseFields.put("nodeId", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getNodeId());
            }
        });

        baseFields.put("requestId", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getStrRequestId());
            }
        });

        baseFields.put("time", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getMillis());
            }
        });

        baseFields.put("message", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getMessage());
            }
        });

        baseFields.put("ipAddress", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getIpAddress());
            }
        });

        baseFields.put("user.name", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getUserName());
            }
        });

        baseFields.put("user.id", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getUserId());
            }
        });

        baseFields.put("user.idProv", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                return new Selection(auditRecord.getIdentityProviderOid());
            }
        });

        baseFields.put("thrown", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                final Throwable thrown = rec.getThrown();
                return new Selection(thrown == null ? null : ExceptionUtils.getStackTraceAsString(thrown));
            }
        });

        baseFields.put("details", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                final Set<AuditDetail> details = rec.getDetails();
                return details == null ? null : new Selection(details.toArray(new AuditDetail[details.size()]));
            }
        });
    }

    static Map<String, FieldGetter<AdminAuditRecord>> adminFields = new TreeMap<String, FieldGetter<AdminAuditRecord>>(String.CASE_INSENSITIVE_ORDER);
    static {
        adminFields.put("entity.class", new FieldGetter<AdminAuditRecord>() {
            @Override
            public Selection getFieldValue(AdminAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getEntityClassname());
            }
        });

        adminFields.put("entity.oid", new FieldGetter<AdminAuditRecord>() {
            @Override
            public Selection getFieldValue(AdminAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getEntityOid());
            }
        });
    }

    static Map<String, FieldGetter<MessageSummaryAuditRecord>> messageFields = new TreeMap<String, FieldGetter<MessageSummaryAuditRecord>>(String.CASE_INSENSITIVE_ORDER);
    static {
        messageFields.put("authType", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord auditRecord, String baseAndRemainingName) {
                final SecurityTokenType type = auditRecord.getAuthenticationType();
                return new Selection(type == null ? null : type.toString());
            }
        });

        messageFields.put("mappingValuesOid", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getMappingValuesOid());
            }
        });

        messageFields.put("operationName", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getOperationName());
            }
        });

        messageFields.put("requestSavedFlag", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getRequestXml() != null);
            }
        });

        messageFields.put("responseSavedFlag", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getResponseXml() != null);
            }
        });

        messageFields.put("routingLatency", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getRoutingLatency());
            }
        });

        messageFields.put("serviceOid", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getServiceOid());
            }
        });

        messageFields.put("status", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getStatus());
            }
        });
    }
}
