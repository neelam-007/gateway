package com.l7tech.objectmodel;

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
    public static final EntityType UNDEFINED = new EntityType(-1);

    public static EntityType fromInterface(Class interfaceType) throws IllegalArgumentException {
        if (interfaceType.equals(com.l7tech.identity.IdentityProviderConfig.class)) return ID_PROVIDER_CONFIG;
        else if (interfaceType.equals(com.l7tech.identity.User.class)) return USER;
        else if (interfaceType.equals(com.l7tech.identity.Group.class)) return GROUP;
        throw new IllegalArgumentException("no EntityType for interface " + interfaceType.getName());
    }

    public boolean equals(java.lang.Object obj) {
        return ((EntityType)obj).val == val;
    }

    public int hashCode() {
        return val;
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
