package com.l7tech.identity.internal;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class InternalGroup extends NamedEntityImp implements Group {
    public InternalGroup() {
        _groupBean = new GroupBean();
    }

    public InternalGroup( GroupBean bean ) {
        _groupBean = bean;
    }

    public void setOid( long oid ) {
        super.setOid(oid);
        _groupBean.setUniqueIdentifier( Long.toString( oid ) );
    }

    public long getOid() {
        return new Long( _groupBean.getUniqueIdentifier() ).longValue();
    }

    public String getDescription() {
        return _groupBean.getDescription();
    }

    public String getName() {
        return _groupBean.getName();
    }

    public Set getMembers() {
        UserManager uman = getUserManager();
        try {
            Set out = new HashSet();
            for (Iterator i = getMemberHeaders().iterator(); i.hasNext();) {
                EntityHeader header = (EntityHeader) i.next();
                out.add( uman.findByPrimaryKey( header.getStrId() ) );
            }
            return out;
        } catch (FindException e) {
            throw new RuntimeException( "Couldn't get group members!", e );
        }
    }

    public Set getMemberHeaders() {
        GroupManager gman = getGroupManager();
        try {
            return gman.getUserHeaders( this );
        } catch (FindException e) {
            throw new RuntimeException( "Couldn't get group's members", e );
        }
    }

    public void setDescription(String description) {
        _groupBean.setDescription( description );
    }

    public void setName( String name ) {
        _groupBean.setName( name );
    }

    public String getUniqueIdentifier() {
        return new Long( _oid ).toString();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return _groupBean.getProviderId();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId ) {
        _groupBean.setProviderId( providerId );
    }

    public int getVersion() {
        return _groupBean.getVersion();
    }

    public void setVersion(int version) {
        _groupBean.setVersion(version);
    }

    public String toString() {
        return "com.l7tech.identity.Group." +
                "\n\tName=" + _name +
                "\n\tproviderId=" + _groupBean.getProviderId();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalGroup)) return false;
        final InternalGroup groupImp = (InternalGroup) o;
        if (_oid != DEFAULT_OID ? !(_oid == groupImp._oid) : groupImp._oid != DEFAULT_OID ) return false;
        return true;
    }

    public int hashCode() {
        if ( _oid != DEFAULT_OID ) return (int)_oid;
        if ( _name == null ) return System.identityHashCode(this);

        int hash = _name.hashCode();
        hash += 29 * (int)_groupBean.getProviderId();
        return hash;
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom( Group objToCopy) {
        InternalGroup imp = (InternalGroup)objToCopy;
        setOid(imp.getOid());
        setDescription(imp.getDescription());
        setName(imp.getName());
        setProviderId(imp.getProviderId());
    }

    public GroupBean getGroupBean() {
        return _groupBean;
    }

    private GroupManager getGroupManager() {
        if ( _groupManager == null ) {
            IdentityProviderConfig config = null;
            long providerId = _groupBean.getProviderId();
            try {
                config = getConfigManager().findByPrimaryKey( providerId );
            } catch (FindException e) {
                throw new IllegalStateException( "Group " + this._name + "'s IdentityProviderConfig (id = " + providerId + ") has ceased to exist!" );
            }
            IdentityProvider provider = IdentityProviderFactory.makeProvider( config );
            _groupManager = provider.getGroupManager();
        }
        return _groupManager;
    }

    private UserManager getUserManager() {
        if ( _userManager == null ) {
            IdentityProviderConfig config = null;
            long providerId = _groupBean.getProviderId();
            try {
                config = getConfigManager().findByPrimaryKey( providerId );
            } catch (FindException e) {
                throw new IllegalStateException( "Group " + this._name + "'s IdentityProviderConfig (id = " + providerId + ") has ceased to exist!" );
            }
            IdentityProvider provider = IdentityProviderFactory.makeProvider( config );
            _userManager = provider.getUserManager();
        }
        return _userManager;
    }

    private IdentityProviderConfigManager getConfigManager() {
        IdentityProviderConfigManager ipc =
          (IdentityProviderConfigManager)Locator.
          getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find " + IdentityProviderConfigManager.class);
        }
        return ipc;
    }

    private GroupBean _groupBean;
    private GroupManager _groupManager;
    private UserManager _userManager;
}
