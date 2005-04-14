package com.l7tech.objectmodel;

import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.alert.AlertEvent;
import com.l7tech.common.alert.Notification;
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
    public static final EntityType ID_PROVIDER_CONFIG = new EntityType(1, IdentityProviderConfig.class);
    public static final EntityType USER = new EntityType(2, User.class);
    public static final EntityType GROUP = new EntityType(3, Group.class);
    public static final EntityType SERVICE = new EntityType(4, PublishedService.class);
    public static final EntityType JMS_CONNECTION = new EntityType(5, JmsConnection.class);
    public static final EntityType JMS_ENDPOINT = new EntityType(6, JmsEndpoint.class);
    public static final EntityType TRUSTED_CERT = new EntityType(7, TrustedCert.class);
    public static final EntityType ALERT_TRIGGER = new EntityType(8, AlertEvent.class);
    public static final EntityType ALERT_ACTION = new EntityType(9, Notification.class);
    public static final EntityType MAXED_OUT_SEARCH_RESULT = new EntityType(10, String.class);
    public static final EntityType UNDEFINED = new EntityType(-1, null);

    private int val;
    private Class entityClass;

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

    private EntityType(int val, Class clazz) {
        this.val = val;
        this.entityClass = clazz;
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

    public Class getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class entityClass) {
        this.entityClass = entityClass;
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
                return "MAXED_OUT_SEARCH_RESULT";
            default:
                return "?";
        }
    }

    public static EntityType fromInterface(Class interfaceType) throws IllegalArgumentException {
        if (interfaceType.equals(IdentityProviderConfig.class)) return ID_PROVIDER_CONFIG;
        else if (interfaceType.equals(User.class)) return USER;
        else if (interfaceType.equals(Group.class)) return GROUP;
        else if (interfaceType.equals(PublishedService.class)) return SERVICE;
        else if (interfaceType.equals(JmsConnection.class)) return JMS_CONNECTION;
        else if (interfaceType.equals(JmsEndpoint.class)) return JMS_ENDPOINT;
        else if (interfaceType.equals(TrustedCert.class)) return TRUSTED_CERT;
        else if (interfaceType.equals(AlertEvent.class)) return ALERT_TRIGGER;
        else if (interfaceType.equals(Notification.class)) return ALERT_ACTION;
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
                return MAXED_OUT_SEARCH_RESULT;
            default:
                return UNDEFINED;
        }
    }

}
