/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_ICON;

import java.util.List;

/**
 * Evaluate children until none left or one fails; return last result evaluated.
 *
 * Semantically equivalent to a short-circuited AND.
 */
public final class AllAssertion extends CompositeAssertion {
    public AllAssertion() {
    }

    public AllAssertion( List children ) {
        super( children );
    }

    /**
     * Check if the assertion is the root assertion.
     * @return true if this AllAssertion has no parent.
     */
    public boolean isRoot() {
        return getParent() == null;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(PALETTE_NODE_CLASSNAME, "com.l7tech.console.tree.AllNode");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");
        meta.put(POLICY_NODE_ICON_OPEN, "com/l7tech/console/resources/folderOpen.gif");

        meta.put(SHORT_NAME, "All assertions must evaluate to true");
        meta.put(DESCRIPTION, "All child assertions must evaluate to true.");

        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.console.tree.policy.AllAssertionTreeNode");

        meta.put(PROPERTIES_ACTION_NAME, "Add 'All…' Folder");
        meta.put(PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/folder.gif");

        meta.put(CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/folder.gif");
        meta.put(CLIENT_ASSERTION_POLICY_ICON_OPEN, "com/l7tech/proxy/resources/tree/folderOpen.gif");
        meta.put(USED_BY_CLIENT, Boolean.TRUE);
        
        return meta;
    }

}
