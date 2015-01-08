package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.JMSDestinationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.JMSDestinationMO;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.*;
import com.l7tech.server.bundling.JmsContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Iterator;

@Component
public class JMSDestinationTransformer extends APIResourceWsmanBaseTransformer<JMSDestinationMO, JmsEndpoint, JmsEndpointHeader, JMSDestinationResourceFactory> {

    @Override
    @Inject
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
}
