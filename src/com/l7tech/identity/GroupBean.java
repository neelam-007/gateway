package com.l7tech.identity;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

public class GroupBean implements Group, Serializable {
    public GroupBean(long providerId, String _name) {
        this.providerId = providerId;
        this._name = _name;
    }

    public GroupBean() {
    }

    public String getId() {
        return _uniqueId;
    }

    public void setUniqueIdentifier( String uid ) {
        _uniqueId = uid;
    }

    public String getDescription() {
        return _description;
    }

    public GroupBean getGroupBean() {
        return this;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public int getVersion() {
        return _version;
    }

    public void setVersion( int version ) {
        _version = version;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return providerId;
    }

    public Map getProperties() {
        if ( properties == null ) properties = new HashMap();
        return properties;
    }

    public void setProperties( Map properties ) {
        this.properties = properties;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupBean)) return false;

        final GroupBean groupBean = (GroupBean)o;

        if (providerId != groupBean.providerId) return false;
        if (_uniqueId != null ? !_uniqueId.equals(groupBean._uniqueId) : groupBean._uniqueId != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (_uniqueId != null ? _uniqueId.hashCode() : 0);
        result = 29 * result + (int)(providerId ^ (providerId >>> 32));
        return result;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId ) {
        this.providerId = providerId;
    }

    private static final long serialVersionUID = -2260828785148311161L;

    private String _uniqueId;
    private String _name;
    private String _description;
    private long providerId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
    private Map properties;
    private int _version;
}
