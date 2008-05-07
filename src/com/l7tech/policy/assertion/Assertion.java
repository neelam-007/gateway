/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.HardwareAccelerated;
import com.l7tech.policy.assertion.annotation.ProcessesMultipart;
import com.l7tech.common.util.ClassUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a generic Assertion.  An Assertion is a bean that holds configuration for a single
 * server or client assertion instance.
 * <p/>
 * Assertions derived from CompositeAssertion can contain child assertions, including other CompositeAssertions.
 * Assertions arranged in a tree, along with a tiny amount of metadata, make up a policy fragment.
 * Assertions can be serialized to policy XML using the WspWriter and deserialized from policy XML using the WspReader.
 * <p/>
 * On the Gateway, a server assertion instance is invoked with a PolicyEnforcementContext
 * to take some action in response to a request or a response message.  The ServerPolicyFactory takes care
 * of instantiating a tree of ServerAssertion instances from a tree of generic Assertion beans.
 * <p/>
 * Within the XML VPN Client, a client assertion instance is invoked with a PolicyApplicationContext
 * to apply security decoration to a request message, or to unapply security decoration from a response message.
 * The ClientPolicyFactory takes care of instantiating a tree of ClientAssertion instances from a tree of
 * generic Assertion beans.
 *
 * @noinspection unchecked,ForLoopReplaceableByForEach,EqualsWhichDoesntCheckParameterClass
 */
public abstract class Assertion implements Cloneable, Serializable {
    private static final Map metadataCache = Collections.synchronizedMap(new HashMap());
    protected transient CompositeAssertion parent;
    private transient int ordinal;
    private transient Long ownerPolicyOid = null;

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
     * @param parent the new parent
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

    /**
     * Renumber the target assertion and all its children.
     *
     * @param target             the assertion to renumber
     * @param number             the number to assign to this assertion.  Must be non-negative.
     *                           It's first child, if any, will be assigned the number (newStartingOrdinal + 1).
     * @return the lowest unused ordinal after this assertion and any children have been renumbered.
     */
    protected static int renumber(Assertion target, int number) {
        return target.renumber(number);
    }

    /**
     * Set the target assertion's parent.
     *
     * @param target  the assertion to reparent
     * @param parent  the new parent
     */
    protected static void setParent(Assertion target, CompositeAssertion parent) {
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

    /**
     * Properly clone this Assertion.  The clone will have its parent set to null.
     * @noinspection CloneDoesntDeclareCloneNotSupportedException
     */
    @Override
    public Object clone() {
        final Assertion clone;
        try {
            clone = (Assertion) super.clone();
            clone.setParent(null);
        } catch (CloneNotSupportedException e) {
            // can't happen
            throw new RuntimeException(e);
        }
        return clone;
    }

    /**
     * More user friendly version of clone.
     * @return an Assertion instance that is a (hopefully) deep copy of this one.
     */
    public Assertion getCopy() {
        return (Assertion) clone();
    }

    /**
     * Test whether the assertion is a credential source. Defaults to <code>false</code>
     *
     * @return true if credential source, false otherwise
     */
    public boolean isCredentialSource() {
        return false;
    }

    /**
     * Test whether the assertion is a credential modifier. Defaults to <code>false</code>
     *
     * <p>This is for assertions that modifiy the request, adding credential information
     * (presumably replacing one kind of credential with another).</p>
     *
     * <p>A modifier is typically not a credential source, but may be followed by one in
     * a policy.</p>
     *
     * @return true if credential source, false otherwise
     */
    public boolean isCredentialModifier() {
        return false;
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
     * Creates and returns an iterator that traverses the assertion subtree
     * rooted at this assertion in preorder.  The first node returned by the
     * iterator's
     * <code>next()</code> method is this assertion.<P>

     * @return	an <code>Iterator</code> for traversing the assertion tree in
     *          preorder
     */
    public Iterator preorderIterator(AssertionTranslator translator) throws PolicyAssertionException {
        return new NewPreorderIterator(this, translator);
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
        return (Assertion[])ll.toArray(new Assertion[ll.size()]);

    }

    public String toIndentedString(int indentLevel) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < indentLevel; ++i)
            b.append("  ");
        b.append(this.toString());
        b.append("\n");
        return b.toString();
    }

