package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML1.AttributeStatementType;
import x0Assertion.oasisNamesTcSAML1.AttributeType;
import x0Assertion.oasisNamesTcSAML1.SubjectStatementAbstractType;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.logging.Level;

/**
 * @author emil
 * @version 27-Jan-2005
 */
class SamlAttributeStatementValidate extends SamlStatementValidate {
    private SamlAttributeStatement attribueStatementRequirements;

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml     the saml statemenet assertion
     * @param applicationContext the applicaiton context to allo access to components and services
     */
    SamlAttributeStatementValidate(RequestWssSaml requestWssSaml, ApplicationContext applicationContext) {
        super(requestWssSaml, applicationContext);
        attribueStatementRequirements = requestWssSaml.getAttributeStatement();
        if (attribueStatementRequirements == null) {
            throw new IllegalArgumentException("Attribute requirements have not been specified");
        }
    }

    /**
     * Validate the attribute statement
     *
     * @param document
     * @param statementAbstractType
     * @param wssResults
     * @param validationResults     where the results are collected
     */
    protected void validate(Document document,
                            SubjectStatementAbstractType statementAbstractType,
                            ProcessorResult wssResults, Collection validationResults) {
        if (!(statementAbstractType instanceof AttributeStatementType)) {
            throw new IllegalArgumentException("Expected " + AttributeStatementType.class);
        }
        AttributeStatementType attributeStatementType = (AttributeStatementType)statementAbstractType;
        AttributeType[] receivedAttributes = attributeStatementType.getAttributeArray();
        SamlAttributeStatement.Attribute[] expectedAttributes = attribueStatementRequirements.getAttributes();

        for (int i = 0; i < expectedAttributes.length; i++) {
            SamlAttributeStatement.Attribute expectedAttribute = expectedAttributes[i];
            if (!isAttributePresented(expectedAttribute, receivedAttributes, validationResults)) {
                SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("No matching Attribute presented. Required {0}", null, expectedAttribute, null);
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(result.toString());
                }
                validationResults.add(result);
                return;
            }
        }
    }

    /**
     * Test whether the expected attribute is present in the receiv ed attributes array
     *
     * @param expectedAttribute  the attribute expected
     * @param receivedAttributes the presented attributes
     * @return true if the expected attribute is present, false otherwise
     */
    private boolean isAttributePresented(SamlAttributeStatement.Attribute expectedAttribute, AttributeType[] receivedAttributes, Collection validationResults) {
        String name = expectedAttribute.getName();
        String nameSpace = expectedAttribute.getNamespace();
        String value = expectedAttribute.getValue();
        if (isEmpty(name) || isEmpty(value)) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("Invalid Attribute constraint (name or value is null)", null, null, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return false;
        }

        for (int i = 0; i < receivedAttributes.length; i++) {
            AttributeType receivedAttribute = receivedAttributes[i];
            if (name.equals(receivedAttribute.getAttributeName())) {
                if (!isEmpty(nameSpace) && !nameSpace.equals(receivedAttribute.getAttributeNamespace())) {
                    continue;
                }
                XmlObject[] values = receivedAttribute.getAttributeValueArray();
                for (int j = 0; j < values.length; j++) {
                    XmlObject presentedValue = values[j];
                    XmlCursor cursor = presentedValue.newCursor();
                    try {
                        if (cursor.getTextValue().equals(value)) {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(MessageFormat.format("Matched name {0}, value {1} ", new Object[]{name, value}));
                            }
                            return true;
                        }
                    } finally {
                        cursor.dispose();
                    }
                }
            }
        }
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    private boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }

}
