/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a generic Assertion.  Immutable except for de-persistence.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class Assertion implements Cloneable, Serializable {
    protected transient CompositeAssertion parent;
    private transient int ordinal;

    // 2.1 CustomAssertion compatibility
    private static final long serialVersionUID = -2639281346815614287L;

    public Assertion() {
        this.parent = null;
        this.ordinal = 1;
    }

    public CompositeAssertion getParent() {
        return parent;
    }

    /**
     * Reparent this assertion.  In normal operation, this should only be called by CompositeAssertions.
     * @param parent
     */
    protected void setParent(CompositeAssertion parent) {
        this.parent = parent;
    }

    /**
     * Notify this node that a child has been added, removed, or changed underneath it.  This causes the
     * entire policy tree to be traversed, renumbering all nodes and filling in any missing parent references.
     */
    public void treeChanged() {
        final Assertion p = getParent();
        if (p == null)
            renumber(1);
        else
            p.treeChanged();
    }

    /**
     * Check the ordinal number of this assertion within the policy tree.
     *
     * @return The ordinal number of this assertion's position within its policy, counting from top to bottom.
     */
    public int getOrdinal() {
        return ordinal;
    }

    /**
     * Look up the assertion in this subtree with the given ordinal.  Requires that this tree have up-to-date
     * numbering.  To ensure up-to-date numbering, call treeChanged() on an assertion within the tree.
     * @param ordinal the ordinal number of the assertion to check.
     * @return the Assertion with a matching ordinal, or null if one was not found.
     */
    public Assertion getAssertionWithOrdinal(int ordinal) {
        if (getOrdinal() == ordinal)
            return this;
        return null;
    }

    /** Renumber the target assertion and all its children. */
    protected static final int renumber(Assertion target, int number) {
        return target.renumber(number);
    }

    /** Set the target assertion's parent. */
    protected static final void setParent(Assertion target, CompositeAssertion parent) {
        target.setParent(parent);
    }

    /**
     * Renumber this assertion (and its children, if any) starting from the specified number.  After calling this,
     * getOrdinal() on this assertion (or its children, if any) will return meaningful values.
     * <p>
     * In normal operation this method should only be called by CompositeAssertions.  Normally, users should
     * call treeChanged() to request a policy tree to renumber itself.
     *
     * @param newStartingOrdinal the number to assign to this assertion.  Must be non-negative.
     *                           It's first child, if any, will be assigned the number (newStartingOrdinal + 1).
     * @return the lowest unused ordinal after this assertion and any children have been renumbered.
     */
    protected int renumber(int newStartingOrdinal) {
        this.ordinal = newStartingOrdinal;
        return newStartingOrdinal + 1;
    }

    /** Properly clone this Assertion.  The clone will have its parent set to null. */
    public Object clone() throws CloneNotSupportedException {
        Assertion clone = (Assertion)super.clone();
        clone.setParent(null);
        return clone;
    }

    /** More user friendly version of clone. */
    public Assertion getCopy() {
        try {
            return (Assertion) clone();
        } catch (CloneNotSupportedException e) {
            // can't happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates and returns an iterator that traverses the assertion subtree
     * rooted at this assertion in preorder.  The first node returned by the
     * iterator's
     * <code>next()</code> method is this assertion.<P>

     * @return	an <code>Iterator</code> for traversing the assertion tree in
     *          preorder
     */
    public Iterator preorderIterator() {
        return new PreorderIterator(this);
    }

    /**
     * Returns the path from the root, to get to this node. The last element
     * in the path is this node.
     *
     * @return an assertion path instance containing the <code>Assertion</code>
     * objects giving the path, where the first element in the path is the root and
     * the last element is this node.
     */
    public Assertion[] getPath() {
        Assertion node = this;
        LinkedList ll = new LinkedList();
        while (node !=null) {
            ll.addFirst(node);
            node = node.getParent();
        }
        return (Assertion[])ll.toArray(new Assertion[]{});

    }

    public String toIndentedString(int indentLevel) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < indentLevel; ++i)
            b.append("  ");
        b.append(this.toString());
        b.append("\n");
        return b.toString();
    }

    public String toString() {
        String fullClass = getClass().getName();
        return fullClass.substring(fullClass.lastIndexOf('.') + 1);
    }

    /**
     * preorder depth first assertion traversal.
     * The class is not synchronized.
     */
    final class PreorderIterator implements Iterator {
        private Stack stack = new Stack(); // oh well
        private Assertion lastReturned = null;

        public PreorderIterator(Assertion rootNode) {
            List list = new ArrayList(1);
            list.add(rootNode);
            stack.push(list.iterator());
        }

        public boolean hasNext() {
            return (!stack.empty() &&
              ((Iterator)stack.peek()).hasNext());
        }

        public Object next() {
            Iterator iterator = null;

            try {
                iterator = (Iterator)stack.peek();
            } catch (EmptyStackException e) {
                throw new NoSuchElementException(); // se contract
            }

            Assertion node = (Assertion)iterator.next();

            Iterator children = Collections.EMPTY_LIST.iterator();
            if (hasChildren(node)) {
                children = ((CompositeAssertion)node).getChildren().iterator();
            }

            if (!iterator.hasNext()) {
                stack.pop();
            }
            if (children.hasNext()) {
                stack.push(children);
            }
            lastReturned = node;
            return node;
        }

        /**
         * remove the current assertion that has been returned from
         * the <code>next()</code> method.
         *
         * @throws UnsupportedOperationException if unsuported remove
         * operation is requested. For exmaple root assertion cannot
         * be removed.
         *
         * @throws IllegalStateException as described by interface
         * {@see Iterator}
         */
        public void remove()
          throws UnsupportedOperationException, IllegalStateException {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            CompositeAssertion assparent = lastReturned.getParent();
            if (assparent == null) { // remove from parent
                throw new UnsupportedOperationException("cannot remove root");
            }
            // pop children that were pushed in next()
            if (hasChildren(lastReturned)) {
                stack.pop();
            }


            List nc = new ArrayList(assparent.getChildren());
            boolean removed = false;
            Iterator i;

            for (i = nc.iterator(); i.hasNext();) {
                if (lastReturned.equals(i.next())) {
                    i.remove();
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                throw new IllegalStateException("Object missing in parent: "+lastReturned);
            }
            assparent.setChildren(nc);
            if (i.hasNext()) {
                stack.pop();
                stack.push(i); // replace with new iterator
            }
            lastReturned = null;
        }

        private boolean hasChildren(Assertion a) {
            return (a instanceof CompositeAssertion &&
              ((CompositeAssertion)a).getChildren().size() > 0);
        }
    }
}

