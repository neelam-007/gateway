/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class CompositeAssertion extends Assertion {
    protected CompositeAssertion _parent;
    protected List _children = Collections.EMPTY_LIST;

    public Iterator children() {
        return Collections.unmodifiableList( _children ).iterator();
    }

    public CompositeAssertion( CompositeAssertion parent, List children ) {
        _parent = parent;
        _children = children;
    }

    public CompositeAssertion( List children ) {
        _parent = null;
        _children = children;
    }
}
