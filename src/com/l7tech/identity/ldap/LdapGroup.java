package com.l7tech.identity.ldap;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LdapGroup implements Group {
    public static final int OU_GROUP = 0;
    public static final int NORMAL_GROUP = 1;

    public LdapGroup( GroupBean bean ) {
        _groupBean = bean;
    }

    public LdapGroup() {
        _groupBean = new GroupBean();
    }

    public String getDescription() {
        return _groupBean.getDescription();
    }

    public String getUniqueIdentifier() {
        return _dn;
    }

    public String getName() {
        return _groupBean.getName();
    }

    public Set getMembers() {
        try {
            return getGroupManager().getUserHeaders( this );
        } catch (FindException e) {
            throw new RuntimeException( "Couldn't get group members!", e );
        }
    }

    public Set getMemberHeaders() {
        UserManager uman = getUserManager();
        Set headers = new HashSet();
        for (Iterator i = getMembers().iterator(); i.hasNext();) {
            User user = (User) i.next();
            headers.add( uman.userToHeader( user ) );
        }
        return headers;
    }

    public void setDescription(String description) {
        _groupBean.setDescription( description );
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

    public String getDn() {
        return _dn;
    }

    public void setDn(String dn) {
        _dn = dn;
    }

    public String getCn() {
        return _groupBean.getName();
    }

    public void setCn(String cn) {
        _groupBean.setName( cn );
    }

    public String toString() {
        return "com.l7tech.identity.Group." +
                "\n\tName=" + getName() +
                "\n\tproviderId=" + providerId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LdapGroup)) return false;
        final LdapGroup groupImp = (LdapGroup) o;
        if ( providerId != groupImp.providerId ) return false;
        if ( !_dn.equals(groupImp._dn) ) return false;
        return true;
    }

    public int hashCode() {
        if ( _dn == null ) return System.identityHashCode( this );

        int hash = _dn.hashCode();
        hash += 29 * (int)providerId;

        return hash;
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom( Group objToCopy) {
        LdapGroup imp = (LdapGroup)objToCopy;
        setDn(imp.getDn());
        setCn(imp.getCn());
        setDescription(imp.getDescription());
        setProviderId(imp.getProviderId());
    }

    public GroupBean getGroupBean() {
        return _groupBean;
    }

    private GroupManager getGroupManager() {
        if ( _groupManager == null ) {
            IdentityProviderConfig config = null;
            try {
                config = getConfigManager().findByPrimaryKey( providerId );
            } catch (FindException e) {
                throw new IllegalStateException( "Group " + getName() + "'s IdentityProviderConfig (id = " + providerId + ") has ceased to exist!" );
            }
            IdentityProvider provider = IdentityProviderFactory.makeProvider( config );
            _groupManager = provider.getGroupManager();
        }
        return _groupManager;
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

    private UserManager getUserManager() {
        if ( _userManager == null ) {
            IdentityProviderConfig config = null;
            try {
                config = getConfigManager().findByPrimaryKey( providerId );
            } catch (FindException e) {
                throw new IllegalStateException( "Group " + getName() + "'s IdentityProviderConfig (id = " + providerId + ") has ceased to exist!" );
            }
            IdentityProvider provider = IdentityProviderFactory.makeProvider( config );
            _userManager = provider.getUserManager();
        }
        return _userManager;
    }

    private String _dn;

    private GroupManager _groupManager;
    private UserManager _userManager;
    private GroupBean _groupBean;

    private long providerId = IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID;
}
