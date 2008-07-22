/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import java.io.Serializable;
import java.util.List;

/**
 * Asserts that one and only one child assertion returns a true value.
 *
 * Semantically equivalent to a non-short-circuited exclusive-OR.
 *
 * @author alex
 * @version $Revision$
 */
public class ExactlyOneAssertion extends CompositeAssertion implements Serializable {
    public ExactlyOneAssertion() {
        super();
    }

    public ExactlyOneAssertion( List children ) {
        super( children );
    }
}
