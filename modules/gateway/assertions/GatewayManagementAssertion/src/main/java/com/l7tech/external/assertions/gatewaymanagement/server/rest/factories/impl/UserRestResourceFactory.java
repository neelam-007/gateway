package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.gateway.api.ManagedObjectFactory;
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


    private Map<String, String> sortKeys = CollectionUtils.MapBuilder.<String, String>builder()
            .put("logon", "logon").map();
    private Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> filters = CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
            .put("logon", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("logon", RestResourceFactoryUtils.stringConvert))
            .map();

    public String getSortKey(String sort) {
        return sortKeys.get(sort);
    }

    public Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> getFiltersInfo() {
        return filters;
    }

    public List<UserMO> listResources(@NotNull String providerId, @NotNull Integer offset, @NotNull Integer count, @Nullable String sort, @Nullable Boolean order, @Nullable Map<String, List<Object>> filters) {
        try {
            EntityHeaderSet<IdentityHeader> users = retrieveUserManager(providerId).findAllHeaders();
            return Functions.map(users, new Functions.Unary<UserMO, IdentityHeader>() {
                @Override
                public UserMO call(IdentityHeader userHeader) {
                    if(userHeader.getType().equals(EntityType.USER))
                    {
                        return getResource(userHeader);
                    }
                    return null;
                }
            });
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException(e.getMessage(), e);
        }
    }

    public static UserMO getResource(IdentityHeader userHeader){
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(userHeader.getStrId());
        userMO.setLogin(userHeader.getName());
        userMO.setFirstName(userHeader.getCommonName());
        userMO.setLastName(userHeader.getDescription());
        userMO.setProviderId(userHeader.getProviderGoid().toString());
        return userMO;
    }

    public UserMO getResource(@NotNull String providerId, @NotNull String login) throws FindException, ResourceFactory.ResourceNotFoundException {
        User user = retrieveUserManager(providerId).findByLogin(login);
        if(user== null){
            throw new ResourceFactory.ResourceNotFoundException( "Resource not found: " + login);
        }
        return buildMO(user);
    }

    /**
     * Builds a user MO
     *
     * @param user The user to build the MO from
     * @return The user MO
     */
    private UserMO buildMO(User user) {
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(user.getId());
        userMO.setLogin(user.getLogin());
        userMO.setProviderId(user.getProviderId().toString());
        userMO.setFirstName(user.getFirstName());
        userMO.setLastName(user.getLastName());
        userMO.setEmail(user.getEmail());
        userMO.setDepartment(user.getDepartment());
        userMO.setSubjectDn(user.getSubjectDn());
        return userMO;
    }

    private UserManager retrieveUserManager(String providerId) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(Goid.parseGoid(providerId));

        if (provider == null)
            throw new FindException("IdentityProvider could not be found");

        return provider.getUserManager();
    }
}
