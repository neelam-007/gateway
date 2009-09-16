/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_ICON;

import java.io.Serializable;
import java.util.List;

/**
 * Asserts that one and only one child assertion returns a true value.
 *
 * Semantically equivalent to a non-short-circuited exclusive-OR.
 */
public final class ExactlyOneAssertion extends CompositeAssertion implements Serializable {
    public ExactlyOneAssertion() {
        super();
    }

    public ExactlyOneAssertion( List children ) {
        super( children );
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.console.tree.policy.ExactlyOneAssertionTreeNode");

        meta.put(PROPERTIES_ACTION_NAME, "Add 'exactly one' assertion");
        meta.put(PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/folder.gif");

        return meta;
    }    
}
