package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.GroupTransformer;
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

    @Inject
    private GroupTransformer transformer;

    private Map<String, String> sortKeys = CollectionUtils.MapBuilder.<String, String>builder()
            .put("name", "name").map();
    private Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> filters = CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
            .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
            .map();

    public Map<String, String> getSortKeysMap() {
        return sortKeys;
    }

    public Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> getFiltersInfo() {
        return filters;
    }

    public List<GroupMO> listResources(@NotNull String providerId, @NotNull Integer offset, @NotNull Integer count, @Nullable String sort, @Nullable Boolean order, @Nullable Map<String, List<Object>> filters) {
        try {
            GroupManager groupManager = retrieveGroupManager(providerId);
            EntityHeaderSet<IdentityHeader> groups = new EntityHeaderSet<IdentityHeader>();
            if(filters.containsKey("name")){
                for(Object name: filters.get("name")){
                    groups.addAll(groupManager.search(name.toString()));
                }
            }else{
                groups.addAll(groupManager.findAllHeaders());
            }
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
        return transformer.convertToMO(group);
    }

    private GroupManager retrieveGroupManager(String providerId) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(Goid.parseGoid(providerId));

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getGroupManager();
    }
}
