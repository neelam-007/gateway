package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.util.MasterPasswordManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class GroupTransformer implements EntityAPITransformer<GroupMO, Group> {

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.GROUP.toString();
    }

    @NotNull
    @Override
    public GroupMO convertToMO(@NotNull EntityContainer<Group> groupEntityContainer,  MasterPasswordManager passwordManager) {
        return convertToMO(groupEntityContainer.getEntity(), passwordManager);
    }

    @NotNull
    @Override
    public GroupMO convertToMO(@NotNull Group group,  MasterPasswordManager passwordManager) {
        GroupMO groupMO = ManagedObjectFactory.createGroupMO();
        groupMO.setId(group.getId());
        groupMO.setProviderId(group.getProviderId().toString());
        groupMO.setName(group.getName());
        groupMO.setDescription(group.getDescription());
        return groupMO;
    }

    @NotNull
    @Override
    public EntityContainer<Group> convertFromMO(@NotNull GroupMO groupMO, MasterPasswordManager passwordManager) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(groupMO,true, passwordManager);
    }

    @NotNull
    @Override
    public EntityContainer<Group> convertFromMO(@NotNull GroupMO groupMO, boolean strict, MasterPasswordManager passwordManager) throws ResourceFactory.InvalidResourceException {
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

    @NotNull
    @Override
    public Item<GroupMO> convertToItem(@NotNull GroupMO m) {
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
