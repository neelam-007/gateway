package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;

import java.io.Serializable;

/**
 * Class AssertionPath represents a sequence of <code>Assertion</code>
 * instances.
 * The elements of the path are ordered such that the root is always the
 * first element.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AssertionPath implements Serializable {
    /**
     * Path representing the parent, null if lastPathComponent represents
     * the root.
     * */
    private AssertionPath parentPath;
    /** Last path assertion. */
    private Assertion lastPathComponent;
    private int pathOrder;

    /**
     * Construct the new assertion path from the given <code>Assertion</code>
     * array representing assertion path.
     *
     * @param assertions the assertion path as array
     */
    public AssertionPath(Assertion[] assertions) {
        if (assertions == null || assertions.length == 0)
            throw new IllegalArgumentException();

        lastPathComponent = assertions[assertions.length - 1];
        if (assertions.length > 1)
            parentPath = new AssertionPath(assertions, assertions.length - 1);
    }

    /**
     * Constructs a AssertionPath containing only a single element.
     * This is usually used to construct a AssertionPath for the the
     * root.
     * <p>
     * @param singlePath  an Object representing the path to a node
     * @see #AssertionPath(Assertion[])
     */
    public AssertionPath(Assertion singlePath) {
        if (singlePath == null)
            throw new IllegalArgumentException("path in AssertionPath must be non null.");
        lastPathComponent = singlePath;
        parentPath = null;
    }

    /** copy constructor */
    public AssertionPath(AssertionPath ap) {
        this.parentPath = ap.parentPath;
        this.lastPathComponent = ap.lastPathComponent;
    }

    /**
     * Constructs a new AssertionPath with the identified path components of
     * length <code>length</code>.
     */
    protected AssertionPath(Assertion[] assertionPath, int length) {
        lastPathComponent = assertionPath[length - 1];
        if (length > 1)
            parentPath = new AssertionPath(assertionPath, length - 1);
    }

    /**
     * Returns a new path containing all the elements of this object
     * plus <code>child</code>. <code>child</code> will be the last
     * element of the newly created AssertionPath.
     *
     * @param child the child assertion
     * @return the assertion path ending with the child
     * @throws NullPointerException thrown if child is <b>null</b>
     */
    public AssertionPath addAssertion(Assertion child)
      throws NullPointerException {
        AssertionPath ap = new AssertionPath(this);
        ap.parentPath = this;
        ap.lastPathComponent = child;
        return ap;
    }

    /**
     * Returns true if <code>assertionPath</code> is a descendant of this
     * AssertionPath. A AssertionPath P1 is a descendent of a AssertioPath
     * P2
     * if P1 contains all of the components that make up
     * P2's path.
     * For example, if this object has the path [a, b],
     * and <code>assertionPath</code> has the path [a, b, c],
     * then <code>assertionPath</code> is a descendant of this object.
     * However, if <code>assertionPath</code> has the path [a],
     * then it is not a descendant of this object.
     *
     * @return true if <code>assertionPath</code> is a descendant of this path
     */
    public boolean isDescendant(AssertionPath assertionPath) {
        if (assertionPath == null) return false;

        if (assertionPath == this)
            return true;

        int pathLength = getPathCount();
        int oPathLength = assertionPath.getPathCount();

        if (oPathLength < pathLength)
        // Can't be a descendant, has fewer components in the path.
            return false;
        while (oPathLength-- > pathLength)
            assertionPath = assertionPath.getParentPath();
        return equals(assertionPath);

    }

    /**
     * @return a path containing all the elements of this object,
     * except the last path component.
     */
    public AssertionPath getParentPath() {
        return parentPath;
    }

    /**
     * Returns the number of elements in the path.
     *
     * @return the int representing the path count
     */
    public int getPathCount() {
        int result = 0;
        for (AssertionPath path = this; path != null; path = path.parentPath) {
            result++;
        }
        return result;
    }

    /**
     * Returns the path assertion  at the specified index.
     *
     * @param element  an int specifying an assertion in the path, where
     *                 0 is the first element in the path
     * @return the Assertion at that index location
     * @throws IllegalArgumentException if the index is beyond the length
     *         of the path
     * @see #AssertionPath(Assertion[])
     */
    public Assertion getPathAssertion(int element) {
        int pathLength = getPathCount();

        if (element < 0 || element >= pathLength)
            throw new IndexOutOfBoundsException("Index " + element + " is out of the specified range");

        AssertionPath path = this;

        for (int i = pathLength - 1; i != element; i--) {
            path = path.parentPath;
        }
        return path.lastPathComponent;
    }

    /**
     * Returns the last assertion in the path
     *
     * @return the <code>Assertion</code> representing
     * the path count
     */
    public Assertion lastAssertion() {
        return lastPathComponent;
    }

    /**
     * Returns an ordered array of Assertions containing the components
     * of this  <code>AssertionPath</code>.
     * The first element (index 0) is the root.
     *
     * @return an array of Objects representing the AssertionPath
     * @see #AssertionPath(Assertion[])
     */
    public Assertion[] getPath() {
        int i = getPathCount();
        Assertion[] result = new Assertion[i--];

        for (AssertionPath path = this; path != null; path = path.parentPath) {
            result[i--] = path.lastPathComponent;
        }
        return result;
    }

    /**
     * Returns true if this set contains the specified <code>Assertion</code>.
     * <p>
     * @param a the assertion to test the presence of
     * @return true if this path contains the assertion, false otheriwse
     */
    public boolean contains(Assertion a) {
        if (a == null) return false;
        Assertion[] path = getPath();
        for (int i = 0; i < path.length; i++) {
           Assertion assertion = path[i];
           if (a.equals(assertion)) return true;
       }
        return false;
    }

    /**
     * Tests two AssertionPaths for equality by checking each element of
     * the paths for equality. Two paths are considered equal if they are
     * of the same length, and contain the same elements (<code>.equals</code>).
     *
     * @param o the Object to compare
     */
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this)
            return true;
        if (o instanceof AssertionPath) {
            AssertionPath oAssertionPath = (AssertionPath)o;

            if (getPathCount() != oAssertionPath.getPathCount())
                return false;

            for (AssertionPath apath = this; apath != null; apath = apath.parentPath) {
                if (!(apath.lastPathComponent.equals(oAssertionPath.lastPathComponent))) {
                    return false;
                }
                oAssertionPath = oAssertionPath.parentPath;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the hashCode for the object. The hash code of a AssertionPath
     * is defined to be the hash code of the last component in the path.
     *
     * @return the hashCode for the object
     */
    public int hashCode() {
        return lastPathComponent.hashCode();
    }

    /**
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer tempSpot = new StringBuffer("[");

        for (int counter = 0,
          maxCounter = getPathCount(); counter < maxCounter; counter++) {
            if (counter > 0)
                tempSpot.append(", ");
            tempSpot.append(getPathAssertion(counter).getClass());
        }
        tempSpot.append("]");
        return tempSpot.toString();
    }

    public int getPathOrder() {
        return pathOrder;
    }

    public void setPathOrder(int pathOrder) {
        this.pathOrder = pathOrder;
    }
}
