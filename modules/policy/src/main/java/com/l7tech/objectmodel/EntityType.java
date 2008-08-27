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
    public static final EntityType ID_PROVIDER_CONFIG = new EntityType(1, "Identity Provider");
    public static final EntityType USER = new EntityType(2, "User");
    public static final EntityType GROUP = new EntityType(3, "Group");
    public static final EntityType SERVICE = new EntityType(4, "Published Service");
    public static final EntityType JMS_CONNECTION = new EntityType(5, "JMS Connection");
    public static final EntityType JMS_ENDPOINT = new EntityType(6, "JMS Endpoint");
    public static final EntityType TRUSTED_CERT = new EntityType(7, "Trusted Certificate");
    public static final EntityType ALERT_TRIGGER = new EntityType(8, "Alert Trigger");
    public static final EntityType ALERT_ACTION = new EntityType(9, "Alert Action");
    public static final EntityType SAMPLE_MESSAGE = new EntityType(10, "Sample Message");
    public static final EntityType MAXED_OUT_SEARCH_RESULT = new EntityType(11, "Exceeded maximum search result size");
    public static final EntityType RBAC_ROLE = new EntityType(12, "Role");
    public static final EntityType ATTRIBUTE_CONFIG = new EntityType(13, "Attribute Config");
    public static final EntityType SCHEMA_ENTRY = new EntityType(14, "Schema Entry");
    public static final EntityType PRIVATE_KEY = new EntityType(15, "Private Key");
    public static final EntityType REVOCATION_CHECK_POLICY = new EntityType(16, "Revocation Check Policy");
    public static final EntityType CONNECTOR = new EntityType(17, "HTTP(S) Listen Port");
    public static final EntityType POLICY = new EntityType(18, "Policy");
    public static final EntityType POLICY_VERSION = new EntityType(19, "Policy Version");
    public static final EntityType FOLDER = new EntityType(21, "Folder");
    public static final EntityType SERVICE_ALIAS = new EntityType(22, "Published Service Alias");
    public static final EntityType POLICY_ALIAS = new EntityType(23, "Policy Alias");
    public static final EntityType UNDEFINED = new EntityType(-1, "Undefined");

    private static final long serialVersionUID = -5485680679515491927L;

    private int val;
    private String name;

    public EntityType(int num, String name) {
        this.val = num;
        this.name = name;
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

    public String getName() {
        return name;
    }

    /**
     * this is exposed for facilitating the serialization of thi enum-type class
     */
    public void setName(String name) {
        this.name = name;
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
            case 13:
                return "ATTRIBUTE_CONFIG";
            case 14:
                return "SCHEMA_ENTRY";
            case 15:
                return "PRIVATE_KEY";
            case 16:
                return "REVOCATION_CHECK_POLICY";
            case 17:
                return "CONNECTOR";
            case 18:
 	 	        return "POLICY";
            case 21:
                return "FOLDER";
            case 22:
                return "SERVICE_ALIAS";
            case 23:
                return "POLICY_ALIAS";

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
            case 13:
                return ATTRIBUTE_CONFIG;
            case 14:
                return SCHEMA_ENTRY;
            case 15:
                return PRIVATE_KEY;
            case 16:
                return REVOCATION_CHECK_POLICY;
            case 17:
                return CONNECTOR;
            case 18:
 	 	        return POLICY;
            case 19:
                return POLICY_VERSION;
            case 21:
                return FOLDER;
            case 22:
                return SERVICE_ALIAS;
            case 23:
                return POLICY_ALIAS;            
            default:
                return UNDEFINED;
        }
    }

}
