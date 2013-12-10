package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.JMSDestinationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.JMSConnection;
import com.l7tech.gateway.api.JMSDestinationDetail;
import com.l7tech.gateway.api.JMSDestinationMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class JMSDestinationRestResourceFactory extends WsmanBaseResourceFactory<JMSDestinationMO, JMSDestinationResourceFactory> {

    public JMSDestinationRestResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("enabled", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("disabled", new Functions.UnaryThrows<Boolean, String, IllegalArgumentException>() {
                            @Override
                            public Boolean call(String s) throws IllegalArgumentException {
                                return !Boolean.valueOf(s);
                            }
                        }))
                        .put("inbound", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("messageSource", RestResourceFactoryUtils.booleanConvert))
                        .put("template", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("template", RestResourceFactoryUtils.booleanConvert))
                        .put("destination", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("destinationName", RestResourceFactoryUtils.stringConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @Override
    @Inject
    public void setFactory(JMSDestinationResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public JMSDestinationMO getResourceTemplate() {
        JMSDestinationMO jmsDestinationMO = ManagedObjectFactory.createJMSDestination();
        JMSDestinationDetail jmsDetails = ManagedObjectFactory.createJMSDestinationDetails();
        JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();

        jmsDetails.setName("TemplateJMSDestination");
        jmsDetails.setDestinationName("TemplateDestinationName");
        jmsConnection.setProviderType(JMSConnection.JMSProviderType.TIBCO_EMS);

        jmsDestinationMO.setJmsDestinationDetail( jmsDetails );
        jmsDestinationMO.setJmsConnection( jmsConnection );
        return jmsDestinationMO;

    }
}
