/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.*;
import java.io.Serializable;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class CompositeAssertion extends Assertion implements Serializable {
    protected CompositeAssertion parent;
    protected List children = Collections.EMPTY_LIST;

    public CompositeAssertion() {
    }

    public Iterator children() {
        return Collections.unmodifiableList( children ).iterator();
    }

    public List getChildren() {
        return children;
    }

    public void setChildren(List children) {
        this.children = children;
    }

    public CompositeAssertion getParent() {
        return parent;
    }

    public void setParent(CompositeAssertion parent) {
        this.parent = parent;
    }

    public CompositeAssertion( CompositeAssertion parent, List children ) {
        this.parent = parent;
        this.children = children;
    }

    public CompositeAssertion( List children ) {
        this.parent = null;
        this.children = children;
    }

    public String toString() {
        return "<" + this.getClass().getName() + " children=" + children + " parent=" + parent + ">";
    }
}
