/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.AssertionPath;
import com.l7tech.proxy.datamodel.PendingRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Immutable except for de-persistence.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class Assertion implements Cloneable, Serializable {
    protected CompositeAssertion parent;

    /**
     * SSG Server-side processing of the given request.
     * @param request       (In/Out) The request to check.  May be modified by processing.
     * @param response      (Out) The response to send back.  May be replaced during processing.
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public abstract AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException;

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public abstract AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException;

    public Assertion() {
        this.parent = null;
    }

    public Assertion(CompositeAssertion parent) {
        this.parent = parent;
    }

    public CompositeAssertion getParent() {
        return parent;
    }

    public void setParent(CompositeAssertion parent) {
        this.parent = parent;
    }

    /** Properly clone this Assertion.  The clone will have its parent set to null. */
    public Object clone() throws CloneNotSupportedException {
        Assertion clone = (Assertion)super.clone();
        clone.setParent(null);
        return clone;
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
    public String toString() {
        return "<" + this.getClass().getName() + ">";
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

