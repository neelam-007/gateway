package com.l7tech.identity.internal;

import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * User from the internal identity provider.
 * Password property is stored as HEX(MD5(login:L7SSGDigestRealm:password)). If you pass a clear text passwd in
 * setPassword, this encoding will be done ofr you (provided that login was set before).
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 *
 *
 */
public class InternalUser extends NamedEntityImp implements User {
    public InternalUser( UserBean bean ) {
        _userBean = bean;
    }

    public InternalUser() {
        _userBean = new UserBean();
        _userBean.setProviderId(DEF_PROVIDER_ID);
    }

    public String getUniqueIdentifier() {
        return new Long( _oid ).toString();
    }

    public void setOid( long oid ) {
        super.setOid( oid );
        _userBean.setUniqueIdentifier( Long.toString( oid ) );
    }

    public long getOid() {
        String uniqueId = _userBean.getUniqueIdentifier();
        if ( uniqueId == null || uniqueId.length() == 0 )
            return -1L;
        else
            return new Long( _userBean.getUniqueIdentifier() ).longValue();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId) {
        _userBean.setProviderId(providerId);
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

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return _userBean.getProviderId();
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
                "\n\tproviderId=" + _userBean.getProviderId();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalUser)) return false;
        final InternalUser userImp = (InternalUser) o;
        if (_userBean.getProviderId() != DEFAULT_OID ? !( _userBean.getProviderId()== userImp._userBean.getProviderId())
                : userImp._userBean.getProviderId() != DEFAULT_OID ) return false;
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
        hash += 29 * (int)_userBean.getProviderId();
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

    public int getVersion() {
        return _userBean.getVersion();
    }

    public void setVersion(int version) {
        _userBean.setVersion(version);
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
        setPassword( imp.getPassword() );
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private UserBean _userBean;
    private static final long DEF_PROVIDER_ID = IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID;
}
