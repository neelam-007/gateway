package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EmailListenerResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class EmailListenerAPIResourceFactory extends WsmanBaseResourceFactory<EmailListenerMO, EmailListenerResourceFactory> {

    public EmailListenerAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.EMAIL_LISTENER.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.EmailListenerResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public EmailListenerMO getResourceTemplate() {
        EmailListenerMO emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName("TemplateEmailListener");
        emailListenerMO.setHostname("hostName");
        emailListenerMO.setPort(1234);
        emailListenerMO.setActive(true);
        emailListenerMO.setUsername("username");
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.POP3);
        emailListenerMO.setFolder("INBOX");
        emailListenerMO.setPollInterval(1000);
        emailListenerMO.setUseSsl(false);
        emailListenerMO.setDeleteOnReceive(true);
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(EmailListener.PROP_HARDWIRED_SERVICE_ID, new Goid(123, 456).toString())
                .put(EmailListener.PROP_IS_HARDWIRED_SERVICE, Boolean.TRUE.toString()).map());
        return emailListenerMO;

    }
}
