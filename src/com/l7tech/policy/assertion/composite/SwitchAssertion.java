/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.Assertion;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * @author alex
 * @version $Revision$
 */
public class SwitchAssertion extends CompositeAssertion {
    public SwitchAssertion() {
        super();
    }

    public SwitchAssertion( List children ) {
        super( children );
    }
}
