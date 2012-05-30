package com.l7tech.gateway.common.audit;

import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.bind.MarshalException;
import java.io.IOException;

/**
 * marshalling of an audit record's additional properties to a DOM element.
 * Properties includes:
 *          None.
*/
public class AuditRecordPropertiesDomMarshaller {
    private static final String NS = "http://l7tech.com/audit/rec";

    /**
     * Add the specified AuditRecord as a DOM element as a child of the specified parent element.
     *
     * @param factory  DOM factory to use.  Required.
     * @param auditRecord the audit record to marshall.  Required.
     * @return the element that was created.
     */
    public Element marshal(Document factory, AuditRecord auditRecord) throws MarshalException, IOException {
        Element e = createElement(factory, "audit");
        addAuditRecordFields(e, auditRecord);

        if (auditRecord instanceof AdminAuditRecord) {
            AdminAuditRecord adminAuditRecord = (AdminAuditRecord) auditRecord;
            addAdminRecordFields(e, adminAuditRecord);
        } else if (auditRecord instanceof SystemAuditRecord) {
            SystemAuditRecord systemAuditRecord = (SystemAuditRecord) auditRecord;
            addSystemRecordFields(e, systemAuditRecord);
        } else if (auditRecord instanceof MessageSummaryAuditRecord) {
            MessageSummaryAuditRecord messageSummaryAuditRecord = (MessageSummaryAuditRecord) auditRecord;
            addMessageRecordFields(e, messageSummaryAuditRecord);
        } else
            throw new MarshalException("Unsupported audit record type");
        XmlUtil.nodeToFormattedOutputStream(e, System.out);
        return e;
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
        // nothing to add
    }


    private void addAdminRecordFields(Element e, AdminAuditRecord rec) {
        // nothing to add
    }

    private void addMessageRecordFields(Element e, MessageSummaryAuditRecord rec) {
        // nothing to add
    }

    private void addSystemRecordFields(Element e, SystemAuditRecord rec) {
        // nothing to add
    }
}
