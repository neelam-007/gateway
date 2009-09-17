/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import static com.l7tech.policy.assertion.AssertionMetadata.*;


/**
 * An assertion that always returns a negative result.
 *
 * @author alex
 * @version $Revision$
 */
public class FalseAssertion extends Assertion {
    /**
     * Static FalseAssertion with no parent.  Useful as a "do-nothing" policy tree that needn't be
     * instantiated every time such a thing is needed.
     */
    private static final FalseAssertion INSTANCE = new FalseAssertion();

    public static FalseAssertion getInstance() {
        return INSTANCE;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});

        meta.put(SHORT_NAME, "Stop Processing");
        meta.put(DESCRIPTION, "Evaluate to false and stop processing this branch of the policy.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Stop16.gif");
        return meta;
    }
    
}
