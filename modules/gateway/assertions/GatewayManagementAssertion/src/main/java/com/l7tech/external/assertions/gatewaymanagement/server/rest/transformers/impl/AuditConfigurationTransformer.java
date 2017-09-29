package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.AuditConfigurationMO;
import com.l7tech.gateway.api.AuditFtpConfig;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfigImpl;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.util.Charsets;
import java.text.ParseException;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class AuditConfigurationTransformer implements EntityAPITransformer<AuditConfigurationMO, AuditConfiguration> {

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.AUDIT_CONFIG.toString();
    }

    @NotNull
    @Override
    public AuditConfigurationMO convertToMO(@NotNull EntityContainer<AuditConfiguration> userEntityContainer, SecretsEncryptor secretsEncryptor) {
        return convertToMO(userEntityContainer.getEntity(), secretsEncryptor);
    }


    @NotNull
    public AuditConfigurationMO convertToMO(@NotNull AuditConfiguration AuditConfiguration) {
        return convertToMO(AuditConfiguration, null);
    }

    @NotNull
    @Override
    public AuditConfigurationMO convertToMO(@NotNull AuditConfiguration auditConfiguration,  SecretsEncryptor secretsEncryptor) {
        AuditConfigurationMO auditConfigurationMO = ManagedObjectFactory.createAuditConfiguration();
        auditConfigurationMO.setAlwaysSaveInternal(auditConfiguration.isAlwaysSaveInternal());

        if(auditConfiguration.getFtpClientConfig()!=null){
            AuditFtpConfig ftpConfigMO = ManagedObjectFactory.createAuditFtpConfig();
            ftpConfigMO.setHost(auditConfiguration.getFtpClientConfig().getHost());
            ftpConfigMO.setPort(auditConfiguration.getFtpClientConfig().getPort());
            ftpConfigMO.setTimeout(auditConfiguration.getFtpClientConfig().getTimeout());
            ftpConfigMO.setUser(auditConfiguration.getFtpClientConfig().getUser());
            if( secretsEncryptor !=null ){
                ftpConfigMO.setPassword(secretsEncryptor.encryptSecret(auditConfiguration.getFtpClientConfig().getPass().getBytes(Charsets.UTF8)), secretsEncryptor.getWrappedBundleKey());
            }
            ftpConfigMO.setDirectory(auditConfiguration.getFtpClientConfig().getDirectory());
            ftpConfigMO.setVerifyServerCert(auditConfiguration.getFtpClientConfig().isVerifyServerCert());
            ftpConfigMO.setSecurity(AuditFtpConfig.SecurityType.valueOf(auditConfiguration.getFtpClientConfig().getSecurity().getWspName()));
            ftpConfigMO.setEnabled(auditConfiguration.getFtpClientConfig().isEnabled());

            auditConfigurationMO.setFtpConfig(ftpConfigMO);
        }
        if(auditConfiguration.getSinkPolicyGoid()!=null) {
            auditConfigurationMO.setSinkPolicyReference(new ManagedObjectReference(PolicyMO.class, auditConfiguration.getSinkPolicyGoid().toString()));
        }
        if(auditConfiguration.getLookupPolicyGoid()!=null) {
            auditConfigurationMO.setLookupPolicyReference(new ManagedObjectReference(PolicyMO.class, auditConfiguration.getLookupPolicyGoid().toString()));
        }

        return auditConfigurationMO;
    }

    @NotNull
    @Override
    public EntityContainer<AuditConfiguration> convertFromMO(@NotNull AuditConfigurationMO AuditConfigurationMO, SecretsEncryptor secretsEncryptor)
            throws ResourceFactory.InvalidResourceException {
        return convertFromMO(AuditConfigurationMO, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<AuditConfiguration> convertFromMO(@NotNull AuditConfigurationMO auditConfigurationMO, boolean strict, SecretsEncryptor secretsEncryptor)
            throws ResourceFactory.InvalidResourceException {

        AuditConfiguration auditConfiguration = new AuditConfiguration();
        auditConfiguration.setSinkPolicyGoid(auditConfigurationMO.getSinkPolicyReference() == null? null : Goid.parseGoid(auditConfigurationMO.getSinkPolicyReference().getId()));
        auditConfiguration.setLookupPolicyGoid(auditConfigurationMO.getLookupPolicyReference() == null? null : Goid.parseGoid(auditConfigurationMO.getLookupPolicyReference().getId()));
        auditConfiguration.setAlwaysSaveInternal(auditConfigurationMO.getAlwaysSaveInternal());

        if(auditConfigurationMO.getFtpConfig() != null) {
            FtpClientConfig ftpClientConfig = FtpClientConfigImpl.newFtpConfig(auditConfigurationMO.getFtpConfig().getHost());
            ftpClientConfig.setPort(auditConfigurationMO.getFtpConfig().getPort());
            ftpClientConfig.setTimeout(auditConfigurationMO.getFtpConfig().getTimeout());
            ftpClientConfig.setUser(auditConfigurationMO.getFtpConfig().getUser());

            if(auditConfigurationMO.getFtpConfig().getPassword() != null) {
                if (auditConfigurationMO.getFtpConfig().getPasswordBundleKey() != null) {
                    try {
                        ftpClientConfig.setPass(new String(secretsEncryptor.decryptSecret(auditConfigurationMO.getFtpConfig().getPasswordValue(), auditConfigurationMO.getFtpConfig().getPasswordBundleKey()), Charsets.UTF8));
                    } catch (ParseException e) {
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Failed to decrypt password");
                    }
                } else {
                    // get plaintext password
                        ftpClientConfig.setPass(auditConfigurationMO.getFtpConfig().getPasswordValue());
                } 
            }
            ftpClientConfig.setDirectory(auditConfigurationMO.getFtpConfig().getDirectory());
            ftpClientConfig.setVerifyServerCert(auditConfigurationMO.getFtpConfig().isVerifyServerCert() == null ? false : auditConfigurationMO.getFtpConfig().isVerifyServerCert() );
            ftpClientConfig.setSecurity((FtpSecurity)FtpSecurity.getEnumTranslator().stringToObject(auditConfigurationMO.getFtpConfig().getSecurity().name()));
            ftpClientConfig.setEnabled(auditConfigurationMO.getFtpConfig().isEnabled());

            auditConfiguration.setFtpClientConfig(ftpClientConfig);
        }
        return new EntityContainer<>(auditConfiguration);


    }

    @NotNull
    @Override
    public Item<AuditConfigurationMO> convertToItem(@NotNull AuditConfigurationMO m) {
        return new ItemBuilder<AuditConfigurationMO>(AuditConfiguration.ENTITY_NAME, AuditConfiguration.ENTITY_ID.toString(), getResourceType())
                .setContent(m)
                .build();
    }
}
