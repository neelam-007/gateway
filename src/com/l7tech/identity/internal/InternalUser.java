package com.l7tech.identity.internal;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 * User from the internal identity provider
 * Password property is stored as HEX(MD5(login:L7SSGDigestRealm:password)). If you pass a clear text passwd in
 * setPassword, this encoding will be done ofr you (provided that login was set before).
 */
public class InternalUser extends NamedEntityImp implements User {
    public InternalUser( UserBean bean ) {
        _userBean = bean;
    }

    public InternalUser() {
        _userBean = new UserBean();
    }

    public String getUniqueIdentifier() {
        return new Long( _oid ).toString();
    }

    public void setOid( long oid ) {
        super.setOid( oid );
        _userBean.setUniqueIdentifier( Long.toString( oid ) );
    }

    public long getOid() {
        return new Long( _userBean.getUniqueIdentifier() ).longValue();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId) {
        this.providerId = providerId;
    }

    public Set getGroups() {
        try {
            GroupManager gman = getGroupManager();
            Set out = new HashSet();
            for (Iterator i = getGroupHeaders().iterator(); i.hasNext();) {
                EntityHeader entityHeader = (EntityHeader) i.next();
                Group g = gman.findByPrimaryKey( entityHeader.getStrId() );
                out.add( g );
            }
            return out;
        } catch (FindException e) {
            throw new RuntimeException( "Couldn't get user's groups!", e );
        }
    }

    public Set getGroupHeaders() {
        try {
            return getGroupManager().getGroupHeaders( this );
        } catch (FindException e) {
            throw new RuntimeException( "Couldn't get user's groups!" );
        }
    }

    public UserBean getUserBean() {
        return _userBean;
    }

    /**
     * set the login before setting the password.
     * if the password is not encoded, this will encode it.
     */
    public void setLogin(String login) {
        _userBean.setLogin( login );
    }

    public String getCert() {
        return _cert;
    }

    public void setCert( String cert ) {
        _cert = cert;
    }

    public int getCertResetCounter() {
        return _certResetCounter;
    }

    public void setCertResetCounter(int certResetCounter) {
        _certResetCounter = certResetCounter;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return providerId;
    }

    public String getLogin() {
        return _userBean.getLogin();
    }

    public String getPassword() {
        return _userBean.getPassword();
    }

    public String getFirstName() {
        return _userBean.getFirstName();
    }

    public String getLastName() {
        return _userBean.getLastName();
    }

    public String getEmail() {
        return _userBean.getEmail();
    }

    public String getTitle() {
        return _userBean.getTitle();
    }

    public String getDepartment() {
        return _userBean.getDepartment();
    }

    public String toString() {
        return "com.l7tech.identity.User." +
                "\n\tName=" + _name +
                "\n\tFirst name=" + _userBean.getFirstName() +
                "\n\tLast name=" + _userBean.getLastName() +
                "\n\tLogin=" + _userBean.getLogin() +
                "\n\tproviderId=" + providerId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalUser)) return false;
        final InternalUser userImp = (InternalUser) o;
        if ( providerId != DEFAULT_OID ? !( providerId== userImp.providerId ) : userImp.providerId != DEFAULT_OID ) return false;
        String login = getLogin();
        String ologin = userImp.getLogin();
        if ( login != null ? !login.equals(ologin) : ologin != null) return false;
        return true;
    }

    public int hashCode() {
        if ( _oid != DEFAULT_OID ) return (int)_oid;
        String login = getLogin();
        if ( login == null ) return System.identityHashCode(this);

        int hash = login.hashCode();
        hash += 29 * (int)providerId;
        return hash;
    }

    public void setDepartment( String department ) {
        _userBean.setDepartment( department );
    }

    public void setEmail( String email ) {
        _userBean.setEmail( email );
    }

    public void setFirstName( String firstName ) {
        _userBean.setFirstName( firstName );
    }

    public void setLastName( String lastName ) {
        _userBean.setLastName( lastName );
    }

    public void setTitle( String title ) {
        _userBean.setTitle( title );
    }

    public void setPassword( String password ) {
        _userBean.setPassword( password );
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom( User objToCopy ) {
        InternalUser imp = (InternalUser)objToCopy;
        setOid(imp.getOid());
        setName(imp.getName());
        setProviderId(imp.getProviderId());
        setLogin(imp.getLogin());
        setDepartment(imp.getDepartment());
        setEmail(imp.getEmail());
        setFirstName(imp.getFirstName());
        setLastName(imp.getLastName());
        setTitle(imp.getTitle());
        setCert( imp.getCert() );
        setCertResetCounter( imp.getCertResetCounter() );
        setPassword( imp.getPassword() );
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private GroupManager getGroupManager() {
        if ( _groupManager == null ) {
            IdentityProviderConfigManager ipc =
              (IdentityProviderConfigManager)Locator.
              getDefault().lookup(IdentityProviderConfigManager.class);
            if (ipc == null) {
                throw new RuntimeException("Could not find " + IdentityProviderConfigManager.class);
            }

            IdentityProviderConfig config = null;
            try {
                config = ipc.findByPrimaryKey( providerId );
            } catch (FindException e) {
                throw new IllegalStateException( "User " + _userBean.getLogin() + "'s IdentityProviderConfig (id = " + providerId + ") has ceased to exist!" );
            }
            IdentityProvider provider = IdentityProviderFactory.makeProvider( config );
            _groupManager = provider.getGroupManager();
        }
        return _groupManager;
    }

    private UserBean _userBean;
    private int _certResetCounter;
    private String _cert;

    private GroupManager _groupManager;
    private long providerId = IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID;
}
