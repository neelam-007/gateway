package com.l7tech.objectmodel;

import java.util.Hashtable;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 26, 2003
 *
 */
public class EntityType {
    public static final EntityType ID_PROVIDER_CONFIG = new EntityType(1);
    public static final EntityType USER = new EntityType(2);
    public static final EntityType GROUP = new EntityType(3);
    public static final EntityType SERVICE = new EntityType(4);
    public static final EntityType UNDEFINED = new EntityType(-1);

    public static EntityType fromInterface(Class interfaceType) throws IllegalArgumentException {
        if (interfaceType.equals(com.l7tech.identity.IdentityProviderConfig.class)) return ID_PROVIDER_CONFIG;
        else if (interfaceType.equals(com.l7tech.identity.User.class)) return USER;
        else if (interfaceType.equals(com.l7tech.identity.Group.class)) return GROUP;
        else if (interfaceType.equals(com.l7tech.service.PublishedService.class)) return SERVICE;
        throw new IllegalArgumentException("no EntityType for interface " + interfaceType.getName());
    }

    /**
     * Returns a hash code value for the object.
     * The method is implemented to satisfy general contract of <code>hashCode</code>
     * and <code>equals</code>.
     *
     * @return  a hash code value for this object.
     * @see     Object#equals(Object)
     */
    public int hashCode() {
        return val;
    }


    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param   that   the reference object with which to compare.
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     * @see     #hashCode()
     */
    public boolean equals(java.lang.Object that) {
        if (that == this) return true;
        if (!(that instanceof EntityType)) return false;

        return this.hashCode() == that.hashCode();
    }

    /**
     * this constructor is provided to maintain serializablility use
     * the static values instead
     */
    public EntityType() {
        val = -1;
    }

    private EntityType(int val) {
        this.val = val;
    }

    /**
     * this is exposed for facilitating the serialization of thi enum-type class
     */
    public int getVal() {
        return val;
    }

    /**
     * this is exposed for facilitating the serialization of thi enum-type class
     */
    public void setVal(int val) {
        this.val = val;
    }

    private int val;
}
