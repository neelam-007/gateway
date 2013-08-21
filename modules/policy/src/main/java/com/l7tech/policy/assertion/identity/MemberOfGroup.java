/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.util.GoidUpgradeMapper;

/**
 * Asserts that the requestor is a member of a particular group.
 *
 * @author alex
 */
public class MemberOfGroup extends IdentityAssertion {
    public MemberOfGroup() {
        super();
    }

    public String getGroupName() {
        return _groupName;
    }

    public void setGroupName(String groupName) {
        _groupName = groupName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
        mapGroupId();
    }

    public MemberOfGroup(Goid providerOid, String groupName, String groupID) {
        super(providerOid);
        this._groupName = groupName;
        this.groupId = groupID;
    }

    @Override
    public void setIdentityProviderOid(long providerOid) {
        super.setIdentityProviderOid(providerOid);
        mapGroupId();
    }

    private final Goid INTERNAL_IDENTITY_PROVIDER = new Goid(0,-2);
    private void mapGroupId(){
        if(getGroupId()!=null && getGroupId().length()!=32 && getIdentityProviderOid()!=null && !getIdentityProviderOid().equals(PersistentEntity.DEFAULT_GOID)){
            try{
                Long groupOidId = Long.parseLong(getGroupId());
                if(getIdentityProviderOid().equals(INTERNAL_IDENTITY_PROVIDER)){
                    setGroupId(GoidUpgradeMapper.mapOidFromTableName("internal_group", groupOidId).toString());
                }else{
                    setGroupId(GoidUpgradeMapper.mapOidFromTableName("fed_group", groupOidId).toString());
                }
            }catch(NumberFormatException e){
                // no need to map dn group id
            }
        }
    }

    @Override
    @Migration(mapName = NONE, mapValue =  NONE, export = false, resolver = PropertyResolver.Type.USERGROUP)
    public EntityHeader[] getEntitiesUsed() {
        EntityHeader[] headers = super.getEntitiesUsed();
        EntityHeader[] headers2 = new EntityHeader[headers.length + 1];
        System.arraycopy(headers, 0, headers2, 0, headers.length);
        headers2[headers.length] = new IdentityHeader(_identityProviderOid, groupId, EntityType.GROUP, _groupName, null, null, null);
        return headers2;
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(!oldEntityHeader.getType().equals(newEntityHeader.getType())) {
            return;
        }

        if(oldEntityHeader instanceof IdentityHeader && newEntityHeader instanceof IdentityHeader) {
            IdentityHeader oldIdentityHeader = (IdentityHeader)oldEntityHeader;
            IdentityHeader newIdentityHeader = (IdentityHeader)newEntityHeader;

            if(oldIdentityHeader.getProviderGoid().equals(_identityProviderOid) && oldIdentityHeader.getStrId().equals(groupId)) {
                _identityProviderOid = newIdentityHeader.getProviderGoid();
                groupId = newIdentityHeader.getStrId();
                _groupName = newIdentityHeader.getName();

                return;
            }
        }

        super.replaceEntity(oldEntityHeader, newEntityHeader);
    }

    @Override
    public String loggingIdentity() {
        return getGroupName();
    }

    @Override
    public IdentityTarget getIdentityTarget() {
        return new IdentityTarget(IdentityTarget.TargetIdentityType.GROUP, getIdentityProviderOid(), getGroupId(), getGroupName());
    }

    @Override
    public String toString() {
        return super.toString() + " " + getGroupName();
    }

    protected String _groupName;
    protected String groupId;
}
