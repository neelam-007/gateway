/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

import java.util.List;

/**
 * Evaluate children until none left or one succeeds; returns last result evaluated.
 *
 * Semantically equivalent to a short-circuited OR.
 */
public final class OneOrMoreAssertion extends CompositeAssertion {
    public OneOrMoreAssertion() {
    }

    public OneOrMoreAssertion(List children) {
        super(children);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(PALETTE_NODE_CLASSNAME, "com.l7tech.console.tree.OneOrMoreNode");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");
        meta.put(POLICY_NODE_ICON_OPEN, "com/l7tech/console/resources/folderOpen.gif");

        meta.put(SHORT_NAME, "At least one assertion must evaluate to true");
        meta.put(DESCRIPTION, "At least one child assertion must evaluate to true.");

        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.console.tree.policy.OneOrMoreAssertionTreeNode");
        
        meta.put(PROPERTIES_ACTION_NAME, "Add 'At least one...' Folder");
        meta.put(PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/folder.gif");

        meta.put(CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/folder.gif");
        meta.put(CLIENT_ASSERTION_POLICY_ICON_OPEN, "com/l7tech/proxy/resources/tree/folderOpen.gif");

        meta.put(USED_BY_CLIENT, Boolean.TRUE);
        return meta;
    }

}
