package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.SecurePasswordResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.util.MasterPasswordManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class SecurePasswordTransformer extends APIResourceWsmanBaseTransformer<StoredPasswordMO, SecurePassword,EntityHeader, SecurePasswordResourceFactory> {

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
    public StoredPasswordMO convertToMO(@NotNull SecurePassword securePassword, MasterPasswordManager passwordManager) {
        StoredPasswordMO mo =  super.convertToMO(securePassword, passwordManager);
        if(passwordManager!=null){
            // todo encrypt and attach password
//            mo.setPassword(passwordManager.encryptPassword(securePassword.getEncodedPassword()));
        }
        return mo;
    }
}
