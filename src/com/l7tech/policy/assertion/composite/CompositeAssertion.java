/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.Assertion;

import java.io.Serializable;
import java.util.*;

/**
 * @author alex
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

    public void clearChildren() {
        children.clear();
    }

    public void addChild(Assertion kid) {
        children.add(kid);
        setParent(kid, this);        
        super.treeChanged();
    }

    public void addChild(int index, Assertion kid) {
        children.add(index, kid);
        setParent(kid, this);
        super.treeChanged();
    }

    public void removeChild(Assertion kid) {
        children.remove(kid);
        super.treeChanged();
    }

    public void setChildren(List children) {
        this.children = reparentedChildren(this, children);
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
     * Check if this composite assertion currently has any children.  Empty composite assertions are invalid, and will
     * always throw PolicyException at runtime.
     *
     * @return true if this composition assertion lacks children and hence will always fail at runtime
     */
    public boolean isEmpty() {
        return children.isEmpty();
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
            Object next = i.next();
            if (!(next instanceof Assertion))
                throw new ClassCastException("CompositeAssertion contains a child of non-Assertion type " + next.getClass());
            Assertion child = (Assertion)next;
            setParent(child, newParent);
            newKids.add(child);
        }
        return newKids;
    }

    public String toIndentedString(int indentLevel) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < indentLevel; ++i)
            b.append("  ");
        b.append(super.toString());
        b.append(":\n");
        for (Iterator i = children.iterator(); i.hasNext();) {
            Assertion a = (Assertion) i.next();
            b.append(a.toIndentedString(indentLevel + 1));
        }
        return b.toString();
    }

    public String toString() {
        return toIndentedString(0);
    }
}
