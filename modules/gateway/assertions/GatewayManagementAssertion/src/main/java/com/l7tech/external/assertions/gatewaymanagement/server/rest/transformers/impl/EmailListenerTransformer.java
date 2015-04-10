package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EmailListenerResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.util.Charsets;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.ParseException;

@Component
public class EmailListenerTransformer extends APIResourceWsmanBaseTransformer<EmailListenerMO, EmailListener, EntityHeader, EmailListenerResourceFactory> {

    @Override
    @Inject
    @Named("emailListenerResourceFactory")
    protected void setFactory(EmailListenerResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<EmailListenerMO> convertToItem(@NotNull EmailListenerMO m) {
        return new ItemBuilder<EmailListenerMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public EntityContainer<EmailListener> convertFromMO(@NotNull EmailListenerMO emailListenerMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        if(emailListenerMO.getPassword()!=null && emailListenerMO.getPasswordBundleKey() != null){
            try {
                emailListenerMO.setPassword(new String(secretsEncryptor.decryptSecret(emailListenerMO.getPassword(), emailListenerMO.getPasswordBundleKey()), Charsets.UTF8));
            } catch (ParseException e) {
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Failed to decrypt password");
            }
        }
        return super.convertFromMO(emailListenerMO, strict, secretsEncryptor);
    }

    @NotNull
    @Override
    public EmailListenerMO convertToMO(@NotNull EmailListener emailListener, SecretsEncryptor secretsEncryptor) {
        EmailListenerMO mo =  super.convertToMO(emailListener, secretsEncryptor);
        if( secretsEncryptor !=null &&mo.getPassword() == null ){
            mo.setPassword(secretsEncryptor.encryptSecret(emailListener.getPassword().getBytes(Charsets.UTF8)),secretsEncryptor.getWrappedBundleKey());
        }
        return mo;
    }
}
