package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.util.Pair;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML1.AttributeStatementType;
import x0Assertion.oasisNamesTcSAML1.AttributeType;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
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
     */
    SamlAttributeStatementValidate(RequireWssSaml requestWssSaml) {
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
     * @param collectAttrValues
     */
    protected void validate(Document document,
                            XmlObject statementAbstractType,
                            ProcessorResult wssResults, Collection validationResults, Collection<Pair<String, String[]>> collectAttrValues) {
        if (!(statementAbstractType instanceof AttributeStatementType)) {
            throw new IllegalArgumentException("Expected " + AttributeStatementType.class);
        }
        AttributeStatementType attributeStatementType = (AttributeStatementType)statementAbstractType;
        AttributeType[] receivedAttributes = attributeStatementType.getAttributeArray();
        SamlAttributeStatement.Attribute[] expectedAttributes = attribueStatementRequirements.getAttributes();

        for (SamlAttributeStatement.Attribute expectedAttribute : expectedAttributes) {
            AttributeType attr = findAttribute(expectedAttribute, receivedAttributes, validationResults);
            if (attr == null) {
                SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("No matching Attribute presented. Required {0}", null, expectedAttribute);
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(result.toString());
                }
                validationResults.add(result);
                return;
            }

            if (collectAttrValues != null) {
                // Record the validated attribute so it can be saved as a context variable later
                String attrName = attr.getAttributeName();
                List<String> values = new ArrayList<String>();
                for (XmlObject xo : attr.getAttributeValueArray()) {
                    String value = Saml2AttributeStatementValidate.toString(xo);
                    values.add(value);
                }
                collectAttrValues.add(new Pair<String, String[]>(attrName, values.toArray(new String[values.size()])));
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
    private AttributeType findAttribute(SamlAttributeStatement.Attribute expectedAttribute, AttributeType[] receivedAttributes, Collection validationResults) {
        String expectedName = expectedAttribute.getName();
        String expectedNamespace = expectedAttribute.getNamespace();
        String expectedValue = expectedAttribute.getValue();
        boolean expectedAny = expectedAttribute.isAnyValue();
        if (isEmpty(expectedName) || (!expectedAny && isEmpty(expectedValue))) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("Invalid Attribute constraint (name or value is null)", null);
            validationResults.add(result);
            logger.finer(result.toString());
            return null;
        }

        for (AttributeType receivedAttribute : receivedAttributes) {
            if (expectedName.equals(receivedAttribute.getAttributeName())) {
                if (!isEmpty(expectedNamespace) && !expectedNamespace.equals(receivedAttribute.getAttributeNamespace())) {
                    continue;
                }
                XmlObject[] values = receivedAttribute.getAttributeValueArray();
                for (XmlObject presentedValue : values) {
                    XmlCursor cursor = presentedValue.newCursor();
                    try {
                        if (expectedAny && !isEmpty(cursor.getTextValue())) {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(MessageFormat.format("Matched name {0}, any value", expectedName, expectedValue));
                            }
                            return receivedAttribute;
                        } else if (cursor.getTextValue().equals(expectedValue)) {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(MessageFormat.format("Matched name {0}, value {1} ", expectedName, expectedValue));
                            }
                            return receivedAttribute;
                        }
                    } finally {
                        cursor.dispose();
                    }
                }
            }
        }
        return null;
    }

    private boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }

}
