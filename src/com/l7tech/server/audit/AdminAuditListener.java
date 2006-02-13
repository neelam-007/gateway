/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.audit;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.*;
import com.l7tech.server.service.ServiceEvent;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ApplicationObjectSupport;

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
public class AdminAuditListener extends ApplicationObjectSupport implements ApplicationListener {
    private final String nodeId;
    private final AuditContext auditContext;
    private static final String SERVICE_DISABLED = "disabled";

    public AdminAuditListener(ClusterInfoManager clusterInfoManager, AuditContext auditContext) {
        this.nodeId = clusterInfoManager.thisNodeId();
        this.auditContext = auditContext;

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


    public static final Level DEFAULT_LEVEL = Level.INFO;

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof Updated) {
            Updated updated = (Updated)event;
            if (updated.getEntity() instanceof PublishedService) {
                EntityChangeSet changes = updated.getChangeSet();
                Object o = changes.getOldValue(SERVICE_DISABLED);
                Object n = changes.getNewValue(SERVICE_DISABLED);

                if (Boolean.FALSE.equals(o) && Boolean.TRUE.equals(n)) {
                    event = new ServiceEvent.Disabled(updated.getEntity(), changes);
                } else {
                    event = new ServiceEvent.Enabled(updated.getEntity(), changes);
                }
            }
        }

        if (event instanceof AdminEvent || event instanceof PersistenceEvent) {
            auditContext.setCurrentRecord(makeAuditRecord((AdminEvent)event));
            auditContext.flush();
        }

        if (event instanceof LogonEvent) {
            auditContext.setCurrentRecord(makeAuditRecord((LogonEvent)event));
            auditContext.flush();
        }
    }

    public static class LevelMapping {
        public LevelMapping(Class entityClass, Map eventClassesToLevels) {
            if (entityClass == null || eventClassesToLevels == null) throw new IllegalArgumentException("Args must not be null");
            if (!Entity.class.isAssignableFrom(entityClass)) throw new IllegalArgumentException(Entity.class.getName() + " is not assignable from " + entityClass.getName());
            for (Iterator i = eventClassesToLevels.keySet().iterator(); i.hasNext();) {
                Class eventClass = (Class)i.next();
                if (!ApplicationEvent.class.isAssignableFrom(eventClass)) throw new IllegalArgumentException(ApplicationEvent.class.getName() + " is not assignable from " + eventClass.getName());
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

    private Level level(ApplicationEvent genericEvent) {
        if (genericEvent instanceof PersistenceEvent) {
            PersistenceEvent event = (PersistenceEvent)genericEvent;
            Entity ent = event.getEntity();
            LevelMapping lm = (LevelMapping)levelMappings.get(ent.getClass());
            Level level = DEFAULT_LEVEL;
            if (lm != null) {
                Map map = lm.getEventClassesToLevels();
                Level temp = (Level)map.get(event.getClass());
                if (temp != null) level = temp;
            }
            return level;
        } else if (genericEvent instanceof AdminEvent) {
            return Level.INFO;
        } else {
            throw new IllegalArgumentException("Can't handle events of type " + genericEvent.getClass().getName());
        }
    }

    private AdminAuditRecord makeAuditRecord(ApplicationEvent genericEvent) {
        if (genericEvent instanceof PersistenceEvent) {
            PersistenceEvent event = (PersistenceEvent)genericEvent;
            final Entity entity = event.getEntity();

            String name = null;
            char action;
            long entityOid = entity.getOid();
            String entityClassname = entity.getClass().getName();
            if (entity instanceof NamedEntity) name = ((NamedEntity)entity).getName();

            int ppos = entityClassname.lastIndexOf(".");
            String localClassname = ppos >= 0 ? entityClassname.substring(ppos + 1) : entityClassname;
            StringBuffer msg = new StringBuffer(localClassname);
            msg.append(" #").append(entityOid);
            if (entity instanceof NamedEntity) {
                msg.append(" (");
                msg.append(((NamedEntity)entity).getName());
                msg.append(")");
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

            return new AdminAuditRecord(level(event), nodeId, entityOid, entityClassname, name, action, msg.toString(), info.identityProviderOid, info.login, info.id, info.ip);
        } else if (genericEvent instanceof AdminEvent) {
            AdminEvent event = (AdminEvent)genericEvent;
            AdminInfo info = getAdminInfo();
            if (info == null) return null;

            return new AdminAuditRecord(level(event), nodeId, 0, "<none>", "", 'D', event.getNote(), info.identityProviderOid, info.login, info.id, info.ip);
        } else if (genericEvent instanceof LogonEvent) {
            LogonEvent le = (LogonEvent)genericEvent;
            User admin = (User)le.getSource();
            String ip = null;
            try {
                ip = UnicastRemoteObject.getClientHost();
            } catch (ServerNotActiveException e) {
                logger.log(Level.WARNING, "cannot get remote ip", e);
            }
            return new AdminAuditRecord(Level.INFO, nodeId, 0, "<none>", "", AdminAuditRecord.ACTION_LOGIN,
                                        "Administrator logged in", admin.getProviderId(), admin.getLogin(), admin.getUniqueIdentifier(), ip);
        } else {
            throw new IllegalArgumentException("Can't handle events of type " + genericEvent.getClass().getName());
        }
    }

    private boolean isEmpty(Object o) {
        if (o == null) return true;
        if (o instanceof String) return (((String)o).length() == 0);
        return false;
    }

    private AdminInfo getAdminInfo() {
        Subject clientSubject = null;
        String login = null;
        String uniqueId = null;
        String address = null;
        long providerOid = IdentityProviderConfig.DEFAULT_OID;
        try {
            address = UnicastRemoteObject.getClientHost();
            clientSubject = Subject.getSubject(AccessController.getContext());
        } catch (ServerNotActiveException e) {
            logger.warning("The administrative event caused as local call, outside of servicing an adminstrative remote call." +
              "Will use ip/user" + LOCALHOST_IP + "/" + LOCALHOST_SUBJECT);
            address = LOCALHOST_IP;
            login = LOCALHOST_SUBJECT;
        }
        if (clientSubject != null) {
            Set principals = clientSubject.getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                Principal p = (Principal)principals.iterator().next();
                if (p instanceof User) {
                    User u = (User) p;
                    login = u.getLogin();
                    uniqueId = u.getUniqueIdentifier();
                    providerOid = u.getProviderId();
                }
                if (login == null) login = p.getName();
                if (uniqueId == null) uniqueId = "principal:"+login;
            }
        }

        //return new AdminInfo(...);
        return new AdminInfo(login, uniqueId, providerOid, address);
    }

    private static class AdminInfo {
        public AdminInfo(String login, String id, long ipOid, String ip) {
            this.ip = ip;
            this.id = id;
            this.identityProviderOid = ipOid;
            this.login = login;
        }

        private final String login;
        private final String id;
        private final long identityProviderOid;
        private final String ip;
    }

    private Map levelMappings = new HashMap();
    private static final Logger logger = Logger.getLogger(AdminAuditListener.class.getName());
    public static final String LOCALHOST_IP = "127.0.0.1";
    public static final String LOCALHOST_SUBJECT = "localsystem";
}
