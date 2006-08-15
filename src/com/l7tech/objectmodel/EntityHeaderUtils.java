/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;
import com.l7tech.identity.AnonymousIdentityReference;
import com.l7tech.service.PublishedService;
import com.l7tech.service.SampleMessage;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.alert.AlertEvent;
import com.l7tech.common.alert.Notification;

/**
 * @author alex
 */
public final class EntityHeaderUtils {
    public static EntityType getEntityHeaderType(Class<? extends Entity> entityClass) {
        if (IdentityProviderConfig.class.isAssignableFrom(entityClass)) return EntityType.ID_PROVIDER_CONFIG;
        if (User.class.isAssignableFrom(entityClass)) return EntityType.USER;
        if (Group.class.isAssignableFrom(entityClass)) return EntityType.GROUP;
        if (PublishedService.class.isAssignableFrom(entityClass)) return EntityType.SERVICE;
        if (JmsConnection.class.isAssignableFrom(entityClass)) return EntityType.JMS_CONNECTION;
        if (JmsEndpoint.class.isAssignableFrom(entityClass)) return EntityType.JMS_ENDPOINT;
        if (TrustedCert.class.isAssignableFrom(entityClass)) return EntityType.TRUSTED_CERT;
        if (AlertEvent.class.isAssignableFrom(entityClass)) return EntityType.ALERT_TRIGGER;
        if (Notification.class.isAssignableFrom(entityClass)) return EntityType.ALERT_ACTION;
        if (SampleMessage.class.isAssignableFrom(entityClass)) return EntityType.SAMPLE_MESSAGE;
        if (Role.class.isAssignableFrom(entityClass)) return EntityType.RBAC_ROLE;
        return EntityType.UNDEFINED;
    }

    /**
     * Creates and returns an {@link com.l7tech.objectmodel.AnonymousEntityReference} that's as close a reflection of the given
     * {@link com.l7tech.objectmodel.EntityHeader} as possible.  <code>null</code> is returned only if the {@link com.l7tech.objectmodel.EntityHeader#type}
     * is {@link com.l7tech.objectmodel.EntityType#MAXED_OUT_SEARCH_RESULT}.  Otherwise, this method will create either an
     * {@link com.l7tech.objectmodel.AnonymousEntityReference}, an {@link com.l7tech.identity.AnonymousIdentityReference}, or throw an exception.
     * @param header the EntityHeader to translate
     * @return the anonymous entity reference, or null if the header was of type {@link com.l7tech.objectmodel.EntityType#MAXED_OUT_SEARCH_RESULT}
     */
    public static AnonymousEntityReference fromHeader(EntityHeader header) {
        EntityType type = header.getType();
        if (type == EntityType.ID_PROVIDER_CONFIG) {
            return new AnonymousEntityReference(IdentityProviderConfig.class, header.getOid(), header.getName());
        } else if (type == EntityType.USER || type == EntityType.GROUP) {
            long providerOid = IdentityProviderConfig.DEFAULT_OID;
            if (header instanceof IdentityHeader) {
                IdentityHeader identityHeader = (IdentityHeader) header;
                providerOid = identityHeader.getProviderOid();
            }
            Class clazz = type == EntityType.USER ? User.class : Group.class;
            return new AnonymousIdentityReference(clazz, header.getStrId(), providerOid, header.getName());
        } else if (type == EntityType.SERVICE) {
            return new AnonymousEntityReference(PublishedService.class, header.getOid(), header.getName());
        } else if (type == EntityType.JMS_CONNECTION) {
            return new AnonymousEntityReference(JmsConnection.class, header.getOid(), header.getName());
        } else if (type == EntityType.JMS_ENDPOINT) {
            return new AnonymousEntityReference(JmsEndpoint.class, header.getOid(), header.getName());
        } else if (type == EntityType.TRUSTED_CERT) {
            return new AnonymousEntityReference(TrustedCert.class, header.getOid(), header.getName());
        } else if (type == EntityType.ALERT_TRIGGER) {
            return new AnonymousEntityReference(AlertEvent.class, header.getOid(), header.getName());
        } else if (type == EntityType.ALERT_ACTION) {
            return new AnonymousEntityReference(Notification.class, header.getOid(), header.getName());
        } else if (type == EntityType.SAMPLE_MESSAGE) {
            return new AnonymousEntityReference(SampleMessage.class, header.getOid(), header.getName());
        } else if (type == EntityType.RBAC_ROLE) {
            return new AnonymousEntityReference(Role.class, header.getOid(), header.getName());
        } else if (type == EntityType.MAXED_OUT_SEARCH_RESULT) {
            return null;
        } else if (type == EntityType.UNDEFINED) {
            throw new IllegalArgumentException("Can't get reference to " + header.toString());
        } else {
            throw new IllegalArgumentException("Can't get reference to " + header.toString());
        }
    }

    private EntityHeaderUtils() { }
}
