package com.l7tech.identity;

import java.security.Principal;

public class GroupBean implements Principal {
    public String getUniqueIdentifier() {
        return _uniqueId;
    }

    public void setUniqueIdentifier( String uid ) {
        _uniqueId = uid;
    }

    public String getDescription() {
        return _description;
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
}
