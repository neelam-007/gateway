package com.l7tech.gateway.common.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.token.SecurityTokenType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.bind.MarshalException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Hack for write-only marshalling of an AuditRecord to a DOM element.  Can be replaced with a more standard
 * implementation later.
 */
public class AuditRecordDomMarshaller {
    private static final String NS = "http://l7tech.com/audit/rec";

    /**
     * Add the specified AuditRecord as a DOM element as a child of the specified parent element.
     *
     * @param factory  DOM factory to use.  Required.
     * @param auditRecord the audit record to marshall.  Required.
     * @return the element that was created.
     */
    public Element marshal(Document factory, AuditRecord auditRecord) throws MarshalException {
        if (auditRecord instanceof AdminAuditRecord) {
            AdminAuditRecord adminAuditRecord = (AdminAuditRecord) auditRecord;
            return marshallAdmin(factory, adminAuditRecord);
        } else if (auditRecord instanceof SystemAuditRecord) {
            SystemAuditRecord systemAuditRecord = (SystemAuditRecord) auditRecord;
            return marshallSystem(factory, systemAuditRecord);
        } else if (auditRecord instanceof MessageSummaryAuditRecord) {
            MessageSummaryAuditRecord messageSummaryAuditRecord = (MessageSummaryAuditRecord) auditRecord;
            return marshallMessage(factory, messageSummaryAuditRecord);
        } else
            throw new MarshalException("Unsupported audit record type");
    }

    private Element createElement(Document factory, String name) {
        Element e = factory.createElementNS(NS, name);
        e.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns", NS);
        return e;
    }

    private Element elm(Element parent, String name, String contents) {
        Document factory = parent.getOwnerDocument();
        Element e = factory.createElementNS(NS, name);
        Text text = factory.createTextNode(contents==null?"":contents);
        e.appendChild(text);
        parent.appendChild(e);
        return e;
    }

    private void addAuditRecordFields(Element e, AuditRecord rec) {
        if (rec == null)
            return;

        String id = rec.getGoid()!=null?rec.getId():null;
        String ipAddress = rec.getIpAddress();
        String message = rec.getMessage();
        long millis = rec.getMillis();
        String nodeId = rec.getNodeId();
        Level level = rec.getLevel();
        String levelStr = rec.getStrLvl();
        String requestId = rec.getStrRequestId();
        long sequenceNumber = rec.getSequenceNumber();

        if (id!=null)
            e.setAttribute("id", id);
        e.setAttribute("name", rec.getName());
        e.setAttribute("sequenceNumber", Long.toString(sequenceNumber));
        e.setAttribute("level", Integer.toString(level.intValue()));
        e.setAttribute("levelStr", levelStr);

        elm(e, "nodeId", nodeId);
        if (requestId != null && requestId.length() > 0)
            elm(e, "requestId", requestId);
        elm(e, "time", Long.toString(millis));
        elm(e, "message", message);
        elm(e, "ipAddress", ipAddress);

        if (rec.getUserId() != null || rec.getUserName() != null) {
            Element user = e.getOwnerDocument().createElementNS(NS, "user");
            if(rec.getIdentityProviderGoid()!=null){
                user.setAttribute("identityProviderGoid", Goid.toString(rec.getIdentityProviderGoid()));
            }
            user.setAttribute("id", rec.getUserId());
            user.setAttribute("name", rec.getUserName());
            e.appendChild(user);
        }

        final List<AuditDetail> details =
                new ArrayList<AuditDetail>(rec.getDetails()==null?Collections.<AuditDetail>emptySet():rec.getDetails());
        if (!details.isEmpty()) {
            Collections.sort( details );
            Element d = e.getOwnerDocument().createElementNS(NS, "details");
            for (AuditDetail detail : details) {
                addDetailRecordFields(d, detail);
            }
            e.appendChild(d);
        }
    }

