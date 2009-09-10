/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.util.Functions;

public abstract class ClientAssertionWithMetaSupport extends ClientAssertion{

    private final Assertion assertion;

    public ClientAssertionWithMetaSupport(final Assertion assertion){
        this.assertion = assertion;
    }

    public String getName() {
        Object factory = assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        String name = null;
        if (factory instanceof Functions.Unary) {
            //noinspection unchecked
            Functions.Unary<String, Assertion> unary = (Functions.Unary<String, Assertion>)factory;
            name = unary.call(assertion);
        } else if(factory instanceof Functions.Binary){
            //noinspection unchecked
            Functions.Binary<String, Assertion, Boolean> unary = (Functions.Binary<String, Assertion, Boolean>)factory;
            name = unary.call(assertion, true);
        } else if (factory != null && factory instanceof String) {
            name = addFeatureNames(factory.toString());
        } else {
            Object obj = assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME);
            if (obj != null)
                name = addFeatureNames(obj.toString());
        }
        return name != null ? name : assertion.getClass().getName();

    }

    /**
     * Add any prefixes and suffixes to a static name based on supported features (like SecurityHeaderAddressable).
     * <p/>
     * This method is never invoked if the name is already a dynamic name, having come from a POLICY_NODE_NAME_FACTORY.
     *
     * @param name
     * @return the name, possibly with one or more prefixes or suffixes added.
     */
    protected String addFeatureNames(String name) {
        return AssertionUtils.decorateName(assertion, name);
    }
    
    public String iconResource(boolean open) {
        final String s = (String)assertion.meta().get(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON);
        return s != null ? s : "com/l7tech/console/resources/policy16.gif";
    }

}
