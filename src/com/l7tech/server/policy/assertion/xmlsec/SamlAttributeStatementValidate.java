package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlStatementAssertion;
import org.apache.xmlbeans.XmlObject;
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
    /**
     * Construct  the <code>SamlStatementValidate</code> for the statement assertion
     *
     * @param statementAssertion the saml statemenet assertion
     * @param applicationContext the applicaiton context to allo access to components and services
     */
    SamlAttributeStatementValidate(SamlStatementAssertion statementAssertion, ApplicationContext applicationContext) {
        super(statementAssertion, applicationContext);
    }

    /**
         * Validate the attribute statement
         *
         * @param document
         * @param statementAbstractType
         * @param wssResults
         * @param validationResults     where the results are collected
         */
    protected void validateStatement(Document document,
                                     SubjectStatementAbstractType statementAbstractType,
                                     ProcessorResult wssResults, Collection validationResults) {
        if (!(statementAbstractType instanceof AttributeStatementType)) {
            throw new IllegalArgumentException("Expected " + AttributeStatementType.class);
        }
        AttributeStatementType attributeStatementType = (AttributeStatementType)statementAbstractType;
        SamlAttributeStatement samlAttribueStatement = (SamlAttributeStatement)statementAssertionConstraints;
        AttributeType[] receivedAttributes = attributeStatementType.getAttributeArray();
        SamlAttributeStatement.Attribute[] expectedAttributes = samlAttribueStatement.getAttributes();

        for (int i = 0; i < expectedAttributes.length; i++) {
            SamlAttributeStatement.Attribute expectedAttribute = expectedAttributes[i];
            if (!isAttributePresented(expectedAttribute, receivedAttributes, validationResults)) {
                validationResults.add(new Error("No matching Attribute has been presented", expectedAttribute, null, null));
                return ;
            }
        }
    }

    /**
     * Test whether the expected attribute is present in the receiv ed attributes array
     * @param expectedAttribute the attribute expected
     * @param receivedAttributes the presented attributes
     * @return true if the expected attribute is present, false otherwise
     */
    private boolean isAttributePresented(SamlAttributeStatement.Attribute expectedAttribute, AttributeType[] receivedAttributes, Collection validationResults) {
        String name = expectedAttribute.getName();
        String nameSpace = expectedAttribute.getNamespace();
        String value = expectedAttribute.getValue();
        if (name == null || value == null) {
            validationResults.add(new Error("Invalid Attribute constraint (name or value is null)", expectedAttribute, null, null));
        }
        for (int i = 0; i < receivedAttributes.length; i++) {
            AttributeType receivedAttribute = receivedAttributes[i];
            if (name.equals(receivedAttribute.getAttributeName())) {
                if (nameSpace != null && !nameSpace.equals(receivedAttribute.getAttributeNamespace())) {
                    continue;
                }
                XmlObject[] values = receivedAttribute.getAttributeValueArray();
                for (int j = 0; j < values.length; j++) {
                    XmlObject presentedValue = values[j];
                    if (presentedValue.compareTo(value) == XmlObject.EQUAL) {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(MessageFormat.format("Matched name {0}, value {1} ", new Object[] {name, value}));
                        }
                        return true;
                    }
                }
            }
        }
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

}
