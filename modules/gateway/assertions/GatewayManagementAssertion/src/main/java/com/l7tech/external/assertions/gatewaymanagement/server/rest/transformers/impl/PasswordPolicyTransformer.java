package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PasswordPolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PasswordPolicyMO;
import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.identity.IdentityProviderPasswordPolicyManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.bundling.EntityContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class PasswordPolicyTransformer extends APIResourceWsmanBaseTransformer<PasswordPolicyMO, IdentityProviderPasswordPolicy, EntityHeader, PasswordPolicyResourceFactory> {


    @Override
    @Inject
    @Named("passwordPolicyResourceFactory")
    protected void setFactory(PasswordPolicyResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<PasswordPolicyMO> convertToItem(@NotNull PasswordPolicyMO m) {
        return new ItemBuilder<PasswordPolicyMO>("Manage Password Rules", m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public EntityContainer<IdentityProviderPasswordPolicy> convertFromMO(@NotNull PasswordPolicyMO passwordPolicyMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return super.convertFromMO(passwordPolicyMO, strict, secretsEncryptor);
    }

    @NotNull
    @Override
    public PasswordPolicyMO convertToMO(@NotNull IdentityProviderPasswordPolicy passwordPolicy, SecretsEncryptor secretsEncryptor) {
        PasswordPolicyMO mo =  super.convertToMO(passwordPolicy, secretsEncryptor);
        return mo;
    }
}
