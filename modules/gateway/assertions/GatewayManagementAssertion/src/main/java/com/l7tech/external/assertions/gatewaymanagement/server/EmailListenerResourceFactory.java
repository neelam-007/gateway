package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.email.EmailListenerManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
@ResourceFactory.ResourceType(type = EmailListenerMO.class)
public class EmailListenerResourceFactory extends SecurityZoneableEntityManagerResourceFactory<EmailListenerMO, EmailListener, EntityHeader> {

    //- PUBLIC

    public EmailListenerResourceFactory(final RbacServices rbacServices,
                                        final SecurityFilter securityFilter,
                                        final PlatformTransactionManager transactionManager,
                                        final EmailListenerManager emailListenerManager,
                                        final SecurityZoneManager securityZoneManager) {
        super(false, true, rbacServices, securityFilter, transactionManager, emailListenerManager, securityZoneManager);
        this.emailListenerManager = emailListenerManager;
    }

    //- PROTECTED

    @Override
    public EmailListenerMO asResource(EmailListener emailListener) {
        EmailListenerMO emailResource = ManagedObjectFactory.createEmailListener();

        emailResource.setId(emailListener.getId());
        emailResource.setName(emailListener.getName());
        emailResource.setActive(emailListener.isActive());
        emailResource.setHostname(emailListener.getHost());
        emailResource.setPort(emailListener.getPort());
        emailResource.setFolder(emailListener.getFolder());
        emailResource.setUsername(emailListener.getUsername());
        // omit password
        emailResource.setUseSsl(emailListener.isUseSsl());
        emailResource.setPollInterval(emailListener.getPollInterval());
        emailResource.setDeleteOnReceive(emailListener.isDeleteOnReceive());

        switch (emailListener.getServerType()) {
            case POP3:
                emailResource.setServerType(EmailListenerMO.EmailServerType.POP3);
                break;
            case IMAP:
                emailResource.setServerType(EmailListenerMO.EmailServerType.IMAP);
                break;
            default:
                throw new ResourceAccessException("Unknown email server type '" + emailListener.getServerType() + "'.");
        }

        Map<String, String> properties = new HashMap<>();
        for (String propertyName : emailListener.properties().stringPropertyNames()) {
            properties.put(propertyName, (String) emailListener.properties().get(propertyName));
        }
        emailResource.setProperties(properties);

        // handle SecurityZone
        doSecurityZoneAsResource(emailResource, emailListener);

        return emailResource;
    }

    @Override
    protected EmailListener fromResource(Object resource) throws InvalidResourceException {

        if (!(resource instanceof EmailListenerMO))
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected SiteMinder configuration");

        final EmailListenerMO emailResource = (EmailListenerMO) resource;

        final EmailListener emailListener;
        emailListener = new EmailListener();

        emailListener.setName(emailResource.getName());
        emailListener.setActive(emailResource.getActive());
        emailListener.setHost(emailResource.getHostname());
        emailListener.setPort(emailResource.getPort());
        emailListener.setFolder(emailResource.getFolder());
        emailListener.setUseSsl(emailResource.getUseSsl());
        emailListener.setDeleteOnReceive(emailResource.getDeleteOnReceive());
        emailListener.setPollInterval(emailResource.getPollInterval());
        emailListener.setUsername(emailResource.getUsername());
        emailListener.setPassword(emailResource.getPassword());

        switch ( emailResource.getServerType() ) {
            case POP3:
                emailListener.setServerType(EmailServerType.POP3);
                break;
            case IMAP:
                emailListener.setServerType(EmailServerType.IMAP);
                break;
            default:
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "email server type unknown");
        }


        Properties emailProperties = new Properties();
        if (emailResource.getProperties() != null) {
            for (Map.Entry<String, String> entry : emailResource.getProperties().entrySet()) {
                emailProperties.put(entry.getKey(), entry.getValue());
            }
        }
        emailListener.properties(emailProperties);

        // handle SecurityZone
        doSecurityZoneFromResource(emailResource, emailListener);

        return emailListener;
    }

    @Override
    protected void updateEntity(EmailListener oldEntity, EmailListener newEntity) throws InvalidResourceException {

        oldEntity.setName(newEntity.getName());
        oldEntity.setActive(newEntity.isActive());
        oldEntity.setHost(newEntity.getHost());
        oldEntity.setPort(newEntity.getPort());
        oldEntity.setFolder(newEntity.getFolder());
        oldEntity.setServerType(newEntity.getServerType());
        oldEntity.setUseSsl(newEntity.isUseSsl());
        oldEntity.setUsername(newEntity.getUsername());
        if (newEntity.getPassword() != null) {
            oldEntity.setPassword(newEntity.getPassword());
        }
        oldEntity.setPollInterval(newEntity.getPollInterval());
        oldEntity.setDeleteOnReceive(newEntity.isDeleteOnReceive());
        oldEntity.setSecurityZone(newEntity.getSecurityZone());

        Properties newProperties = new Properties();
        for (String prop : newEntity.properties().stringPropertyNames()) {
            newProperties.put(prop, newEntity.properties().get(prop));
        }
        oldEntity.properties(newProperties);

    }

    //- PRIVATE

    private EmailListenerManager emailListenerManager;

}
