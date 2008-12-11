package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.processor.ProcessorResult;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML2.AttributeStatementType;
import x0Assertion.oasisNamesTcSAML2.AttributeType;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.logging.Level;


/**
 * Validation for SAML 2.x Attribute statement.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
class Saml2AttributeStatementValidate extends SamlStatementValidate {
    private SamlAttributeStatement attribueStatementRequirements;

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml     the saml statemenet assertion
     */
    Saml2AttributeStatementValidate(RequestWssSaml requestWssSaml) {
        super(requestWssSaml);
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
                            XmlObject statementAbstractType,
                            ProcessorResult wssResults, Collection validationResults) {
        if (!(statementAbstractType instanceof AttributeStatementType)) {
            throw new IllegalArgumentException("Expected " + AttributeStatementType.class);
        }
        AttributeStatementType attributeStatementType = (AttributeStatementType)statementAbstractType;
        AttributeType[] receivedAttributes = attributeStatementType.getAttributeArray();
        SamlAttributeStatement.Attribute[] expectedAttributes = attribueStatementRequirements.getAttributes();

        for (SamlAttributeStatement.Attribute expectedAttribute : expectedAttributes) {
            if (!isAttributePresented(expectedAttribute, receivedAttributes, validationResults)) {
                SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("No matching Attribute presented. Required {0}", null, expectedAttribute);
                if (logger.isLoggable(Level.FINER)) logger.finer(result.toString());
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
        String expectedName = expectedAttribute.getName();
        String expectedNameFormat = expectedAttribute.getNameFormat();
        if (expectedNameFormat == null || expectedNameFormat.length()==0) {
            expectedNameFormat = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
        }

        String expectedValue = expectedAttribute.getValue();
        boolean expectedAny = expectedAttribute.isAnyValue();
        if (isEmpty(expectedName)) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("Invalid Attribute constraint (name is null)", null);
            validationResults.add(result);
            logger.finer(result.toString());
            return false;
        }

        for (AttributeType receivedAttribute : receivedAttributes) {
            if (expectedName.equals(receivedAttribute.getName())) {
                String receivedNameFormat = receivedAttribute.getNameFormat();
                if (receivedNameFormat == null) {
                    receivedNameFormat = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
                }

                if (!expectedNameFormat.equals(receivedNameFormat)) {
                    continue;
                }

                if (isEmpty(expectedValue) && receivedAttribute.getAttributeValueArray().length == 0) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "Matched name {0} with no values presented.", new Object[]{ expectedName });
                    }
                    return true;
                }

                XmlObject[] values = receivedAttribute.getAttributeValueArray();
                for (XmlObject presentedValue : values) {
                    XmlCursor cursor = presentedValue.newCursor();
                    try {
                        if (expectedAny && !isEmpty(cursor.getTextValue())) {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(MessageFormat.format("Matched name {0}, any value", expectedName, expectedValue));
                            }
                            return true;
                        } else if (cursor.getTextValue().equals(expectedValue)) {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(MessageFormat.format("Matched name {0}, value {1} ", expectedName, expectedValue));
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
