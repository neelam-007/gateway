/*
 * $Id$
 */
package com.l7tech.objectmodel;

import java.security.Permission;
import java.security.PermissionCollection;

/**
 * This <code>Permission</code> subclass represents access control for objects.
 * The ObjectPermission consists of an object and a set of "actions"
 * as bitmask specifying ways to operate on that object.
 * The possible actions are are
 * <pre>
 *    ObjectPermission.READ
 *    ObjectPermission.ADD
 *    ObjectPermission.WRITE
 *    ObjectPermission.DELETE
 *    ObjectPermission.OWNER
 * </pre>
 * <p/>
 * Read, Write and Delete are discrete permissions, that
 * is, they don't imply each other. Owner permission implies
 * all permissions.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @see java.security.Permission
 */
public final class ObjectPermission extends Permission {

    /**
     * Read an object
     */
    public final static int READ = 0x1;

    /**
     * Add an object
     */
    public final static int ADD = 0x2;

    /**
     * Write to an object
     */
    public final static int WRITE = 0x4;

    /**
     * Delete an object
     */
    public final static int DELETE = 0x8;

    /**
     * Object owner
     */
    public final static int OWNER = 0x10;

    /**
     * All actions
     */
    public final static int ALL = READ | ADD | WRITE | DELETE | OWNER;

    // the actions mask
    private int mask;

    // the object
    private Object target;

    /**
     * the actions string.
     */
    private String actions; // lazt init


    /**
     * Creates a new ObjectPermission object with the specified actions
     * on the given object.
     * <p/>
     * <p/>
     * The <i>action</i> parameter contains action bitmask.
     * <p/>
     * Possible actions are
     * <pre>
     *    ObjectPermission.READ
     *    ObjectPermission.ADD
     *    ObjectPermission.WRITE
     *    ObjectPermission.DELETE
     *    ObjectPermission,OWNER
     * </pre>
     * <p/>
     * The first three are discrete permission while the "owner" automatically
     * adds the first three.
     * <p/>
     * Examples of ObjectPermission instantiation are the following:
     * <pre>
     *    ep = new ObjectPermission(object, ObjectPermission.READ);
     *    ep = new ObjectPermission(object,
     *                                 ObjectPermission.READ |
     *                                 ObjectPermission.DELETE);
     *    dp = new ObjectPermission(object, ObjectPermission,OWNER);
     * </pre>
     *
     * @param object the target object
     * @param mask   the action bitmask.
     * @throws IllegalArgumentException throw on invalid permission mask or <b>null</b> object
     */
    public ObjectPermission(Object object, int mask)
      throws IllegalArgumentException {
        super("object : " + object);
        if (object == null) {
            throw new IllegalArgumentException();
        }
        // Set the integer mask that represents the actions
        if ((mask & ALL) != mask || mask == 0)
            throw new IllegalArgumentException("invalid actions mask");

        this.target = object;
        this.mask = mask;
    }


    /**
     * @return the target object for this permission.
     */
    public Object getTarget() {
        return target;
    }

    /**
     * Checks if this permission object "implies" the specified permission.
     * <P>
     * More specifically, this method first ensures that all of the following
     * are true (and returns false if any of them are not):<p>
     * <ul>
     * <li> <i>p</i> is an instanceof DirectoryPermission,<p>
     * <li> <i>p</i>'s actions are a proper subset of this
     * object's actions<p>
     * <p/>
     * If none of the above are true, <code>implies</code> returns false.
     *
     * @param p the permission to check against.
     * @return true if the specified permission is implied by this object,
     *         false if not.
     */
    public boolean implies(Permission p) {

        if (!(p instanceof ObjectPermission))
            return false;

        ObjectPermission that = (ObjectPermission)p;

        if ((OWNER & this.mask) == OWNER) {
            return true;
        }

        return ((this.mask & that.mask) == that.mask);
    }



    /**
     * Return the current action mask.
     *
     * @return the actions mask.
     */
    public int getMask() {
        return mask;
    }

    /**
     * Returns the "canonical string representation" of the actions in the
     * specified mask.
     *
     * @param mask a specific integer action mask to translate into a string
     * @return the canonical string representation of the actions
     */
    private String getActions(int mask) {
        StringBuffer sb = new StringBuffer();
        boolean comma = false;

        if ((mask & READ) == READ) {
            comma = true;
            sb.append("read");
        }

        if ((mask & ADD) == ADD) {
            if (comma)
                sb.append(',');
            else
                comma = true;
            sb.append("add");
        }

        if ((mask & WRITE) == WRITE) {
            if (comma)
                sb.append(',');
            else
                comma = true;
            sb.append("write");
        }

        if ((mask & DELETE) == DELETE) {
            if (comma)
                sb.append(',');
            else
                comma = true;
            sb.append("delete");
        }


        if ((mask & OWNER) == OWNER) {
            if (comma)
                sb.append(',');
            else
                comma = true;
            sb.append("owner");
        }

        return sb.toString();
    }

    /**
     * Returns the canonical string representation of the actions.
     *
     * @return the canonical string representation of the actions.
     */
    public String getActions() {
        if (actions == null)
            actions = getActions(this.mask);

        return actions;
    }

    /**
     * Returns null, not implemented
     *
     * @return a null PermissionCollection object
     */
    public PermissionCollection newPermissionCollection() {
        return null;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectPermission)) return false;

        final ObjectPermission objectPermission = (ObjectPermission)o;

        if (mask != objectPermission.mask) return false;
        if (!target.equals(objectPermission.target)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = mask;
        result = 29 * result + target.hashCode();
        return result;
    }
}
