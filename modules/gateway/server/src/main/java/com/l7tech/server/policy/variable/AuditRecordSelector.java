package com.l7tech.server.policy.variable;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.util.CompressedStringType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.MarshalException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class AuditRecordSelector implements ExpandVariables.Selector<AuditRecord>{
    private static final Logger logger = Logger.getLogger(AuditRecordSelector.class.getName());

    @Override
    public Selection select(String contextName, AuditRecord auditRecord, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
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

        if (auditRecord instanceof SystemAuditRecord) {
            SystemAuditRecord adminRec = (SystemAuditRecord) auditRecord;
            FieldGetter<SystemAuditRecord> systemFieldFinder = systemFields.get(name);
            if (systemFieldFinder != null) {
                return systemFieldFinder.getFieldValue(adminRec, name);
            }
        }

        if (name.toLowerCase().startsWith("details.")) {
            if (name.length() < "details.".length() + 1)
                return null;
            AuditDetail[] details = auditRecord.getDetailsInOrder();
            if (details == null || details.length == 0)
                // return emmty array of details
                return new Selection(new AuditDetail[0]);
            return selectDetails( name, details, logger );
        }
        
        if(allAvaliableFields.contains(name))
            return new Selection(null);

        return null;
    }

    @Override
    public Class<AuditRecord> getContextObjectClass() {
        return AuditRecord.class;
    }

    static Selection selectDetails( String name,
                                    final AuditDetail[] details,
                                    final Logger logger ) {
        name = name.substring("details.".length());
        int dot = name.indexOf('.');
        int index;
        String remainingName;
        try {
            if (dot == -1) {
                index = Integer.parseInt(name);
                remainingName = null;
            } else {
                index = Integer.parseInt(name.substring(0, dot));
                remainingName = name.length() > dot ? name.substring(dot + 1) : null;
            }
            return new Selection(details[index], remainingName);
        } catch (NumberFormatException nfe) {
            logger.warning("Invalid numeric index for audit detail lookup");
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.fine("Index out of bounds for audit detail lookup");
            return null;
        }
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
                    type = AuditRecordUtils.TYPE_MESSAGE;
                } else if (auditRecord instanceof SystemAuditRecord) {
                    type = AuditRecordUtils.TYPE_SYSTEM;
                } else if (auditRecord instanceof AdminAuditRecord) {
                    type = AuditRecordUtils.TYPE_ADMIN;
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
                String name = auditRecord.getName();
                return new Selection(name == null ? "" : name);
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
                String reqId = auditRecord.getStrRequestId();
                return new Selection( reqId == null? "" : reqId);
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
                String userName = auditRecord.getUserName();
                return new Selection(userName==null?"":userName);
            }
        });

        baseFields.put("user.id", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord auditRecord, String baseAndRemainingName) {
                String userId = auditRecord.getUserId();
                return new Selection(userId==null?"":userId);
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
                // Exceptions were never used with audit records, the "thrown"
                // variable is left in place in case of any backwards
                // compatibility issues. It will be removed from the
                // documentation as of 6.2 / Escolar.
                return new Selection(null);
            }
        });

        baseFields.put("details", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getDetailsInOrder());
            }
        });

        //
        // System audit records only have a couple of fields, so we'll just handle them here with the base fields
        //

        baseFields.put("action", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                if (rec instanceof SystemAuditRecord) {
                    SystemAuditRecord sysrec = (SystemAuditRecord) rec;
                    return new Selection(sysrec.getAction());
                } else if (rec instanceof AdminAuditRecord) {
                    AdminAuditRecord adminrec = (AdminAuditRecord) rec;
                    return new Selection(String.valueOf(adminrec.getAction()));
                }
                else  return new Selection("");
            }
        });

        baseFields.put("component", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                if (rec instanceof SystemAuditRecord) {
                    SystemAuditRecord sysrec = (SystemAuditRecord) rec;
                    final Component component = Component.fromId(sysrec.getComponentId());
                    return new Selection(component == null ? null : component.getName());
                }
                else  return new Selection(null);
            }
        });

        baseFields.put("properties", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                return new Selection(getProperties(rec));
            }
        });

        baseFields.put("signature", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getSignature());
            }
        });

        baseFields.put("resZip", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                if (rec instanceof MessageSummaryAuditRecord) {
                    MessageSummaryAuditRecord msgrec = (MessageSummaryAuditRecord) rec;
                    return new Selection(msgrec.getResponseXml()==null? new  byte[0]:getCompressedString(msgrec.getResponseXml()));
                }
                else  return new Selection(new  byte[0]);
            }
        });

        baseFields.put("reqZip", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                if (rec instanceof MessageSummaryAuditRecord) {
                    MessageSummaryAuditRecord msgrec = (MessageSummaryAuditRecord) rec;
                    return new Selection(msgrec.getRequestXml()==null? new  byte[0]:getCompressedString(msgrec.getRequestXml()));
                }
                else  return new Selection(new  byte[0]);
            }
        });
        baseFields.put("entity.class", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                if (rec instanceof AdminAuditRecord) {
                    AdminAuditRecord adminRec = (AdminAuditRecord) rec;
                    String className = adminRec.getEntityClassname();
                    return new Selection(className==null?"":className);
                }
                else  return new Selection("");
            }
        });
        baseFields.put("entity.oid", new FieldGetter<AuditRecord>() {
            @Override
            public Selection getFieldValue(AuditRecord rec, String baseAndRemainingName) {
                if (rec instanceof AdminAuditRecord) {
                    AdminAuditRecord adminRec = (AdminAuditRecord) rec;
                    return new Selection(adminRec.getEntityOid());
                }
                else  return new Selection("");
            }
        });
    }

    private static String getProperties(AuditRecord rec) {
        Document doc = XmlUtil.createEmptyDocument();
        try {
            Element element = recordMarshaller.marshal(doc, rec);
            doc.appendChild(element);
            return XmlUtil.nodeToFormattedString(element);
        } catch (MarshalException e) {
            logger.warning("Unable to marshall properties");
        } catch (IOException e) {
            logger.warning("Unable to marshall properties");
        }
        return null;
    }

    static Map<String, FieldGetter<SystemAuditRecord>> systemFields = new TreeMap<String, FieldGetter<SystemAuditRecord>>(String.CASE_INSENSITIVE_ORDER);
    static {
        systemFields.put("componentId", new FieldGetter<SystemAuditRecord>() {
            @Override
            public Selection getFieldValue(SystemAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getComponentId());
            }
        });

        systemFields.put("action", new FieldGetter<SystemAuditRecord>() {
            @Override
            public Selection getFieldValue(SystemAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getAction());
            }
        });
    }

    private static final AuditRecordPropertiesDomMarshaller recordMarshaller = new AuditRecordPropertiesDomMarshaller();
    static Map<String, FieldGetter<AdminAuditRecord>> adminFields = new TreeMap<String, FieldGetter<AdminAuditRecord>>(String.CASE_INSENSITIVE_ORDER);
    static {
        adminFields.put("action", new FieldGetter<AdminAuditRecord>() {
            @Override
            public Selection getFieldValue(AdminAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getAction());
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
                return new Selection(rec.getServiceGoid());
            }
        });

        messageFields.put("status", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getStatus());
            }
        });

        messageFields.put("authenticated", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.isAuthenticated()?1:0);
            }
        });

        messageFields.put("responseStatus", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getResponseHttpStatus());
            }
        });

        messageFields.put("savedResponseContentLength", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getResponseContentLength());
            }
        });

        messageFields.put("savedRequestContentLength", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getRequestContentLength());
            }
        });

        messageFields.put("resZip", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getResponseXml()==null? new  byte[0]:getCompressedString(rec.getResponseXml()));
            }
        });

        messageFields.put("reqZip", new FieldGetter<MessageSummaryAuditRecord>() {
            @Override
            public Selection getFieldValue(MessageSummaryAuditRecord rec, String baseAndRemainingName) {
                return new Selection(rec.getRequestXml()==null? new  byte[0] :getCompressedString(rec.getRequestXml()));
            }
        });

    }
    
    static List<String> allAvaliableFields = new ArrayList<String>();
    static{
        allAvaliableFields.add("componentId");
        allAvaliableFields.add("action");
        allAvaliableFields.add("entity.class");
        allAvaliableFields.add("entity.oid");
        allAvaliableFields.add("authType");
        allAvaliableFields.add("mappingValuesOid");
        allAvaliableFields.add("operationName");
        allAvaliableFields.add("requestSavedFlag");
        allAvaliableFields.add("responseSavedFlag");
        allAvaliableFields.add("routingLatency");
        allAvaliableFields.add("savedRequestContentLength");
        allAvaliableFields.add("savedResponseContentLength");
        allAvaliableFields.add("serviceOid");
        allAvaliableFields.add("status");
        allAvaliableFields.add("authenticated");
        allAvaliableFields.add("responseStatus");
        allAvaliableFields.add("resZip");
        allAvaliableFields.add("reqZip");
        allAvaliableFields.add("componentId");
        allAvaliableFields.add("action");

    }
    private static byte[] getCompressedString(String in){
        try {
            return CompressedStringType.compress(in);
        } catch (SQLException e) {
            return null;
        }
    }
}
