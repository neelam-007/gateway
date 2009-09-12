/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.ResponseXpathAssertion;
import com.l7tech.policy.variable.Syntax;

/**
 * @author Franco
 */
public class ResponseXpathPolicyTreeNode extends XpathBasedAssertionTreeNode {
    public ResponseXpathPolicyTreeNode(ResponseXpathAssertion assertion) {
        super( assertion );
    }
}
