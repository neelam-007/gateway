/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.event.*;
import com.l7tech.server.service.ServiceEvent;
import com.l7tech.service.PublishedService;
import com.l7tech.identity.User;
import net.jini.export.ServerContext;
import net.jini.io.context.ClientSubject;

import java.rmi.server.ServerNotActiveException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class AdminAuditListener implements CreateListener, UpdateListener, DeleteListener {
    private static String nodeId = ClusterInfoManager.getInstance().thisNodeId();
    public static final Level DEFAULT_LEVEL = Level.INFO;

    public static class LevelMapping {
        public LevelMapping(Class entityClass, Map eventClassesToLevels) {
            if (entityClass == null || eventClassesToLevels == null) throw new IllegalArgumentException("Args must not be null");
            if (!Entity.class.isAssignableFrom(entityClass)) throw new IllegalArgumentException(Entity.class.getName() + " is not assignable from " + entityClass.getName());
            for ( Iterator i = eventClassesToLevels.keySet().iterator(); i.hasNext(); ) {
                Class eventClass = (Class) i.next();
                if (!Event.class.isAssignableFrom(eventClass)) throw new IllegalArgumentException(Event.class.getName() + " is not assignable from " + eventClass.getName());
            }
            this.entityClass = entityClass;
            this.eventClassesToLevels = eventClassesToLevels;
        }

        public Class getEntityClass() {
            return entityClass;
        }

        public Map getEventClassesToLevels() {
            return eventClassesToLevels;
        }

        private final Class entityClass;
        private final Map eventClassesToLevels;
    }

    public AdminAuditListener() {
        auditRecordManager = (AuditRecordManager)Locator.getDefault().lookup(AuditRecordManager.class);
        if (auditRecordManager == null) throw new IllegalStateException("Couldn't locate AuditRecordManager");

        Map levels;

        levels = new HashMap();
        levels.put(Deleted.class, Level.WARNING);
        levels.put(ServiceEvent.Disabled.class, Level.WARNING);
        LevelMapping lm = new LevelMapping(PublishedService.class, levels);
        levelMappings.put(PublishedService.class.getName(), lm);

        levels = new HashMap();
        levels.put(Deleted.class, Level.WARNING);
        levels.put(Updated.class, Level.WARNING);
        levels.put(Created.class, Level.WARNING);
    }

    private Level level(PersistenceEvent event) {
        Entity ent = event.getEntity();
        LevelMapping lm = (LevelMapping) levelMappings.get(ent.getClass());
        Level level = DEFAULT_LEVEL;
        if (lm != null) {
            Map map = lm.getEventClassesToLevels();
            Level temp = (Level)map.get(event.getClass());
            if (temp != null) level = temp;
        }
        return level;
    }

    private String getAdminLogin() {
        ClientSubject clientSubject = null;
        try {
            clientSubject = (ClientSubject)ServerContext.getServerContextElement(ClientSubject.class);
        } catch ( ServerNotActiveException e ) {
            return null;
        }
        if (clientSubject != null) {
            Set principals = clientSubject.getClientSubject().getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                Principal p = (Principal)principals.iterator().next();
                String login = null;
                if (p instanceof User) login = ((User)p).getLogin();
                if (login == null) login = p.getName();
                return login;
            }
        }
        return null;
    }

    public void entityCreated( Created created ) {
        String adminLogin = getAdminLogin();
        if (adminLogin == null) return;
        try {
            auditRecordManager.save(new AdminAuditRecord(level(created), nodeId, created, adminLogin));
        } catch ( SaveException e ) {
            logger.log( Level.SEVERE, "Couldn't save " + created, e );
        }
    }

    public void entityUpdated( Updated updated ) {
        String adminLogin = getAdminLogin();
        if (adminLogin == null) return;
        try {
            auditRecordManager.save(new AdminAuditRecord(level(updated), nodeId, updated, adminLogin));
        } catch ( SaveException e ) {
            logger.log( Level.SEVERE, "Couldn't save " + updated, e );
        }
    }

    public void entityDeleted( Deleted deleted ) {
        String adminLogin = getAdminLogin();
        if (adminLogin == null) return;
        try {
            auditRecordManager.save(new AdminAuditRecord(level(deleted), nodeId, deleted, adminLogin));
        } catch ( SaveException e ) {
            logger.log( Level.SEVERE, "Couldn't save " + deleted, e );
        }
    }

    private Map levelMappings = new HashMap();
    private final AuditRecordManager auditRecordManager;
    private static final Logger logger = Logger.getLogger(AdminAuditListener.class.getName());
}
