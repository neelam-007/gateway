package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.Assertion;

import java.io.Serializable;
import java.util.*;

/**
 * @author alex
 */
public abstract class CompositeAssertion extends Assertion implements Cloneable, Serializable {
    private List<Assertion> children = new ArrayList<Assertion>();
    private transient List<Assertion> lockedChildren = null;

    public CompositeAssertion() {
    }

    /**
     * Create a new CompositeAssertion with no parent and the specified children.
     * The children will be copied, and each of their parents reset to point to us.
     * @param children the children to adopt.  May be empty but never null.
     */
    public CompositeAssertion( List<? extends Assertion> children ) {
        setChildren(children);
    }

    /**
     * Clone this composite and all it's children.
     * The clone will have copies of the children but no parent.
     */
    @Override
    public Object clone() {
        CompositeAssertion n = (CompositeAssertion) super.clone();
        n.setChildren(copyAndReparentChildren(n, children));
        return n;
    }

    public Iterator<Assertion> children() {
        return Collections.unmodifiableList( children ).iterator();
    }

    /**
     * Return the list of children. Never null
     * @return List of child assertions
     */
    public List<Assertion> getChildren() {
        return lockedChildren != null ? lockedChildren : children;
    }

    public void clearChildren() {
        checkLocked();
        children.clear();
    }

    public void addChild(Assertion kid) {
        checkLocked();
        children.add(kid);
        setParent(kid, this);        
        super.treeChanged();
    }

    public void addChild(int index, Assertion kid) {
        checkLocked();
        children.add(index, kid);
        setParent(kid, this);
        super.treeChanged();
    }

    public void replaceChild(Assertion oldKid, Assertion newKid) {
        checkLocked();
        int index = children.indexOf(oldKid);
        if (index >= 0) {
            children.remove(index);
            children.add(index, newKid);
            setParent(newKid, this);
            setParent(oldKid, null);
            super.treeChanged();
        }
    }

    public void removeChild(Assertion kid) {
        checkLocked();
        children.remove(kid);
        super.treeChanged();
    }

    public void setChildren(List<? extends Assertion> children) {
        checkLocked();
        this.children = reparentedChildren(this, children);
        super.treeChanged();
    }

    @Override
    public void treeChanged() {
        for ( Assertion kid : children ) {
            if ( kid.getParent() != this )
                setParent( kid, this );
        }
        super.treeChanged();
    }

    @Override
    protected int renumber(int newStartingOrdinal) {
        int n = super.renumber(newStartingOrdinal);
        for ( Assertion kid : children ) {
            if ( kid.getParent() != this )
                setParent( kid, this );
            n = renumber( kid, n );
        }
        return n;
    }

    @Override
    public Assertion getAssertionWithOrdinal(int ordinal) {
        if (getOrdinal() == ordinal)
            return this;
        for ( Assertion kid : children ) {
            Assertion kidResult = kid.getAssertionWithOrdinal( ordinal );
            if ( kidResult != null )
                return kidResult;
        }
        return null;
    }

    /**
     * Check if this composite assertion currently has any children.
     *
     * <p>Empty composite assertions may always fail.</p>
     *
     * @return true if this composite assertion lacks children and hence may fail at runtime
     * @see #permitsEmpty()
     */
    public boolean isEmpty() {
        return children.isEmpty();
    }

    /**
     * Can this assertion succeed when empty?
     *
     * @return True if this assertion can succeed, false if it will always fail when empty.
     */
    public boolean permitsEmpty() {
        return false;
    }

    /**
     * Copy children into a new list.  The new copies will each have their parent set to point to us.
     * @param newParent     What to use for the parent of the new list of children.
     * @param children      The children to copy and reparent.
     * @return              The copied and reparented list.
     */
    private List<Assertion> copyAndReparentChildren(CompositeAssertion newParent, List<Assertion> children) {
        List<Assertion> newKids = new LinkedList<Assertion>();
        for ( final Assertion aChildren : children ) {
            Assertion child = aChildren.getCopy();
            setParent( child, newParent );
            newKids.add( child );
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
    private List<Assertion> reparentedChildren(CompositeAssertion newParent, List<? extends Assertion> children) {
        List<Assertion> newKids = new LinkedList<Assertion>();
        for ( Assertion child : children ) {
            setParent( child, newParent );
            newKids.add( child );
        }
        return newKids;
    }

    @Override
    public String toIndentedString(int indentLevel) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < indentLevel; ++i)
            b.append("  ");
        b.append(super.toString());
        b.append(":\n");
        for ( Assertion a : children ) {
            b.append( a.toIndentedString( indentLevel + 1 ) );
        }
        return b.toString();
    }

    @Override
    public String toString() {
        return toIndentedString(0);
    }

    @Override
    public void lock() {
        for (Assertion child : children) {
            child.lock();
        }
        lockedChildren = Collections.unmodifiableList(children);
        super.lock();
    }
}
