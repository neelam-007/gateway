package com.l7tech.console.event;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;

import java.util.EventObject;

/**
 * Class <code>PolicyEvent</code> encapsulates information describing changes
 * to a policy assertion tree and used to notify the listeners of the change.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyEvent extends EventObject {
    /** Path to the parent of the assertions that have changed. */
    protected AssertionPath path;
    /** Indices identifying the position of where the children were. */
    protected int[] childIndices;
    /** Children that have been removed. */
    protected Object[] children;

    /**
     * Used to create an event when nodes have been changed, inserted, or
     * removed, identifying the path to the parent of the modified items as
     * a AssertionPath object.
     *
     * @param source the Object responsible for generating the event (typically
     *               the creator of the event object passes <code>this</code>
     *               for its value)
     * @param path   a AssertionPath object that identifies the path to the
     *               parent of the modified item(s)
     * @param childIndices an array of <code>int</code> that specifies the
     *               index values of the modified items
     * @param children an array of Assertions containing the inserted, removed, or
     *               changed objects
     */
    public PolicyEvent(Object source, AssertionPath path,
                       int[] childIndices, Assertion[] children) {
        super(source);
        this.path = path;
        this.childIndices = childIndices;
        this.children = children;
    }

    /**
     * For all events, except policyStructureChanged, returns the parent
     * of the changed nodes.
     * For policyStructureChanged events, returns the ancestor of the
     * structure that has changed. This and <code>getChildIndices</code>
     * are used to get a list of the effected nodes.
     * <p>
     * The one exception to this is a assertionsChanged event that is to
     * identify the root, in which case this will return the root
     * and <code>getChildIndices</code> will return null.
     *
     * @return the AssertionPath used in identifying the changed assertions.
     */
    public AssertionPath getAssertionPath() {
        return path;
    }

    /**
     * Convenience method to get the array of objects from the AssertionPath
     * instance that this event wraps.
     *
     * @return an array of Assertions, where the first Assertion is the one
     *         stored at the root and the last object is the one
     *         stored at the node identified by the path
     */
    public Assertion[] getPath() {
        if (path != null)
            return path.getPath();
        return null;
    }

    /**
     * Returns the assertiosn that are children of the node identified by
     * <code>getPath</code> at the locations specified by
     * <code>getChildIndices</code>. If this is a removal event the
     * returned objects are no longer children of the parent node.
     *
     * @return an array of Assertions containing the children specified by
     *         the event
     * @see #getPath
     * @see #getChildIndices
     */
    public Assertion[] getChildren() {
        if (children != null) {
            int cCount = children.length;
            Assertion[] retChildren = new Assertion[cCount];

            System.arraycopy(children, 0, retChildren, 0, cCount);
            return retChildren;
        }
        return null;
    }

    /**
     * Returns the values of the child indexes. If this is a removal event
     * the indexes point to locations in the initial list where items
     * were removed. If it is an insert, the indices point to locations
     * in the final list where the items were added. For node changes,
     * the indices point to the locations of the modified nodes.
     *
     * @return an array of <code>int</code> containing index locations for
     *         the children specified by the event
     */
    public int[] getChildIndices() {
        if (childIndices != null) {
            int cCount = childIndices.length;
            int[] retArray = new int[cCount];

            System.arraycopy(childIndices, 0, retArray, 0, cCount);
            return retArray;
        }
        return null;
    }

    /**
     * Returns a string that displays and identifies this object's
     * properties.
     *
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer retBuffer = new StringBuffer();

        retBuffer.append(getClass().getName() + " " +
          Integer.toString(hashCode()));
        if (path != null)
            retBuffer.append(" path " + path);
        if (childIndices != null) {
            retBuffer.append(" indices [ ");
            for (int counter = 0; counter < childIndices.length; counter++)
                retBuffer.append(Integer.toString(childIndices[counter]) + " ");
            retBuffer.append("]");
        }
        if (children != null) {
            retBuffer.append(" children [ ");
            for (int counter = 0; counter < children.length; counter++)
                retBuffer.append(children[counter] + " ");
            retBuffer.append("]");
        }
        return retBuffer.toString();
    }
}
