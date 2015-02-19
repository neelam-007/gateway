package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.SiteMinderConfigurationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.SiteMinderConfigurationMO;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.util.Charsets;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class SiteMinderConfigurationTransformer extends APIResourceWsmanBaseTransformer<SiteMinderConfigurationMO, SiteMinderConfiguration, EntityHeader, SiteMinderConfigurationResourceFactory> {
    private static final Logger logger = Logger.getLogger(SiteMinderConfigurationTransformer.class.getName());
    @Override
    @Inject
    protected void setFactory(SiteMinderConfigurationResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<SiteMinderConfigurationMO> convertToItem(@NotNull SiteMinderConfigurationMO m) {
        return new ItemBuilder<SiteMinderConfigurationMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public EntityContainer<SiteMinderConfiguration> convertFromMO(@NotNull SiteMinderConfigurationMO siteMinderConfigurationMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        final EntityContainer<SiteMinderConfiguration> container = super.convertFromMO(siteMinderConfigurationMO, strict, secretsEncryptor);
        if (secretsEncryptor != null && container.getEntity() != null &&
                siteMinderConfigurationMO.getSecret() != null && siteMinderConfigurationMO.getSecretBundleKey() != null) {
            try {
                container.getEntity().setSecret(new String(secretsEncryptor.decryptSecret(siteMinderConfigurationMO.getSecret(), siteMinderConfigurationMO.getSecretBundleKey()), Charsets.UTF8));
            } catch (final ParseException e) {
                logger.log(Level.WARNING, "Error decrypting secret: " + e.getMessage(), e);
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Failed to decrypt password");
            }
        }
        return container;
    }

    @NotNull
    @Override
    public SiteMinderConfigurationMO convertToMO(@NotNull SiteMinderConfiguration siteMinderConfiguration, SecretsEncryptor secretsEncryptor) {
        final SiteMinderConfigurationMO mo = super.convertToMO(siteMinderConfiguration, secretsEncryptor);
        if (siteMinderConfiguration.getSecret() != null && secretsEncryptor != null) {
            mo.setEncryptedSecret(secretsEncryptor.encryptSecret(siteMinderConfiguration.getSecret().getBytes(Charsets.UTF8)), secretsEncryptor.getWrappedBundleKey());
        }
        return mo;
    }
}
