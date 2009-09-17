/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import static com.l7tech.policy.assertion.AssertionMetadata.*;


/**
 * An assertion that always returns a positive result.
 *
 * @author alex
 * @version $Revision$
 */
public class TrueAssertion extends Assertion {

    /**
     * Static TrueAssertion with no parent.  Useful as a "do-nothing" policy tree that needn't be
     * instantiated every time such a thing is needed.
     */
    private static final TrueAssertion INSTANCE = new TrueAssertion();

    public TrueAssertion() {}

    /**
     * Quickly get an existing TrueAssertion with no parent to use as a "do-nothing" policy tree.
     */
    public static TrueAssertion getInstance() {
        return INSTANCE;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});

        meta.put(SHORT_NAME, "Continue Processing");
        meta.put(DESCRIPTION, "Evaluate to true and continue processing.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");

        meta.putNull(PROPERTIES_ACTION_FACTORY);

        meta.put(USED_BY_CLIENT, Boolean.TRUE);
        return meta;
    }
    
}
