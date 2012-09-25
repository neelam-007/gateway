package com.l7tech.server.policy.variable;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditDetailPropertiesDomMarshaller;
import com.l7tech.gateway.common.audit.MessagesUtil;
import com.l7tech.policy.variable.Syntax;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.MarshalException;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 *
 */
public class AuditDetailSelector implements ExpandVariables.Selector<AuditDetail> {

    private static final Logger logger = Logger.getLogger(AuditDetailSelector.class.getName());

    @Override
    public Selection select(String contextName, AuditDetail detail, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if (detail == null)
            return null;
        if ("componentId".equalsIgnoreCase(name)) {
            return new Selection(detail.getComponentId());
        } else if ("messageId".equalsIgnoreCase(name)) {
            return new Selection(String.valueOf(detail.getMessageId()));
        } else if ("exception".equalsIgnoreCase(name)) {
            return new Selection(detail.getException());
        } else if ("ordinal".equalsIgnoreCase(name)) {
            return new Selection(detail.getOrdinal());
        } else if ("params".equalsIgnoreCase(name)) {
            return new Selection(detail.getParams());
        } else if ("properties".equalsIgnoreCase(name)) {
            return new Selection(getProperties(detail));
        } else if ("time".equalsIgnoreCase(name)) {
            return new Selection(detail.getTime());
        } else if ("fullText".equalsIgnoreCase(name)) {
            AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(detail.getMessageId());
            if (message != null) {
                MessageFormat mf = new MessageFormat(message.getMessage());
                StringBuffer result = new StringBuffer();
                mf.format(detail.getParams(), result, new FieldPosition(0));
                return new Selection(result.toString());
            }
            return new Selection(null);
        }
        return null;
    }

    private String getProperties(AuditDetail detail) {
        Document doc = XmlUtil.createEmptyDocument();
        try {
            Element element = detailsMarshaller.marshal(doc, detail);
            doc.appendChild(element);
            return XmlUtil.nodeToFormattedString(element);
        } catch (MarshalException e) {
            logger.warning("Unable to marshall properties");
        } catch (IOException e) {
            logger.warning("Unable to marshall properties");
        }
        return null;
    }

    @Override
    public Class<AuditDetail> getContextObjectClass() {
        return AuditDetail.class;
    }
    private static final AuditDetailPropertiesDomMarshaller detailsMarshaller = new AuditDetailPropertiesDomMarshaller();
}