    @Override
    public String toString() {
        String fullClass = getClass().getName();
        return fullClass.substring(fullClass.lastIndexOf('.') + 1);
    }

    public Long ownerPolicyOid() {
        Long oid = ownerPolicyOid;

        if ( oid == null ) {
            Assertion parent = getParent();
            if ( parent != null ) {
                oid = parent.ownerPolicyOid();
            }
        }

        return oid;
    }

    public void ownerPolicyOid(Long ownerPolicyOid) {
        if (ownerPolicyOid != null && ownerPolicyOid == -1) ownerPolicyOid = null;
        this.ownerPolicyOid = ownerPolicyOid;
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
            final Iterator iterator;

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
         * @see Iterator#remove()
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

    /**
     * Simplify any children of this assertion.  For a leaf assertion, this takes no action.
     * For a composite assertion, this calls {@link #simplify(Assertion, boolean)} on each child reference and removes
     * any assertions that turn out empty.
     * <p/>
     * Note that this method might result in an empty composite assertion.
     * <p/>
     * To simplify this assertion itself, call the static method {@link #simplify(Assertion, boolean)} on this
     * assertion.  This can't be an instance method since it might need to change ie. a composite assertion
     * into a leaf assertion.
     */
    void simplify() {
        // Leaf assertions have nothing to do here
    }

    /**
     * Simplify the specified assertion tree.  This is a static method because it might need to change
     * ie. a composite assertion into a singleton (or even simplify a do-nothing policy right down to a null
     * policy reference!).
     * <p/>
     * The following transformations will be performed:
     * <ul>
     * <li>Empty composite assertions will be removed:
     * <pre>
     *      AND() -> null
     *      AND(foo OR() baz) -> AND(foo baz)
     * </pre>
     * <li>Singleton composites will be replaced by their single child:
     * <pre>
     *     AND(foo OR(bar) baz) -> AND(foo bar baz)
     *     AND(blat) -> blat
     * </pre>
     * <li>Nested ANDs will be eliminated:
     * <pre>
     *     AND(AND(AND(foo bar) baz) AND(blat OR(blee blah) bloo XOR(bleet bloat) zee))
     *      -> AND(foo bar baz blat OR(blee blah) bloo XOR(bleet bloat) zee)
     * </pre>
     * </ul>
     *
     * @param in   the assertion reference to simplify.  If this is null, this method will immediately return null.
     * @param recurseChildren   if true, child assertions will be recursively simplified.  Otherwise, only the
     *                          specified assertion will be simplified.
     *                          <p/>
     *                          You can gain some performance by passing recurseChildren as false when you are
     *                          building a policy from the bottom up -- this will avoid needlessly resimplifying
     *                          the already-simplified bits as you assemble the policy.
     * @return the simplified reference, possibly null if the policy contained nothing but empty composite assertions.
     */
    public static Assertion simplify(Assertion in, boolean recurseChildren) {
        while (in instanceof CompositeAssertion) {
            CompositeAssertion comp = (CompositeAssertion)in;
            List kids = comp.getChildren();
            if (kids.size() < 1)
                return null;

            if (recurseChildren) {
                for (Iterator i = kids.iterator(); i.hasNext();) {
                    Assertion assertion = (Assertion)i.next();
                    assertion.simplify();
                }
            }

            if (comp instanceof AllAssertion) {
                // Check for an All that contains only primitive assertions and other All's, and merge them
                boolean sawAll = false;
                for (Iterator i = kids.iterator(); i.hasNext();) {
                    Assertion assertion = (Assertion)i.next();
                    if (assertion instanceof AllAssertion) sawAll = true;
                }
                if (sawAll) {
                    // This All contains other Alls.  Eliminate our immediate child Alls.
                    AllAssertion old = (AllAssertion)comp.getCopy();
                    comp.clearChildren();
                    kids = old.getChildren();
                    for (Iterator i = kids.iterator(); i.hasNext();) {
                        Assertion kid = (Assertion)i.next();
                        if (kid instanceof AllAssertion) {
                            // Merge in grandkids
                            AllAssertion kidAll = (AllAssertion)kid;
                            List grandkids = kidAll.getChildren();
                            for (Iterator j = grandkids.iterator(); j.hasNext();) {
                                Assertion grandkid = (Assertion)j.next();
                                comp.addChild(grandkid);
                            }
                        } else {
                            // Merge in simple kids
                            comp.addChild(kid);
                        }
                    }
                    // Repeat the operation on the same node to see if it can be simplified some more
                    continue;
                }
            }

            if (kids.size() != 1)
                break;
            in = (Assertion)kids.get(0);
        }
        return in;
    }

    /**
     * Check if the given assertion has a child of the given type.
     *
     * @param in The assertion to check
     * @param assertionClass The type to find
     * @return true if the given assertion or one of its children is the correct type
     */
    public static boolean contains(Assertion in, Class assertionClass) {
        boolean found = false;

        if (assertionClass.isInstance(in)) {
            found = true;
        }
        else  if (in instanceof CompositeAssertion) {
            CompositeAssertion comp = (CompositeAssertion) in;
            List kids = comp.getChildren();
            for (Iterator iterator = kids.iterator(); iterator.hasNext();) {
                Assertion assertion = (Assertion) iterator.next();
                if (contains(assertion, assertionClass)) {
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    public static boolean isRequest(Assertion assertion) {
        return hasAnnotation(assertion, ProcessesRequest.class);
    }

    public static boolean isResponse(Assertion assertion) {
        return hasAnnotation(assertion, ProcessesResponse.class);
    }

    public static boolean isHardwareAccelerated(Assertion assertion) {
        return hasAnnotation(assertion, HardwareAccelerated.class);
    }

    public static boolean isMultipart(Assertion assertion) {
        return hasAnnotation(assertion, ProcessesMultipart.class);
    }

    public static boolean isWSSecurity(Assertion assertion) {
        boolean isWss = false;

        if ( assertion != null ) {
            RequiresSOAP soapAnnotation = assertion.getClass().getAnnotation(RequiresSOAP.class);
            isWss = soapAnnotation != null && soapAnnotation.wss();
        }

        return isWss;
    }

    private static boolean hasAnnotation(Assertion assertion, Class annotationClass) {
        boolean hasAnnotation = false;

        if ( assertion != null ) {
            hasAnnotation = assertion.getClass().isAnnotationPresent(annotationClass);                    
        }

        return hasAnnotation;
    }

    /**
     * Get the metadata for this assertion class.
     *
     * @return an AssertionMetadata instance.  Never null.
     */
    public AssertionMetadata meta() {
        return defaultMeta();
    }

    /**
     * Get the (possibly cached) DefaultAssertionMetadata for the current assertion class.
     *
     * @return a DefaultAssertionMetadata instance for this assertion class.  Never null.
     */
    protected final DefaultAssertionMetadata defaultMeta() {
        final String classname = getClass().getName();
        try {
            DefaultAssertionMetadata meta = (DefaultAssertionMetadata)metadataCache.get(classname);
            if (meta != null)
                return meta;
            meta = new DefaultAssertionMetadata((Assertion)getClass().newInstance());
            metadataCache.put(classname, meta);
            return meta;
        } catch (InstantiationException e) {
            throw needsMeta(classname, e);
        } catch (IllegalAccessException e) {
            throw needsMeta(classname, e);
        }
    }

    /**
     * Clear any cached metadata regarding the specified asertion class, presumably because it
     * is being deregistered.
     *
     * @param assertionClassname the name of an assertion class whose metadata to flush from the cache
     */
    public static void clearCachedMetadata(String assertionClassname) {
        metadataCache.remove(assertionClassname);
    }
    
    private RuntimeException needsMeta(String classname, Exception cause) {
        //noinspection ThrowableInstanceNeverThrown
        return new RuntimeException("Assertion class " + classname +
                                        " must either override meta() and avoid calling defaultMeta(), " +
                                        "or have a public nullary constructor",
                                    cause);
    }

    /**
     * Assertion beans must not try to override equals and hashcode --
     * identity of an assertion depends on things other than just the content of its fields (such as its position in a
     * policy or service), not just its properties.  Otherwise, for example, it would be impossible to store a set
     * of child assertions that contains two XSLT assertions that implement the same transformation.
     */
    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * Assertion beans must not try to override equals and hashcode --
     * identity of an assertion depends on things other than just the content of its fields (such as its position in a
     * policy or service), not just its properties.  Otherwise, for example, it would be impossible to store a set
     * of child assertions that contains two XSLT assertions that implement the same transformation.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Get the name of the leaf-most feature set for this assertion class.
     *
     * @return the leaf feature set name for this assertion.  Never null.
     */
    public final String getFeatureSetName() {
        // We'll take the assertion's word about its own feature set name, but only if it's claiming
        // to be a modular assertion.
        String claimed = (String)meta().get(AssertionMetadata.FEATURE_SET_NAME);
        if ("set:modularAssertions".equals(claimed))
            return claimed;

        return getDefaultFeatureSetName();
    }

    final String getDefaultFeatureSetName() {
        String fullName = getClass().getName();
        String basePackage = (String)meta().get(AssertionMetadata.BASE_PACKAGE);
        return makeDefaultFeatureSetName(fullName, basePackage);
    }

    static String makeDefaultFeatureSetName(String fullClassname, String basePackage) {
        String rest = ClassUtils.stripPrefix(fullClassname, basePackage + "."); // "com.yoyodyne.assertion.a.b.FooAssertion" => "assertion.a.b.FooAssertion"
        rest = ClassUtils.stripPrefix(rest, "policy.");
        rest = ClassUtils.stripPrefix(rest, "assertion.");                 // "assertion.a.b.FooAssertion" => "a.b.FooAssertion"
        rest = ClassUtils.stripSuffix(rest, "Assertion");
        return "assertion:" + rest;
    }

    /**
     * Get the name of the leaf feature set for the specified assertion class name.
     * For example, for "com.l7tech.policy.assertion.composite.OneOrMoreAssertion", will return
     * the string "assertion:composite.OneOrMore".
     *
     * @param assertionClass assertion class.  Required.
     * @return the leaf feature set name for this assertion.  Never null.
     */
    public static String getFeatureSetName(Class assertionClass) {
        final Assertion assertion;
        try {
            assertion = (Assertion)assertionClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e); // assertion must have default c'tor
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e); // assertion must have default c'tor
        }

        return assertion.getFeatureSetName();
    }

    /**
     * Get the local part of the specified assertion class name with all packages and any trailing "Assertion"
     * removed.  For example, for "com.l7tech.policy.assertion.composite.OneOrMoreAssertion", will return
     * the string "OneOrMore".
     *
     * @param className the assertion class name.  Should start with "com.l7tech.policy.assertion.".
     * @return the base name of this assertion, assuming the usual naming convention.
     */
    public static String getBaseName(String className) {
        if (className.endsWith("."))
            throw new IllegalArgumentException("assertionClass name ends with dot");
        int lastDot = className.lastIndexOf(".");
        String rest = lastDot < 1 ? className : className.substring(lastDot + 1);
        if (rest.endsWith("Assertion"))
            rest = rest.substring(0, rest.length() - "Assertion".length());
        return rest;
    }

    public void updateTemporaryData(Assertion assertion) {
    }
}


