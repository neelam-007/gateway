/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

public abstract class NodeActionWithMetaSupport extends NodeAction{

    private final AssertionMetadata metaData;

    public NodeActionWithMetaSupport(AbstractTreeNode node, Class requiredAssertionLicense, final Assertion assertion) {
        super(node, requiredAssertionLicense,
                getMetaFromAssertion(PROPERTIES_ACTION_NAME, assertion),
                getMetaFromAssertion(PROPERTIES_ACTION_DESC, assertion),
                getMetaFromAssertion(PROPERTIES_ACTION_ICON, assertion));
        metaData = assertion.meta();
    }

    public NodeActionWithMetaSupport(AbstractTreeNode node, final Assertion assertion) {
        super(node, 
                getMetaFromAssertion(PROPERTIES_ACTION_NAME, assertion),
                getMetaFromAssertion(PROPERTIES_ACTION_DESC, assertion),
                getMetaFromAssertion(PROPERTIES_ACTION_ICON, assertion));
        metaData = assertion.meta();
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return metaData.get(PROPERTIES_ACTION_NAME).toString();
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return metaData.get(PROPERTIES_ACTION_DESC).toString();
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return metaData.get(PROPERTIES_ACTION_ICON).toString();
    }
    
    public static String getMetaFromAssertion(final String metaKey, final Assertion assertion){
        return assertion.meta().get(metaKey).toString();
    }

}
