/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.objectmodel.TransactionListener;
import com.l7tech.objectmodel.event.Created;
import com.l7tech.objectmodel.event.Deleted;
import com.l7tech.objectmodel.event.EntityChangeSet;
import com.l7tech.objectmodel.event.Updated;
import com.l7tech.server.event.EventManager;
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

/**
 * @author alex
 * @version $Revision$
 */
public class PersistenceEventInterceptor implements Interceptor {
    public PersistenceEventInterceptor() {
        ignoredClassNames = new HashSet();
        ignoredClassNames.add(SSGLogRecord.class.getName());
        ignoredClassNames.add(ClusterNodeInfo.class.getName());
        ignoredClassNames.add(ResolutionParameters.class.getName());
    }

    private final Set ignoredClassNames;

    public boolean onLoad( Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types ) throws CallbackException {
        return false;
    }

    public boolean onFlushDirty( final Object entity, final Serializable id, final Object[] currentState, final Object[] previousState, final String[] propertyNames, Type[] types ) throws CallbackException {
        if (!ignored( entity )) {
            try {
                HibernatePersistenceContext.getCurrent().registerTransactionListener(new TransactionListener() {
                    public void postCommit() {
                        logger.log(Level.FINE, "Updated " + entity.getClass().getName() + " " + id );
                        EntityChangeSet changes = new EntityChangeSet(propertyNames, previousState, currentState);
                        Updated event = new Updated((Entity)entity, changes);
                        EventManager.fire(event);
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

    private boolean ignored( Object entity ) {
        if (!(entity instanceof Entity)) return true;
        return ignoredClassNames.contains(entity.getClass().getName());
    }

    public boolean onSave( final Object entity, final Serializable id, Object[] state, String[] propertyNames, Type[] types ) throws CallbackException {
        if (!ignored( entity )) {
            try {
                HibernatePersistenceContext.getCurrent().registerTransactionListener(new TransactionListener() {
                    public void postCommit() {
                        logger.log(Level.FINE, "Created " + entity.getClass().getName() + " " + id );
                        Created event = new Created((Entity)entity);
                        EventManager.fire(event);
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

    public void onDelete( final Object entity, final Serializable id, Object[] state, String[] propertyNames, Type[] types ) throws CallbackException {
        if (!ignored( entity )) {
            try {
                HibernatePersistenceContext.getCurrent().registerTransactionListener(new TransactionListener() {
                    public void postCommit() {
                        logger.log(Level.FINE, "Deleted " + entity.getClass().getName() + " " + id );
                        Deleted event = new Deleted((Entity)entity);
                        EventManager.fire(event);
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

    public void preFlush( Iterator entities ) throws CallbackException {
    }

    public void postFlush( Iterator entities ) throws CallbackException {
    }

    public Boolean isUnsaved( Object entity ) {
        return null;
    }

    public int[] findDirty( Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types ) {
        return null;
    }

    public Object instantiate( Class clazz, Serializable id ) throws CallbackException {
        return null;
    }

    private static Logger logger = Logger.getLogger(PersistenceEventInterceptor.class.getName());
}
