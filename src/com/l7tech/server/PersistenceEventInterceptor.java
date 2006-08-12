/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ServiceUsage;
import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.common.security.rbac.AttributePredicate;
import com.l7tech.common.security.rbac.ObjectIdentityPredicate;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.common.security.rbac.ScopePredicate;
import com.l7tech.identity.GroupMembership;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.Entity;
import com.l7tech.server.event.*;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.identity.cert.CertEntryRow;
import com.l7tech.server.service.resolution.ResolutionParameters;
import com.l7tech.service.MetricsBin;
import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Notices when any persistent {@link com.l7tech.objectmodel.PersistentEntity} is saved, updated or deleted, and creates and
 * fires corresponding {@link Updated}, {@link Deleted} and {@link Created} events,
 * if and when the current transaction commits.
 *
 * @author alex
 * @version $Revision$
 */
public class PersistenceEventInterceptor extends ApplicationObjectSupport implements Interceptor {
    private static Logger logger = Logger.getLogger(PersistenceEventInterceptor.class.getName());

    public PersistenceEventInterceptor() {
        ignoredClassNames = new HashSet<String>();
        ignoredClassNames.add(SSGLogRecord.class.getName());
        ignoredClassNames.add(ClusterNodeInfo.class.getName());
        ignoredClassNames.add(ResolutionParameters.class.getName());
        ignoredClassNames.add(AdminAuditRecord.class.getName());
        ignoredClassNames.add(SystemAuditRecord.class.getName());
        ignoredClassNames.add(MessageSummaryAuditRecord.class.getName());
        ignoredClassNames.add(AuditDetail.class.getName());
        ignoredClassNames.add(MetricsBin.class.getName());
        ignoredClassNames.add(ClusterNodeInfo.class.getName());

        ignoredClassNames.add(Permission.class.getName());
        ignoredClassNames.add(ScopePredicate.class.getName());
        ignoredClassNames.add(ObjectIdentityPredicate.class.getName());
        ignoredClassNames.add(AttributePredicate.class.getName());
        ignoredClassNames.add(ServiceUsage.class.getName());
    }

    private final Set<String> ignoredClassNames;

    private boolean ignored(Object entity) {
        if (!(entity instanceof PersistentEntity)) return true;
        return ignoredClassNames.contains(entity.getClass().getName());
    }

    /**
     * Detects saves and fires a {@link com.l7tech.server.event.admin.Created} event if the entity isn't {@link #ignored} and the save is committed
     */
    public boolean onSave(final Object entity, final Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        if (!ignored(entity)) {
            logger.log(Level.FINE, "Created " + entity.getClass().getName() + " " + id);
            getApplicationContext().publishEvent(createdEvent(entity));
        }
        return false;
    }

    /**
     * Detects deletes and fires a {@link Deleted} event if the entity isn't {@link #ignored} and the deletion is committed
     */
    public void onDelete(final Object entity, final Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        if (!ignored(entity)) {
            logger.log(Level.FINE, "Deleted " + entity.getClass().getName() + " " + id);
            getApplicationContext().publishEvent(deletedEvent(entity));
        }
    }

    /**
     * Detects updates and fires an {@link com.l7tech.server.event.admin.Updated} event if the entity isn't {@link #ignored} and the update is committed
     */
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
        if (!ignored(entity)) {
            logger.log(Level.FINE, "Updated " + entity.getClass().getName() + " " + id);
            EntityChangeSet changes = new EntityChangeSet(propertyNames, previousState, currentState);
            getApplicationContext().publishEvent(updatedEvent(entity, changes));
        }
        return false;
    }


    private AdminEvent deletedEvent(Object obj) {
        if (obj instanceof GroupMembership) {
            return new GroupMembershipEvent(new GroupMembershipEventInfo((GroupMembership)obj, "removed", getApplicationContext()));
        } else if (obj instanceof CertEntryRow) {
            return new UserCertEvent(new UserCertEventInfo((CertEntryRow)obj, "removed", null, getApplicationContext()));
        } else if (obj instanceof Entity) {
            return new Deleted((Entity)obj);
        } else
            throw new IllegalStateException("Can't make a Deleted event for a " + obj.getClass().getName());
    }

    private AdminEvent createdEvent(Object obj) {
        if (obj instanceof CertEntryRow) {
            return new UserCertEvent(new UserCertEventInfo((CertEntryRow)obj, "added", null, getApplicationContext()));
        } else if (obj instanceof GroupMembership) {
            return new GroupMembershipEvent(new GroupMembershipEventInfo((GroupMembership)obj, "added", getApplicationContext()));
        } else if (obj instanceof Entity) {
            return new Created((Entity)obj);
        } else
            throw new IllegalStateException("Can't make a Created event for a " + obj.getClass().getName());
    }

    private AdminEvent updatedEvent(Object obj, EntityChangeSet changes) {
        if (obj instanceof CertEntryRow) {
            return new UserCertEvent(new UserCertEventInfo((CertEntryRow)obj, "updated", changes, getApplicationContext()));
        } else if (obj instanceof GroupMembership) {
            return new GroupMembershipEvent(new GroupMembershipEventInfo((GroupMembership)obj, "updated", getApplicationContext()));
        } else if (obj instanceof Entity) {
            return new Updated((Entity)obj, changes);
        } else
            throw new IllegalStateException("Can't make an Updated event for a " + obj.getClass().getName());
    }

    /** Ignored */
    public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
    }

    /** Ignored */
    public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
    }

    /** Ignored */
    public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
    }

    /** Ignored */
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        return false;
    }

    /** Ignored */
    public void preFlush(Iterator entities) throws CallbackException {
    }

    /** Ignored */
    public void postFlush(Iterator entities) throws CallbackException {
    }

    /** Ignored */
    public Boolean isTransient(Object entity) {
        return null;
    }

    /** Ignored */
    public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        return null;
    }

    /** Ignored */
    public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
        return null;
    }

    /** Ignored */
    public String getEntityName(Object object) throws CallbackException {
        return null;
    }

    /** Ignored */
    public Object getEntity(String entityName, Serializable id) throws CallbackException {
        return null;
    }

    /** Ignored */
    public void afterTransactionBegin(Transaction tx) {
    }

    /** Ignored */
    public void beforeTransactionCompletion(Transaction tx) {
    }

    /** Ignored */
    public void afterTransactionCompletion(Transaction tx) {
    }

    /** Ignored */
    public String onPrepareStatement(String sql) {
        return sql;
    }

}
