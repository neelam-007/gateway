package com.l7tech.objectmodel;

import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.service.PublishedService;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Type of entity represented by an EntityHeader.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: May 26, 2003
 *
 */
public class EntityType implements Serializable {
    public static final EntityType ID_PROVIDER_CONFIG = new EntityType(1);
    public static final EntityType USER = new EntityType(2);
    public static final EntityType GROUP = new EntityType(3);
    public static final EntityType SERVICE = new EntityType(4);
    public static final EntityType JMS_PROVIDER = new EntityType(5);
    public static final EntityType UNDEFINED = new EntityType(-1);

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

    public String toString() {
        switch (val) {
            case 1:
                return "ID_PROVIDER_CONFIG";
            case 2:
                return "USER";
            case 3:
                return "GROUP";
            case 4:
                return "SERVICE";
            case 5:
                return "JMS_PROVIDER";
            default:
                return "?";
        }
    }

    public static EntityType fromInterface(Class interfaceType) throws IllegalArgumentException {
        if (interfaceType.equals(IdentityProviderConfig.class)) return ID_PROVIDER_CONFIG;
        else if (interfaceType.equals(User.class)) return USER;
        else if (interfaceType.equals(Group.class)) return GROUP;
        else if (interfaceType.equals(PublishedService.class)) return SERVICE;
        else if (interfaceType.equals(JmsProvider.class)) return JMS_PROVIDER;
        throw new IllegalArgumentException("no EntityType for interface " + interfaceType.getName());
    }

    private Object readResolve() throws ObjectStreamException {
        return fromValue(val);
    }

    /**
     * necessary for use in web service where those are constructed from value
     */ 
    public static EntityType fromValue(int value) {
        switch (value) {
            case 1:
                return ID_PROVIDER_CONFIG;
            case 2:
                return USER;
            case 3:
                return GROUP;
            case 4:
                return SERVICE;
            case 5:
                return JMS_PROVIDER;
            default:
                return UNDEFINED;
        }
    }

    private int val;
}
