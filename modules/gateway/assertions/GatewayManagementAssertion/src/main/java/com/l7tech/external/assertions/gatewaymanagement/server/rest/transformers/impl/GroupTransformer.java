package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.GroupMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.objectmodel.*;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.identity.IdentityProviderFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class GroupTransformer implements APITransformer<GroupMO, Group> {

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.GROUP.toString();
    }

    @Override
    public GroupMO convertToMO(Group group) {
        GroupMO groupMO = ManagedObjectFactory.createGroupMO();
        groupMO.setId(group.getId());
        groupMO.setProviderId(group.getProviderId().toString());
        groupMO.setName(group.getName());
        groupMO.setDescription(group.getDescription());
        return groupMO;
    }

    @Override
    public EntityContainer<Group> convertFromMO(GroupMO groupMO) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(groupMO,true);
    }

    @Override
    public EntityContainer<Group> convertFromMO(GroupMO groupMO, boolean strict) throws ResourceFactory.InvalidResourceException {
        GroupBean group = new GroupBean();
        group.setUniqueIdentifier(groupMO.getId());

        Goid identityProviderGoid = Goid.parseGoid(groupMO.getProviderId());
        try {
            identityProviderFactory.getProvider(identityProviderGoid);
        } catch (FindException e) {
            if(strict){
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "Cannot find identity provider with id: "+e.getMessage());
            }
        }
        group.setProviderId(identityProviderGoid);
        group.setName(groupMO.getName());
        group.setDescription(groupMO.getDescription());
        return new EntityContainer<Group>(group);
    }

    @Override
    public Item<GroupMO> convertToItem(GroupMO m) {
        return new ItemBuilder<GroupMO>(m.getName(), m.getId(), EntityType.GROUP.name())
                .setContent(m)
                .build();
    }

    public GroupMO convertToMO(IdentityHeader groupHeader){
        GroupMO groupMO = ManagedObjectFactory.createGroupMO();
        groupMO.setId(groupHeader.getStrId());
        groupMO.setDescription(groupHeader.getDescription());
        groupMO.setName(groupHeader.getName());
        groupMO.setProviderId(groupHeader.getProviderGoid().toString());
        return groupMO;
    }
}
