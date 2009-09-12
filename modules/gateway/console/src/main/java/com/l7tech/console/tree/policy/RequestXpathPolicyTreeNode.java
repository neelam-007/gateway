/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.RequestXpathAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public class RequestXpathPolicyTreeNode extends XpathBasedAssertionTreeNode {
    public RequestXpathPolicyTreeNode( RequestXpathAssertion assertion ) {
        super( assertion );
    }
}
