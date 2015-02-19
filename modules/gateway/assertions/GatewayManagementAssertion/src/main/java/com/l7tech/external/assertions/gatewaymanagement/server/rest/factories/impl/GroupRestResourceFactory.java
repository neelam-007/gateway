package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.GroupTransformer;
import com.l7tech.gateway.api.GroupMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.Group;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;

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

    public List<GroupMO> listResources(final String sortKey, final Boolean asc, @NotNull String providerId, @Nullable Map<String, List<Object>> filters) throws ResourceFactory.ResourceNotFoundException {
        try {
            GroupManager groupManager = retrieveGroupManager(providerId);
            List<Group> groups = new ArrayList<>();
            if(filters!=null && filters.containsKey("name")){
                for(Object name: filters.get("name")){
                    Group group = groupManager.findByName(name.toString());
                    if(group!=null){
                        groups.add(group);
                    }
                }
            }else{
                Collection<IdentityHeader> userHeaders = groupManager.findAllHeaders();
                for(IdentityHeader idHeader: userHeaders){
                    groups.add(groupManager.findByPrimaryKey(idHeader.getStrId()));
                }
            }
            groups = rbacAccessService.accessFilter(groups, EntityType.GROUP, OperationType.READ, null);

            // sort list
            if (sortKey != null) {
                Collections.sort(groups, new Comparator<Group>() {
                    @Override
                    public int compare(Group o1, Group o2) {
                        if (sortKey.equals("name")) {
                            return (asc == null || asc) ? o1.getName().compareTo(o2.getName()) : o2.getName().compareTo(o1.getName());
                        }
                        if (sortKey.equals("id")) {
                            return (asc == null || asc) ? o1.getId().compareTo(o2.getId()) : o2.getId().compareTo(o1.getId());
                        }
                        return 0;
                    }
                });
            }

            // filter out virtual groups
            groups = Functions.grep(groups, new Functions.Unary<Boolean, Group>() {
                @Override
                public Boolean call(Group group) {
                    return !(group instanceof VirtualGroup);
                }
            });

            return Functions.map(groups, new Functions.Unary<GroupMO, Group>() {
                @Override
                public GroupMO call(Group group) {
                    return transformer.convertToMO(group, null);
                }
            });
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException(e.getMessage(), e);
        }
    }

    public GroupMO getResource(@NotNull String providerId, @NotNull String id) throws FindException, ResourceFactory.ResourceNotFoundException {
        Group group;
        try {
            group = retrieveGroupManager(providerId).findByPrimaryKey(id);
            if (group == null) {
                throw new ResourceFactory.ResourceNotFoundException("Resource not found: " + id);
            }
            // filter out virtual groups
            if( group instanceof VirtualGroup){
                throw new ResourceFactory.ResourceNotFoundException("Resource not found: " + id);
            }
        }catch(FindException e){
            if(e.getCause() instanceof IllegalArgumentException){
                throw new ResourceFactory.ResourceNotFoundException("Resource not found: " + id);
            }
            throw e;
        }
        rbacAccessService.validatePermitted(group, OperationType.READ);
        return transformer.convertToMO(group, null);
    }

    private GroupManager retrieveGroupManager(String providerId) throws ResourceFactory.ResourceNotFoundException, FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(Goid.parseGoid(providerId));

        if (provider == null)
            throw new ResourceFactory.ResourceNotFoundException("IdentityProvider could not be found");

        return provider.getGroupManager();
    }
}
