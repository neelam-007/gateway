/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.io.Serializable;
import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class CompositeAssertion extends Assertion implements Cloneable, Serializable {
    protected List children = new ArrayList();

    public CompositeAssertion() {
        super();
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
        n.setChildren(copyAndReparentChildren(n, children));
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
        final CompositeAssertion parent = getParent();
        super.treeChanged();
    }

    public void treeChanged() {
        for (Iterator i = children.iterator(); i.hasNext();) {
            Assertion kid = (Assertion)i.next();
            if (kid.getParent() != this)
                setParent(kid, this);
        }
        super.treeChanged();
    }

    protected int renumber(int newStartingOrdinal) {
        int n = super.renumber(newStartingOrdinal);
        for (Iterator i = children.iterator(); i.hasNext();) {
            Assertion kid = (Assertion)i.next();
            if (kid.getParent() != this)
                setParent(kid, this);
            n = renumber(kid, n);
        }
        return n;
    }

    public Assertion getAssertionWithOrdinal(int ordinal) {
        if (getOrdinal() == ordinal)
            return this;
        for (Iterator i = children.iterator(); i.hasNext();) {
            Assertion kid = (Assertion)i.next();
            Assertion kidResult = kid.getAssertionWithOrdinal(ordinal);
            if (kidResult != null)
                return kidResult;
        }
        return null;
    }

    /**
     * Copy children into a new list.  The new copies will each have their parent set to point to us.
     * @param newParent     What to use for the parent of the new list of children.
     * @param children      The children to copy and reparent.
     * @return              The copied and reparented list.
     */
    private List copyAndReparentChildren(CompositeAssertion newParent, List children) {
        List newKids = new LinkedList();
        for (Iterator i = children.iterator(); i.hasNext(); ) {
            Assertion child = ((Assertion)i.next()).getCopy();
            setParent(child, newParent);
            newKids.add(child);
        }
        return newKids;
    }

    /**
     * Return a new list of the given child nodes, with their Parent refs altered
     * in place to point to newParent.
     * @param newParent The new Parent CompositeAssertion.
     * @param children A list of child nodes whose Parent fields will be set to newParent.
     * @return A new list, but pointing at those same children.
     */
    private List reparentedChildren(CompositeAssertion newParent, List children) {
        List newKids = new LinkedList();
        for (Iterator i = children.iterator(); i.hasNext(); ) {
            Assertion child = (Assertion)i.next();
            setParent(child, newParent);
            newKids.add(child);
        }
        return newKids;
    }

    public String toIndentedString(int indentLevel) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < indentLevel; ++i)
            b.append("\t");
        b.append(super.toString());
        b.append(":\n");
        for (Iterator i = children.iterator(); i.hasNext();) {
            Assertion a = (Assertion) i.next();
            b.append(toIndentedString(a, indentLevel + 1));
        }
        return b.toString();
    }

    public String toString() {
        return toIndentedString(0);
    }
}
