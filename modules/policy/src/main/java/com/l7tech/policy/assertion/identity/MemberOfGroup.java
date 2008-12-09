/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.UserGroupResolver;

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
    }

    public MemberOfGroup(long providerOid, String groupName, String groupID) {
        super(providerOid);
        this._groupName = groupName;
        this.groupId = groupID;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, export = false, resolver = UserGroupResolver.class)
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

            if(oldIdentityHeader.getProviderOid() == _identityProviderOid && oldIdentityHeader.getStrId().equals(groupId)) {
                _identityProviderOid = newIdentityHeader.getProviderOid();
                groupId = newIdentityHeader.getStrId();

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
    public String toString() {
        return super.toString() + " " + getGroupName();
    }

    protected String _groupName;
    protected String groupId;
}
