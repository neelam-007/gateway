package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.JMSDestinationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.JMSDestinationMO;
import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.JmsEndpointHeader;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.bundling.JmsContainer;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.namespace.QName;
import java.text.ParseException;
import java.util.Iterator;

@Component
public class JMSDestinationTransformer extends APIResourceWsmanBaseTransformer<JMSDestinationMO, JmsEndpoint, JmsEndpointHeader, JMSDestinationResourceFactory> {

    @Inject
    protected JmsConnectionManager jmsConnectionManager;

    @Override
    @Inject
    @Named("jmsDestinationResourceFactory")
    protected void setFactory(JMSDestinationResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<JMSDestinationMO> convertToItem(@NotNull JMSDestinationMO m) {
        return new ItemBuilder<JMSDestinationMO>(m.getJmsDestinationDetail().getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @NotNull
    @Override
    public JmsContainer convertFromMO(@NotNull JMSDestinationMO jmsDestinationMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {

        if(jmsDestinationMO.getJmsConnection().getContextPropertiesTemplate()!=null && jmsDestinationMO.getJmsConnection().getContextPropertiesTemplate().containsKey("java.naming.security.credentials") ){
            final Object keyVal = jmsDestinationMO.getJmsConnection().getContextPropertiesTemplate().get("java.naming.security.credentials");
            if(keyVal instanceof AttributeExtensibleType.AttributeExtensibleString){
                final AttributeExtensibleType.AttributeExtensibleString password = (AttributeExtensibleType.AttributeExtensibleString) keyVal;
                //check that the bundle key is specified.
                if (password.getAttributeExtensions() != null && password.getAttributeExtensions().get(new QName("bundleKey")) != null) {
                    try {
                        //decrypt the password and set it on the MO in plain text
                        jmsDestinationMO.getJmsConnection().getContextPropertiesTemplate().put("java.naming.security.credentials", new String(secretsEncryptor.decryptSecret(password.getValue(), (String) password.getAttributeExtensions().get(new QName("bundleKey"))), Charsets.UTF8));
                    } catch (ParseException e) {
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Failed to decrypt password");
                    }
                } else {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Jms destination contains an encrypted password but the bundle key to decrypt it with is missing.");
                }
            }
        }

        if(jmsDestinationMO.getJmsConnection().getProperties()!=null && jmsDestinationMO.getJmsConnection().getProperties().containsKey("password") ){
            final Object keyVal = jmsDestinationMO.getJmsConnection().getProperties().get("password");
            if(keyVal instanceof AttributeExtensibleType.AttributeExtensibleString){
                final AttributeExtensibleType.AttributeExtensibleString password = (AttributeExtensibleType.AttributeExtensibleString) keyVal;
                //check that the bundle key is specified.
                if (password.getAttributeExtensions() != null && password.getAttributeExtensions().get(new QName("bundleKey")) != null) {
                    try {
                        //decrypt the password and set it on the MO in plain text
                        jmsDestinationMO.getJmsConnection().getProperties().put("password", new String(secretsEncryptor.decryptSecret(password.getValue(), (String) password.getAttributeExtensions().get(new QName("bundleKey"))), Charsets.UTF8));
                    } catch (ParseException e) {
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Failed to decrypt password");
                    }
                } else {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Jms destination contains an encrypted password but the bundle key to decrypt it with is missing.");
                }
            }
        }

        if(jmsDestinationMO.getJmsDestinationDetail().getProperties()!=null && jmsDestinationMO.getJmsDestinationDetail().getProperties().containsKey("password") ){
            final Object keyVal = jmsDestinationMO.getJmsDestinationDetail().getProperties().get("password");
            if(keyVal instanceof AttributeExtensibleType.AttributeExtensibleString){
                final AttributeExtensibleType.AttributeExtensibleString password = (AttributeExtensibleType.AttributeExtensibleString) keyVal;
                //check that the bundle key is specified.
                if (password.getAttributeExtensions() != null && password.getAttributeExtensions().get(new QName("bundleKey")) != null) {
                    try {
                        //decrypt the password and set it on the MO in plain text
                        jmsDestinationMO.getJmsDestinationDetail().getProperties().put("password", new String(secretsEncryptor.decryptSecret(password.getValue(), (String) password.getAttributeExtensions().get(new QName("bundleKey"))), Charsets.UTF8));
                    } catch (ParseException e) {
                        throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Failed to decrypt password");
                    }
                } else {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES,"Jms destination contains an encrypted password but the bundle key to decrypt it with is missing.");
                }
            }
        }

        Iterator<PersistentEntity> entities =  factory.fromResourceAsBag(jmsDestinationMO,strict).iterator();
        JmsEndpoint jmsEndpoint = (JmsEndpoint) entities.next();
        JmsConnection jmsConnection = (JmsConnection) entities.next();
        jmsEndpoint.setConnectionGoid(jmsConnection.getGoid());

        if(jmsDestinationMO.getId() != null){
            //set the entity id as it is not always set
            jmsEndpoint.setGoid(Goid.parseGoid(jmsDestinationMO.getId()));
        }
        return new JmsContainer(jmsEndpoint, jmsConnection);
    }

    @NotNull
    @Override
    public JMSDestinationMO convertToMO(@NotNull JmsEndpoint jmsEndpoint, SecretsEncryptor secretsEncryptor) {
        JMSDestinationMO mo =  super.convertToMO(jmsEndpoint, secretsEncryptor);
        JmsConnection jmsConnection;
        try{
            jmsConnection = jmsConnectionManager.findByPrimaryKey(jmsEndpoint.getConnectionGoid());
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException("JmsConnection not found " + jmsEndpoint.getConnectionGoid());
        }

        if( secretsEncryptor !=null){
            if(!mo.getJmsConnection().getProperties().containsKey("password") && jmsConnection.getPassword()!=null){
                AttributeExtensibleType.AttributeExtensibleString passValue = new AttributeExtensibleType.AttributeExtensibleString();
                passValue.setValue(secretsEncryptor.encryptSecret(jmsConnection.getPassword().getBytes(Charsets.UTF8)));
                passValue.setAttributeExtensions(CollectionUtils.<QName, Object>mapBuilder().put(new QName("bundleKey"), secretsEncryptor.getWrappedBundleKey()).map());
                mo.getJmsConnection().getProperties().put("password", passValue);
            }

            if(!mo.getJmsDestinationDetail().getProperties().containsKey("password") && jmsEndpoint.getPassword()!=null){
                AttributeExtensibleType.AttributeExtensibleString passValue = new AttributeExtensibleType.AttributeExtensibleString();
                passValue.setValue(secretsEncryptor.encryptSecret(jmsEndpoint.getPassword().getBytes(Charsets.UTF8)));
                passValue.setAttributeExtensions(CollectionUtils.<QName, Object>mapBuilder().put(new QName("bundleKey"), secretsEncryptor.getWrappedBundleKey()).map());
                mo.getJmsDestinationDetail().getProperties().put("password", passValue);
            }

            if( !mo.getJmsConnection().getContextPropertiesTemplate().containsKey("java.naming.security.credentials")  && jmsConnection.properties()!=null && jmsConnection.properties().containsKey("java.naming.security.credentials") ) {
                AttributeExtensibleType.AttributeExtensibleString passValue = new AttributeExtensibleType.AttributeExtensibleString();
                passValue.setValue(secretsEncryptor.encryptSecret(jmsConnection.properties().getProperty("java.naming.security.credentials").getBytes(Charsets.UTF8)));
                passValue.setAttributeExtensions(CollectionUtils.<QName, Object>mapBuilder().put(new QName("bundleKey"), secretsEncryptor.getWrappedBundleKey()).map());
                mo.getJmsConnection().getContextPropertiesTemplate().put("java.naming.security.credentials", passValue);
            }
        }
        return mo;
    }
}
