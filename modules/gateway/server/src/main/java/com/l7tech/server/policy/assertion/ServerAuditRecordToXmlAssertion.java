package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditRecordDomMarshaller;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditRecordToXmlAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.MarshalException;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 */
public class ServerAuditRecordToXmlAssertion extends AbstractMessageTargetableServerAssertion<AuditRecordToXmlAssertion> {
    private static final Logger logger = Logger.getLogger(ServerAuditRecordToXmlAssertion.class.getName());
    private static final String MSG_NEED_AUDIT_POLICY = "This assertion only works inside an audit sink policy.";
    private static final AuditRecordDomMarshaller auditRecordDomMarshaller = new AuditRecordDomMarshaller();
    private final Auditor auditor;

    public ServerAuditRecordToXmlAssertion(AuditRecordToXmlAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) {
        super(assertion, assertion);
        this.auditor = new Auditor(this, beanFactory, eventPub, logger);
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
            throws IOException, PolicyAssertionException
    {
        // Get audit context
        AuditRecord auditRecord = getAuditRecord(context);
        Document doc = initializeToEmptyDomDocument(message);
        try {
            doc.appendChild(auditRecordDomMarshaller.marshal(doc, auditRecord));
        } catch (MarshalException e) {
            final String msg = "Unable to convert audit record to XML: " + ExceptionUtils.getMessage(e);
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, msg);
        }

        return AssertionStatus.NONE;
    }

    private Document initializeToEmptyDomDocument(Message mess) {
        try {
            // Try to preserve existing message knobs, if possible
            if (mess.isXml()) {
                Document doc = mess.getXmlKnob().getDocumentWritable();
                NodeList docElement = doc.getChildNodes();
                for (int i = 0; i < docElement.getLength(); ++i) {
                    doc.removeChild(docElement.item(i));
                }
                // It's clear.
                return doc;
            }
            // Fallthrough and reinitialize
        } catch (IOException e) {
            // Fallthrough and reinitialize
        } catch (SAXException e) {
            // Fallthrough and reinitialize
        }

        mess.initialize(XmlUtil.createEmptyDocument());
        try {
            return mess.getXmlKnob().getDocumentWritable();
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Get the audit record.  Only works if a context variable named "audit" exists of type AuditRecord; this is
     * normally only the case if this assertion is currently running inside an audit sink policy.
     * @param context  the PEC.  required
     * @return the audit record.  Never null.
     * @throws AssertionStatusException if there is no context variable "audit" with type AuditRecord.
     */
    private AuditRecord getAuditRecord(PolicyEnforcementContext context) {
        try {
            Object auditContextObj = context.getVariable("audit");
            if (!(auditContextObj instanceof AuditRecord)) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The context variable 'audit' exists but is not of type AuditRecord.");
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, MSG_NEED_AUDIT_POLICY);
            }
            return (AuditRecord) auditContextObj;
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, "audit");
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, MSG_NEED_AUDIT_POLICY);
        }
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }
}
