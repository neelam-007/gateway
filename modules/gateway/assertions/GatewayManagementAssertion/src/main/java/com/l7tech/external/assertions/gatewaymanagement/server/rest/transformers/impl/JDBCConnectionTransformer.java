package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.JDBCConnectionResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.JDBCConnectionMO;
import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.text.ParseException;
import java.util.regex.Matcher;

@Component
public class JDBCConnectionTransformer extends APIResourceWsmanBaseTransformer<JDBCConnectionMO, JdbcConnection, EntityHeader, JDBCConnectionResourceFactory> {

    @Override
    @Inject
    protected void setFactory(JDBCConnectionResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<JDBCConnectionMO> convertToItem(@NotNull JDBCConnectionMO m) {
        return new ItemBuilder<JDBCConnectionMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public EntityContainer<JdbcConnection> convertFromMO(@NotNull JDBCConnectionMO jdbcConnectionMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        //if the password is set and it is in an encrypted form  decrypt it.
        if (jdbcConnectionMO.getConnectionProperties() != null && jdbcConnectionMO.getConnectionProperties().get("password") != null) {
            final Object passwordObject = jdbcConnectionMO.getConnectionProperties().get("password");
            if(passwordObject instanceof AttributeExtensibleType.AttributeExtensibleString){
                final AttributeExtensibleType.AttributeExtensibleString password = (AttributeExtensibleType.AttributeExtensibleString) passwordObject;
                //check that the bundle key is specified.
                if (password.getAttributeExtensions() != null && password.getAttributeExtensions().get(new QName("bundleKey")) != null) {
                    try {
                        //decrypt the password and set it on the connection MO in plain text
                        jdbcConnectionMO.getConnectionProperties().put("password", new String(secretsEncryptor.decryptSecret(password.getValue(), (String) password.getAttributeExtensions().get(new QName("bundleKey"))), Charsets.UTF8));
                    } catch (ParseException e) {
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Failed to decrypt password");
                    }
                } else {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"JDBCConnection contains an encrypted password but the bundle key to decrypt it with is missing.");
                }
            }
        }
        return super.convertFromMO(jdbcConnectionMO, strict, secretsEncryptor);
    }

    @NotNull
    @Override
    public JDBCConnectionMO convertToMO(@NotNull final JdbcConnection jdbcConnection, @Nullable final SecretsEncryptor secretsEncryptor) {
        final JDBCConnectionMO mo =  super.convertToMO(jdbcConnection, secretsEncryptor);
        if(secretsEncryptor !=null){
            // encrypt password.
            //first check if it is a secpass reference
            final Matcher matcher = ServerVariables.SINGLE_SECPASS_PATTERN.matcher(jdbcConnection.getPassword());
            if (!matcher.matches()) {
                //this is a hard coded password so encrypt it.
                final AttributeExtensibleType.AttributeExtensibleString password = new AttributeExtensibleType.AttributeExtensibleString();
                password.setValue(secretsEncryptor.encryptSecret(jdbcConnection.getPassword().getBytes(Charsets.UTF8)));
                password.setAttributeExtensions(CollectionUtils.<QName, Object>mapBuilder().put(new QName("bundleKey"), secretsEncryptor.getWrappedBundleKey()).map());
                mo.getConnectionProperties().put("password", password);
            }
        }
        return mo;
    }
}
