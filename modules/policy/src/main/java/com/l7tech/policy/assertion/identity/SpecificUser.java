/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.util.GoidUpgradeMapper;

import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

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
     * @param providerId the oid of the {@code com.l7tech.identity.IdentityProviderConfig} of the
     * {@code com.l7tech.identity.IdentityProvider} to which the specific user belongs
     * @param userLogin the login of the {@code com.l7tech.identity.User} mentioned by this assertion.
     * May be null.
     * @param userUid the unique identifier (DN for an {@code com.l7tech.identity.ldap.LdapUser} or
     * oid for a {@code com.l7tech.identity.PersistentUser}). May be null.
     */
    public SpecificUser(Goid providerId, String userLogin, String userUid, String userName) {
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
        mapUserId();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

    @Override
    public void setIdentityProviderOid(long providerOid) {
        super.setIdentityProviderOid(providerOid);
        mapUserId();
    }

    @Override
    @Migration(mapName = NONE, mapValue = NONE, export = false, resolver = PropertyResolver.Type.USERGROUP)
    public EntityHeader[] getEntitiesUsed() {
        EntityHeader[] headers = super.getEntitiesUsed();
        EntityHeader[] headers2 = new EntityHeader[headers.length + 1];
        System.arraycopy(headers, 0, headers2, 0, headers.length);
        headers2[headers.length] = new IdentityHeader(super.getIdentityProviderOid(), userUid, EntityType.USER, userLogin, null, userName, null);
        return headers2;
    }

    private final Goid INTERNAL_IDENTITY_PROVIDER = new Goid(0,-2);
    private void mapUserId(){
        if(getUserUid()!=null && getUserUid().length()!=32 && getIdentityProviderOid()!=null&& !getIdentityProviderOid().equals(PersistentEntity.DEFAULT_GOID)){
            try{
                Long groupOidId = Long.parseLong(getUserUid());
                if(getIdentityProviderOid().equals(INTERNAL_IDENTITY_PROVIDER)){
                    if(groupOidId == 3){
                        setUserUid(Goid.toString(new Goid(0,3)));
                    }else{
                        setUserUid(GoidUpgradeMapper.mapOidFromTableName("internal_user", groupOidId).toString());
                    }
                }else{
                    setUserUid(GoidUpgradeMapper.mapOidFromTableName("fed_user",groupOidId).toString());
                }
            }catch(NumberFormatException e){
                // no need to map dn group id
            }
        }
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(!oldEntityHeader.getType().equals(newEntityHeader.getType())) {
            return;
        }

        if(oldEntityHeader instanceof IdentityHeader && newEntityHeader instanceof IdentityHeader) {
            IdentityHeader oldIdentityHeader = (IdentityHeader)oldEntityHeader;
            IdentityHeader newIdentityHeader = (IdentityHeader)newEntityHeader;

            if(oldIdentityHeader.getProviderGoid().equals(_identityProviderOid) && oldIdentityHeader.getStrId().equals(userUid)) {
                _identityProviderOid = newIdentityHeader.getProviderGoid();
                userUid = newIdentityHeader.getStrId();
                userLogin = newIdentityHeader.getName();
                userName = newIdentityHeader.getCommonName();

                return;
            }
        }

        super.replaceEntity(oldEntityHeader, newEntityHeader);
    }

    @Override
    public String loggingIdentity() {
        String idtomatch = getUserLogin();
        if (idtomatch == null || idtomatch.trim().length() < 1)
            idtomatch = getUserName();
        if (idtomatch == null || idtomatch.trim().length() < 1)
            idtomatch = getUserUid();
        return idtomatch;
    }

    @Override
    public IdentityTarget getIdentityTarget() {
        return new IdentityTarget(IdentityTarget.TargetIdentityType.USER, getIdentityProviderOid(), getUserUid(), loggingIdentity());
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = (DefaultAssertionMetadata) super.meta();
        if (meta.get(META_INITIALIZED) != null)
            return meta;

        meta.put(AssertionMetadata.PALETTE_NODE_CLASSNAME, "com.l7tech.console.tree.IdentityNode");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
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
    private static final String META_INITIALIZED = SpecificUser.class.getName() + ".metadataInitialized";
}
