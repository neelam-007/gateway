/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.Collections;
import java.util.Set;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class DateTimeAssertion extends Assertion {
    public void setRanges( Set ranges ) {
        _ranges = ranges;
    }

    public Set getRanges() {
        return _ranges;
    }

    protected Set _ranges = Collections.EMPTY_SET;
}
