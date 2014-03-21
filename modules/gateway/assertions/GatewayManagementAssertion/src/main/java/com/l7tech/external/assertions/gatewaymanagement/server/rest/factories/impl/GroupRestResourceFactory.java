package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.GroupTransformer;
import com.l7tech.gateway.api.GroupMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Component
public class GroupRestResourceFactory {

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @Inject
    private GroupTransformer transformer;

    @Inject
    private RbacAccessService rbacAccessService;

    public List<GroupMO> listResources(@NotNull String providerId, @NotNull Integer offset, @NotNull Integer count, @Nullable String sort, @Nullable Boolean order, @Nullable Map<String, List<Object>> filters) {
        try {
            GroupManager groupManager = retrieveGroupManager(providerId);
            List<IdentityHeader> groups = new ArrayList<>();
            if(filters.containsKey("name")){
                for(Object name: filters.get("name")){
                    groups.addAll(groupManager.search(name.toString()));
                }
            }else{
                groups.addAll(groupManager.findAllHeaders());
            }
            groups = rbacAccessService.accessFilter(groups, EntityType.GROUP, OperationType.READ, null);

            return Functions.map(groups, new Functions.Unary<GroupMO, IdentityHeader>() {
                @Override
                public GroupMO call(IdentityHeader idHeader) {
                    if(idHeader.getType().equals(EntityType.GROUP))
                    {
                        return transformer.convertToMO(idHeader);
                    }
                    return null;
                }
            });
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException(e.getMessage(), e);
        }
    }

    public GroupMO getResource(@NotNull String providerId, @NotNull String name) throws FindException, ResourceFactory.ResourceNotFoundException {
        Group group = retrieveGroupManager(providerId).findByName(name);
        if(group== null){
            throw new ResourceFactory.ResourceNotFoundException( "Resource not found: " + name);
        }
        rbacAccessService.validatePermitted(group, OperationType.READ);
        return transformer.convertToMO(group);
    }

    private GroupManager retrieveGroupManager(String providerId) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(Goid.parseGoid(providerId));

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getGroupManager();
    }
}
