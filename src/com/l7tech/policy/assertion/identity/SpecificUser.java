/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

/**
 * Asserts that the requester is a particular User.
 *
 * @author alex
 * @version $Revision$
 */
public class SpecificUser extends IdentityAssertion {
    public SpecificUser() {
        super();
    }

    /**
     * Constructs a new SpecificUser assertion. Either the userlogin or userUid must be non-null.
     * @param providerId the oid of the {@link com.l7tech.identity.IdentityProviderConfig} of the
     * {@link com.l7tech.identity.IdentityProvider} to which the specific user belongs
     * @param userLogin the login of the {@link com.l7tech.identity.User} mentioned by this assertion.
     * May be null.
     * @param userUid the unique identifier (DN for an {@link com.l7tech.identity.ldap.LdapUser} or
     * oid for a {@link com.l7tech.identity.PersistentUser}). May be null.
     */
    public SpecificUser(long providerId, String userLogin, String userUid, String userName) {
        super(providerId);
        this.userLogin = userLogin;
        this.userName = userName;
        this.userUid = userUid;
    }

    public void setUserLogin( String userLogin ) {
        this.userLogin = userLogin;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid( String userUid ) {
        this.userUid = userUid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append(" ");
        if (userName != null)
            sb.append(userName);
        else if (userLogin != null)
            sb.append(userLogin);
        else if (userUid != null)
            sb.append(userUid);
        return sb.toString();
    }

    protected String userLogin;
    protected String userUid;
    private String userName;
}
