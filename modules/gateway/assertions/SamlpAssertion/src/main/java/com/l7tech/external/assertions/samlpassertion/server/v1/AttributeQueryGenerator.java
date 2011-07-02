package com.l7tech.external.assertions.samlpassertion.server.v1;

import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlp1MessageGenerator;
import com.l7tech.external.assertions.samlpassertion.server.SamlpAssertionException;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import saml.v1.assertion.AttributeDesignatorType;
import saml.v1.protocol.AttributeQueryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: vchan
 */
public final class AttributeQueryGenerator extends AbstractSamlp1MessageGenerator<AttributeQueryType> {

    public AttributeQueryGenerator(final Map<String, Object> variablesMap, final Audit auditor)
        throws SamlpAssertionException
    {
        super(variablesMap, auditor);
    }

    @Override
    protected AttributeQueryType createMessageInstance() {
        return samlpFactory.createAttributeQueryType();
    }

    @Override
    protected void buildSpecificMessageParts() {

        // build subject
        samlpMessage.setSubject( buildSubject() );

        // build attributes
        samlpMessage.getAttributeDesignator().addAll( buildAttributeQuery() );
    }

    private List<AttributeDesignatorType> buildAttributeQuery() {

        List<AttributeDesignatorType> result = new ArrayList<AttributeDesignatorType>();

        SamlAttributeStatement as = assertion.getAttributeStatement();
        if (as != null) {
            AttributeDesignatorType newAttr;
            for (SamlAttributeStatement.Attribute attr : as.getAttributes()) {
                newAttr = samlFactory.createAttributeDesignatorType();
                newAttr.setAttributeName(getVariableValue(attr.getName()));
                newAttr.setAttributeNamespace(getVariableValue(attr.getNamespace()));
                result.add(newAttr);
            }
        }

        return result;
    }

}
