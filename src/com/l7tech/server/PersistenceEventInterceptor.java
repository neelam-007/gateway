/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.identity.internal.GroupMembership;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.objectmodel.TransactionListener;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.EventManager;
import com.l7tech.server.event.GroupMembershipEvent;
import com.l7tech.server.event.GroupMembershipEventInfo;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.service.resolution.ResolutionParameters;
import net.sf.hibernate.CallbackException;
import net.sf.hibernate.Interceptor;
import net.sf.hibernate.type.Type;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.ApplicationContext;

/**
 * Notices when any persistent {@link Entity} is saved, updated or deleted, and creates and fires
 * corresponding {@link Updated}, {@link Deleted} and {@link Created} events,
 * if and when the current transaction commits.
 *
 * @see HibernatePersistenceContext#registerTransactionListener(com.l7tech.objectmodel.TransactionListener)
 * @author alex
 * @version $Revision$
 */
public class PersistenceEventInterceptor implements Interceptor {
    private ApplicationContext springContext;

    public PersistenceEventInterceptor(ApplicationContext springContext) {
        ignoredClassNames = new HashSet();
        ignoredClassNames.add(SSGLogRecord.class.getName());
        ignoredClassNames.add(ClusterNodeInfo.class.getName());
        ignoredClassNames.add(ResolutionParameters.class.getName());
        ignoredClassNames.add(AdminAuditRecord.class.getName());
        ignoredClassNames.add(SystemAuditRecord.class.getName());
        ignoredClassNames.add(MessageSummaryAuditRecord.class.getName());

        if (springContext == null) {
            throw new IllegalArgumentException("Spring Context is required");
        }
        this.springContext = springContext;
    }

    private final Set ignoredClassNames;

    private boolean ignored( Object entity ) {
        if (!(entity instanceof Entity || entity instanceof GroupMembership)) return true;
        return ignoredClassNames.contains(entity.getClass().getName());
    }

    /**
     * Detects updates and fires an {@link com.l7tech.server.event.admin.Updated} event if the entity isn't {@link #ignored} and the update is committed
     */
    public boolean onFlushDirty( final Object entity, final Serializable id, final Object[] currentState, final Object[] previousState, final String[] propertyNames, Type[] types ) throws CallbackException {
        if (!ignored( entity )) {
            try {
                HibernatePersistenceContext.getCurrent().registerTransactionListener(new TransactionListener() {
                    public void postCommit() {
                        logger.log(Level.FINE, "Updated " + entity.getClass().getName() + " " + id );
                        EntityChangeSet changes = new EntityChangeSet(propertyNames, previousState, currentState);
                        try {
                            EventManager.fireInNewTransaction(updatedEvent(entity, changes));
                        } catch (TransactionException e) {
                            logger.log(Level.SEVERE, "Couldn't commit audit transaction", e);
                        }
                    }

                    public void postRollback() { }
                });
            } catch ( TransactionException e ) {
                logger.log( Level.WARNING, e.getMessage(), e );
            } catch ( SQLException e ) {
                logger.log( Level.WARNING, e.getMessage(), e );
            }
        }
        return false;
    }

    /**
     * Detects saves and fires a {@link com.l7tech.server.event.admin.Created} event if the entity isn't {@link #ignored} and the save is committed
     */
    public boolean onSave( final Object entity, final Serializable id, Object[] state, String[] propertyNames, Type[] types ) throws CallbackException {
        if (!ignored( entity )) {
            try {
                HibernatePersistenceContext.getCurrent().registerTransactionListener(new TransactionListener() {
                    public void postCommit() {
                        logger.log(Level.FINE, "Created " + entity.getClass().getName() + " " + id );
                        try {
                            EventManager.fireInNewTransaction(createdEvent(entity));
                        } catch (TransactionException e) {
                            logger.log(Level.SEVERE, "Couldn't commit audit transaction", e);
                        }
                    }

                    public void postRollback() { }
                });
            } catch ( TransactionException e ) {
                logger.log( Level.WARNING, e.getMessage(), e );
            } catch ( SQLException e ) {
                logger.log( Level.WARNING, e.getMessage(), e );
            }
        }
        return false;
    }

    /**
     * Detects deletes and fires a {@link Deleted} event if the entity isn't {@link #ignored} and the deletion is committed
     */
    public void onDelete( final Object entity, final Serializable id, Object[] state, String[] propertyNames, Type[] types ) throws CallbackException {
        if (!ignored( entity )) {
            try {
                HibernatePersistenceContext.getCurrent().registerTransactionListener(new TransactionListener() {
                    public void postCommit() {
                        logger.log(Level.FINE, "Deleted " + entity.getClass().getName() + " " + id );
                        try {
                            EventManager.fireInNewTransaction(deletedEvent(entity));
                        } catch (TransactionException e) {
                            logger.log(Level.SEVERE, "Couldn't commit audit transaction");
                        }
                    }

                    public void postRollback() { }
                });
            } catch ( TransactionException e ) {
                logger.log( Level.WARNING, e.getMessage(), e );
            } catch ( SQLException e ) {
                logger.log( Level.WARNING, e.getMessage(), e );
            }
        }
    }

    AdminEvent deletedEvent(Object obj) {
        if (obj instanceof Entity) {
            return new Deleted((Entity)obj);
        } else if (obj instanceof GroupMembership) {
            return new GroupMembershipEvent(new GroupMembershipEventInfo((GroupMembership)obj, "removed", springContext));
        } else throw new IllegalStateException("Can't make a Deleted event for a " + obj.getClass().getName());
    }

    AdminEvent createdEvent(Object obj) {
        if (obj instanceof Entity) {
            return new Created((Entity)obj);
        } else if (obj instanceof GroupMembership) {
            return new GroupMembershipEvent(new GroupMembershipEventInfo((GroupMembership)obj, "added", springContext));
        } else throw new IllegalStateException("Can't make a Created event for a " + obj.getClass().getName());
    }

    AdminEvent updatedEvent(Object obj, EntityChangeSet changes) {
        if (obj instanceof Entity) {
            return new Updated((Entity)obj, changes);
        } else if (obj instanceof GroupMembership) {
            return new GroupMembershipEvent(new GroupMembershipEventInfo((GroupMembership)obj, "updated", springContext));
        } else throw new IllegalStateException("Can't make an Updated event for a " + obj.getClass().getName());
    }


    /** Ignored */
    public boolean onLoad( Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types ) throws CallbackException {
        return false;
    }

    /** Ignored */
    public void preFlush( Iterator entities ) throws CallbackException {
    }

    /** Ignored */
    public void postFlush( Iterator entities ) throws CallbackException {
    }

    /** Ignored */
    public Boolean isUnsaved( Object entity ) {
        return null;
    }

    /** Ignored */
    public int[] findDirty( Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types ) {
        return null;
    }

    /** Ignored */
    public Object instantiate( Class clazz, Serializable id ) throws CallbackException {
        return null;
    }

    /** Ignored */
    private static Logger logger = Logger.getLogger(PersistenceEventInterceptor.class.getName());
}
