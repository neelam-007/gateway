/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ncesdeco.console;

import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.external.assertions.ncesdeco.NcesDecoratorAssertion;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class NcesDecoratorPolicyNode extends DefaultAssertionPolicyNode<NcesDecoratorAssertion> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.ncesdeco.NcesDecoratorAssertion");

    private static final String RESPONSE_BIT = resources.getString("policyNodeName.response");
    private static final String REQUEST_BIT = resources.getString("policyNodeName.request");
    private static final String NAME_PATTERN = resources.getString("policyNodeName.pattern");

    public NcesDecoratorPolicyNode(NcesDecoratorAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        final String bit;
        switch(assertion.getTarget()) {
            case REQUEST:
                bit = REQUEST_BIT;
                break;
            case RESPONSE:
                bit = RESPONSE_BIT;
                break;
            case OTHER:
                bit = assertion.getOtherTargetMessageVariable();
                break;
            default:
                throw new IllegalStateException("Unsupported target type: " + assertion.getTarget());
        }
        return MessageFormat.format(NAME_PATTERN, bit);
    }
}
