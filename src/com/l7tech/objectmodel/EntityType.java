package com.l7tech.objectmodel;

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
    public static final EntityType JMS_CONNECTION = new EntityType(5);
    public static final EntityType JMS_ENDPOINT = new EntityType(6);
    public static final EntityType TRUSTED_CERT = new EntityType(7);
    public static final EntityType ALERT_TRIGGER = new EntityType(8);
    public static final EntityType ALERT_ACTION = new EntityType(9);
    public static final EntityType SAMPLE_MESSAGE = new EntityType(10);
    public static final EntityType MAXED_OUT_SEARCH_RESULT = new EntityType(11);
    public static final EntityType RBAC_ROLE = new EntityType(12);
    public static final EntityType UNDEFINED = new EntityType(-1);

    private static final long serialVersionUID = -5485680679515491927L;

    private int val;

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
                return "JMS_CONNECTION";
            case 6:
                return "JMS_ENDPOINT";
            case 7:
                return "TRUSTED_CERT";
            case 8:
                return "ALERT_TRIGGER";
            case 9:
                return "ALERT_ACTION";
            case 10:
                return "SAMPLE_MESSAGE";
            case 11:
                return "MAXED_OUT_SEARCH_RESULT";
            case 12:
                return "RBAC_ROLE";
            default:
                return "?";
        }
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
                return JMS_CONNECTION;
            case 6:
                return JMS_ENDPOINT;
            case 7:
                return TRUSTED_CERT;
            case 8:
                return ALERT_TRIGGER;
            case 9:
                return ALERT_ACTION;
            case 10:
                return SAMPLE_MESSAGE;
            case 11:
                return MAXED_OUT_SEARCH_RESULT;
            case 12:
                return RBAC_ROLE;
            default:
                return UNDEFINED;
        }
    }

}
