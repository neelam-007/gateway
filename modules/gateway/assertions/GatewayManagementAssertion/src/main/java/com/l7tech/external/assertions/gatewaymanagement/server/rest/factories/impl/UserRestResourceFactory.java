package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.UserTransformer;
import com.l7tech.gateway.api.UserMO;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
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
public class UserRestResourceFactory {

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @Inject
    private UserTransformer userTransformer;

    private Map<String, String> sortKeys = CollectionUtils.MapBuilder.<String, String>builder()
            .put("id", "id")
            .put("login", "login").map();
    private Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> filters = CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
            .put("login", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("login", RestResourceFactoryUtils.stringConvert))
            .map();

    public String getSortKey(String sort) {
        return sortKeys.get(sort);
    }

    public Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> getFiltersInfo() {
        return filters;
    }

    public List<UserMO> listResources(@NotNull String providerId, @NotNull Integer offset, @NotNull Integer count, @Nullable String sort, @Nullable Boolean order, @Nullable Map<String, List<Object>> filters) {
        try {
            UserManager userManager  = retrieveUserManager(providerId);
            EntityHeaderSet<IdentityHeader> users = new EntityHeaderSet<IdentityHeader>();
            if(filters.containsKey("login")){
                for(Object login: filters.get("login")){
                    users.add(userManager.userToHeader(userManager.findByLogin(login.toString())));
                }
            }else if(filters.containsKey("id")){
                for(Object id: filters.get("id")){
                    users.add(userManager.userToHeader(userManager.findByPrimaryKey(id.toString())));
                }
            }else{
                users.addAll(userManager.findAllHeaders());
            }
            return Functions.map(users, new Functions.Unary<UserMO, IdentityHeader>() {
                @Override
                public UserMO call(IdentityHeader userHeader) {
                    if(userHeader.getType().equals(EntityType.USER))
                    {
                        return userTransformer.convertToMO(userHeader);
                    }
                    return null;
                }
            });
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException(e.getMessage(), e);
        }
    }

    public UserMO getResource(@NotNull String providerId, @NotNull String login) throws FindException, ResourceFactory.ResourceNotFoundException {
        User user = retrieveUserManager(providerId).findByPrimaryKey(login);
        if(user== null){
            throw new ResourceFactory.ResourceNotFoundException( "Resource not found: " + login);
        }
        return userTransformer.convertToMO(user);
    }

    private UserManager retrieveUserManager(String providerId) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(Goid.parseGoid(providerId));

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getUserManager();
    }
}
