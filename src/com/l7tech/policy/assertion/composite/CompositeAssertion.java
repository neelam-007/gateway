/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class CompositeAssertion extends Assertion implements Cloneable, Serializable {
    protected List children = Collections.EMPTY_LIST;

    public CompositeAssertion() {
        super();
    }

    public CompositeAssertion( CompositeAssertion parent, List children ) {
        super(parent);
        setChildren(children);
    }

    /**
     * Create a new CompositeAssertion with no parent and the specified children.
     * The children will be copied, and each of their parents reset to point to us.
     * @param children
     */
    public CompositeAssertion( List children ) {
        super();
        setChildren(children);
    }

    /**
     * Clone this composite and all it's children.
     * The clone will have copies of the children but no parent.
     */
    public Object clone() throws CloneNotSupportedException {
        CompositeAssertion n = (CompositeAssertion)super.clone();
        n.setChildren(children);
        return n;
    }

    public Iterator children() {
        return Collections.unmodifiableList( children ).iterator();
    }

    public List getChildren() {
        return children;
    }

    public void setChildren(List children) {
        this.children = reparentedChildren(this, children);
    }

    /**
     * Copy children into a new list.  The new copies will each have their parent set to point to us.
     * @param newParent     What to use for the parent of the new list of children.
     * @param children      The children to copy and reparent.
     * @return              The copied and reparented list.
     */
    private List reparentedChildren(CompositeAssertion newParent, List children) {
        List newKids = new LinkedList();
        for (Iterator i = children.iterator(); i.hasNext(); ) {
            Assertion child = (Assertion)i.next();
            Assertion newChild = null;
            try {
                newChild = (Assertion)child.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Impossible exception", e); // can't happen
            }
            newChild.setParent(newParent);
            newKids.add(newChild);
        }
        return newKids;
    }

    /**
     * Ensure that this CompositeAssertion has at least one child.
     * @throws PolicyAssertionException if the children list is empty
     */
    protected void mustHaveChildren() throws PolicyAssertionException {
        if (children.isEmpty())
            throw new PolicyAssertionException("CompositeAssertion has no children: " + this);
    }

    public String toString() {
        return "<" + this.getClass().getName() + " children=" + children + " parent=" +
                (parent == null ? "NULL" : parent.getClass().getName()) + ">";
    }
}
