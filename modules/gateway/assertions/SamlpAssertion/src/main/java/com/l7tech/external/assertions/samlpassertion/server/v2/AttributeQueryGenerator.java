package com.l7tech.external.assertions.samlpassertion.server.v2;

import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlp2MessageGenerator;
import com.l7tech.external.assertions.samlpassertion.server.SamlpAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.server.audit.Auditor;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.message.Message;
import com.l7tech.util.ExceptionUtils;
import org.xml.sax.SAXException;
import saml.v2.assertion.AttributeType;
import saml.v2.protocol.AttributeQueryType;

import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: vchan
 */
public final class AttributeQueryGenerator extends AbstractSamlp2MessageGenerator<AttributeQueryType> {
    private static final Logger logger = Logger.getLogger(AttributeQueryGenerator.class.getName());

    public AttributeQueryGenerator(final Map<String, Object> variablesMap, final Auditor auditor)
        throws SamlpAssertionException
    {
        super(variablesMap, auditor);
    }

    public JAXBElement<AttributeQueryType> createJAXBElement(AttributeQueryType samlpMsg) {
        return samlpFactory.createAttributeQuery(samlpMsg);
    }

    protected AttributeQueryType createMessageInstance() {
        return samlpFactory.createAttributeQueryType();
    }

    protected void buildSpecificMessageParts() {

        // build subject
        samlpMessage.setSubject( buildSubject() );

        // build attributes
        samlpMessage.getAttribute().addAll( buildAttributeQuery() );
    }

    private List<AttributeType> buildAttributeQuery() {

        List<AttributeType> result = new ArrayList<AttributeType>();

        SamlAttributeStatement as = assertion.getAttributeStatement();
        if (as != null) {
            AttributeType newAttr = null;
            for (SamlAttributeStatement.Attribute attr : as.getAttributes()) {

                newAttr = samlFactory.createAttributeType();
                newAttr.setName(getVariableValue(attr.getName()));

                // default attrib name format to unspecified
                String formatValue = getVariableValue(attr.getNameFormat());
                if (formatValue != null)
                    newAttr.setNameFormat(getVariableValue(formatValue));
                else
                    newAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED);

                // FriendlyName is optional
                String friendly = getVariableValue(attr.getFriendlyName());
                if (friendly != null && !friendly.isEmpty()) {
                    newAttr.setFriendlyName(friendly);
                }
                // AttributeValue is optional
                if (attr.isRepeatIfMulti()) {
                    Object valueObj = getVariableValues(attr.getValue());
                    if (valueObj instanceof Object[]) {
                        for (Object val : (Object[]) valueObj) {
                            if (val == null) continue;
                            if (val instanceof Message)
                                newAttr.getAttributeValue().add(parseMessageVariable((Message) val));
                            else
                                newAttr.getAttributeValue().add(val);
                        }
                    } else if (valueObj instanceof Message) {
                            newAttr.getAttributeValue().add(parseMessageVariable((Message) valueObj));
                    } else {
                        newAttr.getAttributeValue().add(valueObj);
                    }
                } else if (attr.getValue() != null && !attr.getValue().isEmpty()) {
                    Object val = getVariableValue(attr.getValue());
                    if (val instanceof Message)
                        newAttr.getAttributeValue().add(parseMessageVariable((Message) val));
                    else
                        newAttr.getAttributeValue().add(val);
                }

                result.add(newAttr);
            }
        }

        return result;
    }


    private Object parseMessageVariable(Message var) {
        // only deal with XML based Message variables
        try {
            if (var.isXml()) {
                return var.getXmlKnob().getDocumentReadOnly().getDocumentElement();
            }
        } catch (IOException ioex) {
            // log + continue
            logger.log(Level.WARNING, "Failed to parse Message context variable: {0}", ExceptionUtils.getMessage(ioex));
        } catch (SAXException saex) {
            // log + continue
            logger.log(Level.WARNING, "Failed to parse Message context variable: {0}", ExceptionUtils.getMessage(saex));
        }
        return null;
    }
}