package com.l7tech.identity;

import java.util.Set;
import java.io.Serializable;

public class GroupBean implements Group, Serializable {
    public String getUniqueIdentifier() {
        return _uniqueId;
    }

    public void setUniqueIdentifier( String uid ) {
        _uniqueId = uid;
    }

    public String getDescription() {
        return _description;
    }

    public Set getMembers() {
        throw new UnsupportedOperationException();
    }

    public Set getMemberHeaders() {
        throw new UnsupportedOperationException();
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

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId ) {
        this.providerId = providerId;
    }

    private String _uniqueId;
    private String _name;
    private String _description;
    private long providerId = IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID;
    private int _version;
}