    private void addDetailRecordFields(Element parent, AuditDetail detail) {
        if (detail == null)
            return;

        int componentId = detail.getComponentId();
        int messageId = detail.getMessageId();
        int ordinal = detail.getOrdinal();
        String exception = detail.getException();
        long time = detail.getTime();

        Element d = parent.getOwnerDocument().createElementNS(NS, "detail");
        d.setAttribute("componentId", Integer.toString(componentId));
        d.setAttribute("messageId", Integer.toString(messageId));
        d.setAttribute("ordinal", Integer.toString(ordinal));
        d.setAttribute("time", Long.toString(time));

        String[] params = detail.getParams();
        if (params != null && params.length > 0) {
            Element paramsEl = parent.getOwnerDocument().createElementNS(NS, "params");
            for (String param : params) {
                elm(paramsEl, "param", param);
            }
            d.appendChild(paramsEl);
        }

        if (exception != null) {
            elm(d, "exception", exception);
        }

        parent.appendChild(d);
    }

    private void addMessageContextMappingValuesFields(Element mv, MessageContextMappingValues mappingValuesEntity) {
        mv.setAttribute("digest", mappingValuesEntity.getDigested());
        elm(mv, "authUserId", mappingValuesEntity.getAuthUserId());
        elm(mv, "authUserUniqueId", mappingValuesEntity.getAuthUserUniqueId());
        elm(mv, "createTime", Long.toString(mappingValuesEntity.getCreateTime()));
        elm(mv, "serviceOperation", mappingValuesEntity.getServiceOperation());
        elm(mv, "mapping1value", mappingValuesEntity.getMapping1_value());
        elm(mv, "mapping2value", mappingValuesEntity.getMapping2_value());
        elm(mv, "mapping3value", mappingValuesEntity.getMapping3_value());
        elm(mv, "mapping4value", mappingValuesEntity.getMapping4_value());
        elm(mv, "mapping5value", mappingValuesEntity.getMapping5_value());
    }

    private Element marshallMessage(Document factory, MessageSummaryAuditRecord rec) {
        Element e = createElement(factory, "audit");
        e.setAttribute("type", "message");
        addAuditRecordFields(e, rec);
        SecurityTokenType authenticationType = rec.getAuthenticationType();
        if (authenticationType != null)
            elm(e, "authType", authenticationType.getName());
        MessageContextMappingValues mappingValuesEntity = rec.getMappingValuesEntity();
        if (mappingValuesEntity != null) {
            Element mv = factory.createElementNS(NS, "messageContextMappingValues");
            addMessageContextMappingValuesFields(mv, mappingValuesEntity);
            e.appendChild(mv);
        }
        Goid mappingValuesOid = rec.getMappingValuesId();
        if (mappingValuesOid != null) {
            elm(e, "mappingValuesOid", mappingValuesOid.toString());
        }
        elm(e, "operationName", rec.getOperationName());
        elm(e, "requestContentLength", Integer.toString(rec.getRequestContentLength()));
        elm(e, "requestSavedFlag", rec.getRequestXml() != null ? "true" : "false");
        elm(e, "responseContentLength", Integer.toString(rec.getResponseContentLength()));
        elm(e, "responseSavedFlag", rec.getResponseXml() != null ? "true" : "false");
        elm(e, "responseHttpStatus", Integer.toString(rec.getResponseHttpStatus()));
        elm(e, "routingLatency", Integer.toString(rec.getRoutingLatency()));
        //Leave this as serviceOid as other tooling may expect this to be serviceOid
        if(rec.getServiceGoid()!=null) {
            elm(e, "serviceOid", Goid.toString(rec.getServiceGoid()));
        }
        elm(e, "status", Integer.toString(rec.getStatus()));

        return e;
    }

    private Element marshallSystem(Document factory, SystemAuditRecord rec) {
        Element e = createElement(factory, "audit");
        e.setAttribute("type", "system");
        addAuditRecordFields(e, rec);
        e.setAttribute("action", rec.getAction());
        e.setAttribute("componentId", Integer.toString(rec.getComponentId()));
        return e;
    }

    private Element marshallAdmin(Document factory, AdminAuditRecord rec) {
        Element e = createElement(factory, "audit");
        e.setAttribute("type", "admin");
        addAuditRecordFields(e, rec);

        char action = rec.getAction();
        e.setAttribute("action", Character.toString(action));

        Element ent = factory.createElementNS(NS, "entity");
        ent.setAttribute("class", rec.getEntityClassname());
        if(rec.getEntityGoid()!=null) {
            ent.setAttribute("goid", Goid.toString(rec.getEntityGoid()));
        }
        e.appendChild(ent);

        return e;
    }
}
