/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.OversizedTextAssertion;

/**
 * Policy palette tree node for OversizedTextAssertion.
 */
public class OversizedTextAssertionPaletteNode extends AbstractLeafPaletteNode {
    public OversizedTextAssertionPaletteNode() {
        super("Document Structure Threats", "com/l7tech/console/resources/OversizedElement16.gif");
    }

    public Assertion asAssertion() {
        return new OversizedTextAssertion();
    }
}