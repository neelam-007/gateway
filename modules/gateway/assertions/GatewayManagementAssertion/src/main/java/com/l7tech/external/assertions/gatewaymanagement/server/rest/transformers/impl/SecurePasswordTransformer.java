package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.SecurePasswordResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Charsets;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;

@Component
public class SecurePasswordTransformer extends APIResourceWsmanBaseTransformer<StoredPasswordMO, SecurePassword,EntityHeader, SecurePasswordResourceFactory> {

    @Inject
    protected SecurePasswordManager passwordManager;

    @Override
    @Inject
    protected void setFactory(SecurePasswordResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<StoredPasswordMO> convertToItem(@NotNull StoredPasswordMO m) {
        return new ItemBuilder<StoredPasswordMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public EntityContainer<SecurePassword> convertFromMO(@NotNull StoredPasswordMO storedPasswordMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        if(storedPasswordMO.getPassword()!=null && storedPasswordMO.getPasswordBundleKey() != null){
            try {
                storedPasswordMO.setPassword(new String(secretsEncryptor.decryptSecret(storedPasswordMO.getPassword(), storedPasswordMO.getPasswordBundleKey()), Charsets.UTF8));
            } catch (ParseException e) {
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Failed to decrypt password");
            }
        }
        return super.convertFromMO(storedPasswordMO, strict, secretsEncryptor);
    }

    @NotNull
    @Override
    public StoredPasswordMO convertToMO(@NotNull SecurePassword securePassword, SecretsEncryptor secretsEncryptor) {
        StoredPasswordMO mo =  super.convertToMO(securePassword, secretsEncryptor);
        if(secretsEncryptor !=null){
            // decrypt and encrypt password.
            try {
                String decryptedPassword = new String(passwordManager.decryptPassword(securePassword.getEncodedPassword()));
                mo.setPassword(secretsEncryptor.encryptSecret(decryptedPassword.getBytes(Charsets.UTF8)),secretsEncryptor.getWrappedBundleKey());
            } catch ( FindException | ParseException e) {
                throw new ResourceFactory.ResourceAccessException("Error retrieving password: " +e.getMessage(),e);
            }

        }
        return mo;
    }
}
