/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.Event;
import com.l7tech.server.event.GenericListener;
import com.l7tech.server.event.admin.*;
import com.l7tech.server.service.ServiceEvent;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;

import javax.security.auth.Subject;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessController;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class AdminAuditListener implements GenericListener, CreateListener, UpdateListener, DeleteListener {
    private final ApplicationContext applicationContext;
    private final ClusterInfoManager clusterInfoManager;
    private final String nodeId;

    public void receive(Event event) {
        if (event instanceof PersistenceEvent) throw new IllegalArgumentException("PersistenceEvents should not be handled by receive()");
        if (!(event instanceof AdminEvent)) throw new IllegalArgumentException("Invalid event received--only AdminEvents should be handled here");
        final AuditContext currentAuditContext = getThreadAuditContext();
        currentAuditContext.setCurrentRecord(makeAuditRecord((AdminEvent)event));
        currentAuditContext.flush();
    }

    /**@return the thread local audit context. Obey the TLS semantics */
    private AuditContext getThreadAuditContext() {
        return (AuditContext)applicationContext.getBean("auditContext");
    }

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

    public AdminAuditListener(ApplicationContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Application Context is required");
        }
        applicationContext = ctx;
        clusterInfoManager = (ClusterInfoManager)applicationContext.getBean("clusterInfoManager");
        nodeId = clusterInfoManager.thisNodeId();
        Map levels = new HashMap();
        levels.put(Deleted.class, Level.WARNING);
        levels.put(ServiceEvent.Disabled.class, Level.WARNING);
        LevelMapping lm = new LevelMapping(PublishedService.class, levels);
        levelMappings.put(PublishedService.class.getName(), lm);

        levels = new HashMap();
        levels.put(Deleted.class, Level.WARNING);
        levels.put(Updated.class, Level.WARNING);
        levels.put(Created.class, Level.WARNING);
    }

    private Level level(AdminEvent event) {
        return DEFAULT_LEVEL;
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

    private AdminAuditRecord makeAuditRecord(AdminEvent event) {
        AdminInfo info = getAdminInfo();
        if (info == null) return null;

        return new AdminAuditRecord(level(event), nodeId, 0, "<none>", "", 'D', event.getNote(), info.login, info.ip);
    }

    private AdminAuditRecord makeAuditRecord(PersistenceEvent event) {
        final Entity entity = event.getEntity();

        String name = null;
        char action;
        long entityOid = entity.getOid();
        String entityClassname = entity.getClass().getName();
        if (entity instanceof NamedEntity) name = ((NamedEntity)entity).getName();

        int ppos = entityClassname.lastIndexOf(".");
        String localClassname = ppos >= 0 ? entityClassname.substring(ppos+1) : entityClassname;
        StringBuffer msg = new StringBuffer(localClassname);
        msg.append(" #").append(entityOid);
        if (entity instanceof NamedEntity) {
            msg.append(" (");
            msg.append(((NamedEntity)entity).getName());
            msg.append(")");
        } else {
            msg.append(entityOid);
        }

        if (event instanceof Created) {
            action = AdminAuditRecord.ACTION_CREATED;
            msg.append(" created");
        } else if (event instanceof Deleted) {
            action = AdminAuditRecord.ACTION_DELETED;
            msg.append(" deleted");
        } else if (event instanceof Updated) {
            action = AdminAuditRecord.ACTION_UPDATED;
            msg.append(" updated");
            EntityChangeSet changes = ((Updated)event).getChangeSet();

            List changeDescs = new ArrayList();
            for (Iterator i = changes.getProperties(); i.hasNext();) {
                String property = (String)i.next();
                if ("version".equals(property) || "disabled".equals(property)) continue;
                Object ovalue = changes.getOldValue(property);
                Object nvalue = changes.getNewValue(property);
                String verbed = null;
                if (isEmpty(ovalue)) {
                    if (!isEmpty(nvalue)) {
                        verbed = "set";
                    }
                } else if (isEmpty(nvalue)) {
                    if (!isEmpty(ovalue)) {
                        verbed = "cleared";
                    }
                } else if (!ovalue.equals(nvalue)) {
                    verbed = "changed";
                }

                if (verbed != null) changeDescs.add(verbed + " " + property);
            }

            if (!changeDescs.isEmpty()) msg.append(" (");
            for (Iterator i = changeDescs.iterator(); i.hasNext();) {
                String desc = (String)i.next();
                msg.append(desc);
                if (i.hasNext()) msg.append(", ");
            }
            if (!changeDescs.isEmpty()) msg.append(")");
        } else {
            action = 'X'; // Shouldn't happen
        }

        String note = event.getNote();
        if (note != null && note.length() > 0) {
            msg.append(" (").append(note).append(")");
        }

        AdminInfo info = getAdminInfo();
        if (info == null) return null;

        return new AdminAuditRecord(level(event), nodeId, entityOid, entityClassname, name, action, msg.toString(), info.login, info.ip);
    }

    private boolean isEmpty(Object o) {
        if (o == null) return true;
        if (o instanceof String) return (((String)o).length() == 0);
        return false;
    }

    public void entityCreated( Created created ) {
        final AuditContext current = getThreadAuditContext();
        current.setCurrentRecord(makeAuditRecord(created));
        current.flush();
    }

    public void entityUpdated( Updated updated ) {
        final AuditContext current = getThreadAuditContext();
        current.setCurrentRecord(makeAuditRecord(updated));
        current.flush();
    }

    public void entityDeleted( Deleted deleted ) {
        final AuditContext current = getThreadAuditContext();
        current.setCurrentRecord(makeAuditRecord(deleted));
        current.flush();
    }

    private AdminInfo getAdminInfo() {
        Subject clientSubject = null;
        String login = null;
        String address = null;
        try {
            //todo: verify that this is equivalent to what it used to be 
            clientSubject = Subject.getSubject(AccessController.getContext());
            address = UnicastRemoteObject.getClientHost();
            if (address != null) {
            } else {
                logger.warning("Could not determine administrator IP address. Will use " + LOCALHOST_IP);
                address = LOCALHOST_IP;
            }
        } catch ( ServerNotActiveException e ) {
            return null;
        }
        if (clientSubject != null) {
            Set principals = clientSubject.getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                Principal p = (Principal)principals.iterator().next();
                if (p instanceof User) login = ((User)p).getLogin();
                if (login == null) login = p.getName();
            }
        }

        if (login != null) {
            AdminInfo info = new AdminInfo();
            info.login = login;
            if (address != null) info.ip = address;
            return info;
        }

        logger.warning("Unable to determine current administrator login");
        return null;
    }

    private static class AdminInfo {
        private String login;
        private String ip;
    }

    private Map levelMappings = new HashMap();
    private static final Logger logger = Logger.getLogger(AdminAuditListener.class.getName());
    public static final String LOCALHOST_IP = "127.0.0.1";
}
