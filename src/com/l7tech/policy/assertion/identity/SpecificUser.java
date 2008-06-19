/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.IdentityHeader;                                       

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

    public EntityHeader[] getEntitiesUsed() {
        EntityHeader[] headers = super.getEntitiesUsed();
        EntityHeader[] headers2 = new EntityHeader[headers.length + 1];
        System.arraycopy(headers, 0, headers2, 0, headers.length);
        headers2[headers.length] = new IdentityHeader(super.getIdentityProviderOid(), userUid, EntityType.USER, userLogin, null);
        return headers2;
    }

    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(!oldEntityHeader.getType().equals(newEntityHeader.getType())) {
            return;
        }

        if(oldEntityHeader instanceof IdentityHeader && newEntityHeader instanceof IdentityHeader) {
            IdentityHeader oldIdentityHeader = (IdentityHeader)oldEntityHeader;
            IdentityHeader newIdentityHeader = (IdentityHeader)newEntityHeader;

            if(oldIdentityHeader.getProviderOid() == _identityProviderOid && oldIdentityHeader.getStrId().equals(userUid)) {
                _identityProviderOid = newIdentityHeader.getProviderOid();
                userUid = newIdentityHeader.getStrId();

                return;
            }
        }

        super.replaceEntity(oldEntityHeader, newEntityHeader);
    }

    public String loggingIdentity() {
        String idtomatch = getUserLogin();
        if (idtomatch == null)
            idtomatch = getUserName();
        return idtomatch;
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
