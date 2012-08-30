package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Pair;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import x0Assertion.oasisNamesTcSAML2.AttributeStatementType;
import x0Assertion.oasisNamesTcSAML2.AttributeType;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
     * @param requestSaml     the saml statemenet assertion
     */
    Saml2AttributeStatementValidate(RequireSaml requestSaml) {
        super(requestSaml);
        attribueStatementRequirements = requestSaml.getAttributeStatement();
        if (attribueStatementRequirements == null) {
            throw new IllegalArgumentException("Attribute requirements have not been specified");
        }
    }

    /**
     * Validate the attribute statement
     *
     * @param statementAbstractType
     * @param validationResults     where the results are collected
     * @param collectAttrValues
     * @param serverVariables
     * @param auditor
     */
    @Override
    protected void validate(XmlObject statementAbstractType,
                            Collection<SamlAssertionValidate.Error> validationResults, Collection<Pair<String, String[]>> collectAttrValues, Map<String, Object> serverVariables, Audit auditor) {
        if (!(statementAbstractType instanceof AttributeStatementType)) {
            throw new IllegalArgumentException("Expected " + AttributeStatementType.class);
        }
        AttributeStatementType attributeStatementType = (AttributeStatementType)statementAbstractType;
        AttributeType[] receivedAttributes = attributeStatementType.getAttributeArray();
        SamlAttributeStatement.Attribute[] expectedAttributes = attribueStatementRequirements.getAttributes();

        for (SamlAttributeStatement.Attribute expectedAttribute : expectedAttributes) {
            List<AttributeType> attrs = findAttribute(expectedAttribute, receivedAttributes, validationResults);
            if (attrs == null || attrs.isEmpty()) {
                SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("No matching Attribute presented. Required {0}", null, expectedAttribute);
                if (logger.isLoggable(Level.FINER)) logger.finer(result.toString());
                validationResults.add(result);
                return;
            }

            if (collectAttrValues != null) {
                for (AttributeType attr : attrs) {
                    // Record the validated attribute so it can be saved as a context variable later
                    String attrName = attr.getName();
                    List<String> values = new ArrayList<String>();
                    for (XmlObject xo : attr.getAttributeValueArray()) {
                        String value = toString(xo);
                        values.add(value);
                    }
                    collectAttrValues.add(new Pair<String, String[]>(attrName, values.toArray(new String[values.size()])));
                }
            }
        }
    }

    private static String canonicalize(Node node) throws IOException {
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        XmlUtil.canonicalize(node, baos);
        return baos.toString();
    }

    static String toString(XmlObject xo) {
        try {
            Node node = xo.getDomNode();
            if (Node.ELEMENT_NODE != node.getNodeType() || !"AttributeValue".equals(node.getLocalName())) {
                // Unexpected lack of an enclosing AttributeValue subelement.  Probably can't happen -- getAttributeValueArray() should have returned an empty array in this case.
                return canonicalize(node);
            }

            // If there is a single child that's a text node, return it as a string
            NodeList kids = node.getChildNodes();
            if (kids.getLength() == 1 && kids.item(0).getNodeType() == Node.TEXT_NODE)
                return XmlUtil.getTextValue((Element) node);

            // It's something more complicated.  Return the entire AttributeValue subelement as an XML string.
            return canonicalize(node);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Test whether the expected attribute is present in the receiv ed attributes array
     *
     * @param expectedAttribute  the attribute expected
     * @param receivedAttributes the presented attributes
     * @return all matching attributes, or null if none were found.  If this is non-null, it is guaranteed to contain at least one entry.
     */
    private List<AttributeType> findAttribute(SamlAttributeStatement.Attribute expectedAttribute, AttributeType[] receivedAttributes, Collection validationResults) {
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
            return null;
        }

        List<AttributeType> ret = new ArrayList<AttributeType>();
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
                    ret.add(receivedAttribute);
                }

                XmlObject[] values = receivedAttribute.getAttributeValueArray();
                for (XmlObject presentedValue : values) {
                    XmlCursor cursor = presentedValue.newCursor();
                    try {
                        if (expectedAny && !isEmpty(cursor.getTextValue())) {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(MessageFormat.format("Matched name {0}, any value", expectedName, expectedValue));
                            }
                            ret.add(receivedAttribute);
                        } else if (cursor.getTextValue().equals(expectedValue)) {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(MessageFormat.format("Matched name {0}, value {1} ", expectedName, expectedValue));
                            }
                            ret.add(receivedAttribute);
                        }
                    } finally {
                        cursor.dispose();
                    }
                }
            }
        }
        return ret.isEmpty() ? null : ret;
    }

    private boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }

}
