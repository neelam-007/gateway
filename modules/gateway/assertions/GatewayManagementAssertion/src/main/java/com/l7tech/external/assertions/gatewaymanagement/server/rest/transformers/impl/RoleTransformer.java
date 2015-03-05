package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.RbacRoleResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.RbacRoleMO;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class RoleTransformer extends APIResourceWsmanBaseTransformer<RbacRoleMO, Role,EntityHeader, RbacRoleResourceFactory> {

    @Override
    @Inject
    @Named("rbacRoleResourceFactory")
    protected void setFactory(RbacRoleResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<RbacRoleMO> convertToItem(@NotNull RbacRoleMO m) {
        return new ItemBuilder<RbacRoleMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
