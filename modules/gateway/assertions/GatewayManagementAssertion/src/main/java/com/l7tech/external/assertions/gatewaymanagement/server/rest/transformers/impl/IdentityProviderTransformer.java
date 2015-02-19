package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.IdentityProviderResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.Charsets;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.text.ParseException;
import java.util.regex.Matcher;

@Component
public class IdentityProviderTransformer extends APIResourceWsmanBaseTransformer<IdentityProviderMO, IdentityProviderConfig,EntityHeader, IdentityProviderResourceFactory> {

    @Override
    @Inject
    protected void setFactory(IdentityProviderResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<IdentityProviderMO> convertToItem(@NotNull IdentityProviderMO m) {
        return new ItemBuilder<IdentityProviderMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public EntityContainer<IdentityProviderConfig> convertFromMO(@NotNull IdentityProviderMO identityProviderMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        if(identityProviderMO.getIdentityProviderType() == IdentityProviderMO.IdentityProviderType.LDAP){
            final IdentityProviderMO.LdapIdentityProviderDetail detail = identityProviderMO.getLdapIdentityProviderDetail();
            if(detail.getBindPasswordBundleKey()!=null) {
                try {
                    detail.setBindPassword(new String(secretsEncryptor.decryptSecret(detail.getBindPassword(), detail.getBindPasswordBundleKey()), Charsets.UTF8));
                } catch (ParseException e) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Failed to decrypt password");
                }
            }
        }
        return super.convertFromMO(identityProviderMO, strict, secretsEncryptor);
    }

    @NotNull
    @Override
    public IdentityProviderMO convertToMO(@NotNull IdentityProviderConfig identityProviderConfig, SecretsEncryptor secretsEncryptor) {
        IdentityProviderMO mo =  super.convertToMO(identityProviderConfig, secretsEncryptor);
        if( secretsEncryptor !=null ){
            if(identityProviderConfig instanceof LdapIdentityProviderConfig){
                final IdentityProviderMO.LdapIdentityProviderDetail detail = mo.getLdapIdentityProviderDetail();
                final LdapIdentityProviderConfig ldapIdProvider = (LdapIdentityProviderConfig)identityProviderConfig;
                if(ldapIdProvider.getBindPasswd()!=null) {
                    final Matcher matcher = ServerVariables.SINGLE_SECPASS_PATTERN.matcher(ldapIdProvider.getBindPasswd());
                    if (matcher.matches()) {
                        detail.setBindPassword(ldapIdProvider.getBindPasswd());
                    }else {
                        detail.setBindPassword(secretsEncryptor.encryptSecret(ldapIdProvider.getBindPasswd().getBytes(Charsets.UTF8)), secretsEncryptor.getWrappedBundleKey());
                    }
                }
            }

        }
        return mo;
    }
}
