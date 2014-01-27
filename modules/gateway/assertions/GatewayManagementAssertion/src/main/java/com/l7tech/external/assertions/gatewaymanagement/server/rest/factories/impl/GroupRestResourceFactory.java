package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.GroupMO;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Component
public class GroupRestResourceFactory {

    @Inject
    private IdentityProviderFactory identityProviderFactory;


    private Map<String, String> sortKeys = CollectionUtils.MapBuilder.<String, String>builder()
            .put("name", "name").map();
    private Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> filters = CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
            .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
            .map();

    public String getSortKey(String sort) {
        return sortKeys.get(sort);
    }

    public Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> getFiltersInfo() {
        return filters;
    }

    public List<GroupMO> listResources(@NotNull String providerId, @NotNull Integer offset, @NotNull Integer count, @Nullable String sort, @Nullable Boolean order, @Nullable Map<String, List<Object>> filters) {
        try {
            EntityHeaderSet<IdentityHeader> groups = retrieveGroupManager(providerId).findAllHeaders();
            return Functions.map(groups, new Functions.Unary<GroupMO, IdentityHeader>() {
                @Override
                public GroupMO call(IdentityHeader idHeader) {
                    if(idHeader.getType().equals(EntityType.GROUP))
                    {
                        return getResource(idHeader);
                    }
                    return null;
                }
            });
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException(e.getMessage(), e);
        }
    }

    public static GroupMO getResource(IdentityHeader groupHeader){
        GroupMO groupMO = ManagedObjectFactory.createGroupMO();
        groupMO.setId(groupHeader.getStrId());
        groupMO.setDescription(groupHeader.getDescription());
        groupMO.setName(groupHeader.getName());
        groupMO.setProviderId(groupHeader.getProviderGoid().toString());
        return groupMO;
    }

    public GroupMO getResource(@NotNull String providerId, @NotNull String name) throws FindException, ResourceFactory.ResourceNotFoundException {
        Group group = retrieveGroupManager(providerId).findByName(name);
        if(group== null){
            throw new ResourceFactory.ResourceNotFoundException( "Resource not found: " + name);
        }
        return buildMO(group);
    }

    /**
     * Builds a group MO
     *
     * @param group The group to build the MO from
     * @return The group MO
     */
    private GroupMO buildMO(Group group) {
        GroupMO groupMO = ManagedObjectFactory.createGroupMO();
        groupMO.setId(group.getId());
        groupMO.setProviderId(group.getProviderId().toString());
        groupMO.setName(group.getName());
        groupMO.setDescription(group.getDescription());
        return groupMO;
    }

    private GroupManager retrieveGroupManager(String providerId) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(Goid.parseGoid(providerId));

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getGroupManager();
    }
}
