package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.UserMO;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class UserTransformer implements APITransformer<UserMO, User> {

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.USER.toString();
    }

    @Override
    public UserMO convertToMO(User user) {
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

    @Override
    public User convertFromMO(UserMO userMO) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(userMO, true);
    }

    @Override
    public User convertFromMO(UserMO userMO, boolean strict) throws ResourceFactory.InvalidResourceException {
        UserBean user = new UserBean();
        user.setUniqueIdentifier(userMO.getId());
        user.setLastName(userMO.getLogin());

        Goid identityProviderGoid = Goid.parseGoid(userMO.getProviderId());
        try {
            identityProviderFactory.getProvider(identityProviderGoid);
        } catch (FindException e) {
            if(strict){
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "Cannot find identity provider with id: "+e.getMessage());
            }
        }
        user.setProviderId(identityProviderGoid);
        user.setFirstName(userMO.getFirstName());
        user.setLastName(userMO.getLastName());
        user.setEmail(userMO.getEmail());
        user.setDepartment(userMO.getDepartment());
        user.setSubjectDn(userMO.getSubjectDn());
        return user;
    }

    @Override
    public Item<UserMO> convertToItem(UserMO m) {
        return new ItemBuilder<UserMO>(m.getLogin(), m.getId(), EntityType.USER.name())
                .setContent(m)
                .build();
    }

    @Override
    public Item<UserMO> convertToItem(EntityHeader header) {
        return new ItemBuilder<UserMO>(header.getName(), header.getStrId(), EntityType.USER.name())
                .build();
    }

    public UserMO convertToMO(IdentityHeader userHeader){
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setId(userHeader.getStrId());
        userMO.setLogin(userHeader.getName());
        userMO.setFirstName(userHeader.getCommonName());
        userMO.setLastName(userHeader.getDescription());
        userMO.setProviderId(userHeader.getProviderGoid().toString());
        return userMO;
    }
}
