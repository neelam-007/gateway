/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.identity.*;
import com.l7tech.service.PublishedService;
import com.l7tech.service.SampleMessage;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.alert.AlertEvent;
import com.l7tech.common.alert.Notification;

import java.io.Serializable;

/**
 * @author alex
 * @version $Revision$
 */
public class AnonymousEntityReference implements NamedEntity, Serializable {
    public AnonymousEntityReference(Class entityClass, long oid) {
        this(entityClass, oid, null);
    }

    public AnonymousEntityReference(Class entityClass, String uniqueId) {
        this(entityClass, uniqueId, null);
    }

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
            throw new IllegalArgumentException("Can't get reference to " + header.toString());
        } else if (type == EntityType.UNDEFINED) {
            throw new IllegalArgumentException("Can't get reference to " + header.toString());
        } else {
            throw new IllegalArgumentException("Can't get reference to " + header.toString());
        }
    }

    public AnonymousEntityReference(Class entityClass, String uniqueId, String name) {
        this.entityClass = entityClass;
        this.uniqueId = uniqueId;
        this.name = name;
        try {
            this.oid = Long.valueOf(uniqueId).longValue();
        } catch (NumberFormatException e) {
        }
    }

    public AnonymousEntityReference(Class entityClass, long oid, String name) {
        this.entityClass = entityClass;
        this.oid = oid;
        this.name = name;
        this.uniqueId = Long.toString(oid);
    }

    public Class getEntityClass() {
        return entityClass;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public long getOid() {
        return oid;
    }

    public String getName() {
        if (name != null) return name;
        return entityClass.getSimpleName() + " #" + oid;
    }

    public String toString() {
        return getName();
    }

    public int getVersion() {
        return 0;
    }

    /** @deprecated */
    public void setVersion(int version) {
        throw new UnsupportedOperationException();
    }

    /** @deprecated */
    public void setOid(long oid) {
        throw new UnsupportedOperationException();
    }

    /** @deprecated */
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    private static final long serialVersionUID = 5741062976640065826L;

    private final Class entityClass;
    private final String name;
    protected String uniqueId;
    private long oid;
}
