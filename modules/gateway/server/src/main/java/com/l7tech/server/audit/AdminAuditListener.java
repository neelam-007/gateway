/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.HasAuditDetails;
import com.l7tech.server.event.admin.*;
import com.l7tech.server.event.system.BackupEvent;
import com.l7tech.server.service.ServiceEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ApplicationObjectSupport;

import java.rmi.server.ServerNotActiveException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class AdminAuditListener extends ApplicationObjectSupport implements ApplicationListener {
    private final String nodeId;
    private static final String SERVICE_DISABLED = "disabled";
    private final AuditContextFactory auditContextFactory;

    public AdminAuditListener(String nodeId, AuditContextFactory auditContextFactory) {
        this.nodeId = nodeId;
        this.auditContextFactory = auditContextFactory;

        Map<Class<? extends PersistenceEvent>, Level> levels = new HashMap<Class<? extends PersistenceEvent>, Level>();
        levels.put(Deleted.class, Level.WARNING);
        levels.put(ServiceEvent.Disabled.class, Level.WARNING);
        LevelMapping lm = new LevelMapping(PublishedService.class, levels);
        levelMappings.put(PublishedService.class, lm);

        levels = new HashMap<Class<? extends PersistenceEvent>, Level>();
        levels.put(Deleted.class, Level.WARNING);
        levels.put(Updated.class, Level.WARNING);
        levels.put(Created.class, Level.WARNING);
    }


    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AdminEvent) {
            AdminEvent adminEvent = (AdminEvent) event;
            if (adminEvent.isAuditIgnore())
                return;
        }

        if (event instanceof Updated) {
            Updated updated = (Updated)event;
            if (updated.getEntity() instanceof PublishedService) {
                PublishedService service = (PublishedService) updated.getEntity();
                EntityChangeSet changes = updated.getChangeSet();
                Object o = changes.getOldValue(SERVICE_DISABLED);
                Object n = changes.getNewValue(SERVICE_DISABLED);

                if (Boolean.FALSE.equals(o) && Boolean.TRUE.equals(n)) {
                    event = new ServiceEvent.Disabled(service, changes);
                } else if (Boolean.TRUE.equals(o) && Boolean.FALSE.equals(n)) {
                    event = new ServiceEvent.Enabled(service, changes);
                }
            }
        }

        if (event instanceof AdminEvent || event instanceof LogonEvent) {
            auditContextFactory.emitAuditRecordWithDetails(makeAuditRecord(event), false, event.getSource(), makeAuditDetails(event));
        }
    }

    private static Collection<AuditDetail> makeAuditDetails(ApplicationEvent event) {
        Collection<AuditDetail> ret = null;

        if (event instanceof HasAuditDetails) {
            HasAuditDetails hasAuditDetails = (HasAuditDetails) event;
            ret = hasAuditDetails.getAuditDetails();
        }

        return ret;
    }

    public static class LevelMapping {
        public LevelMapping(Class<? extends Entity> entityClass, Map<Class<? extends PersistenceEvent>, Level> eventClassesToLevels) {
            if (entityClass == null || eventClassesToLevels == null) throw new IllegalArgumentException("Args must not be null");
            if (!Entity.class.isAssignableFrom(entityClass)) throw new IllegalArgumentException(Entity.class.getName() + " is not assignable from " + entityClass.getName());
            for (Class<? extends PersistenceEvent> eventClass : eventClassesToLevels.keySet()) {
                if (!ApplicationEvent.class.isAssignableFrom(eventClass))
                    throw new IllegalArgumentException(ApplicationEvent.class.getName() + " is not assignable from " + eventClass.getName());
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

        private final Class<? extends Entity> entityClass;
        private final Map<Class<? extends PersistenceEvent>, Level> eventClassesToLevels;
    }

    private Level level(ApplicationEvent genericEvent) {
        if (genericEvent instanceof PersistenceEvent) {
            PersistenceEvent event = (PersistenceEvent)genericEvent;
            Entity ent = event.getEntity();
            LevelMapping lm = levelMappings.get(ent.getClass());
            Level level = AdminAuditConstants.DEFAULT_LEVEL;
            if (lm != null) {
                Map map = lm.getEventClassesToLevels();
                Level temp = (Level)map.get(event.getClass());
                if (temp != null) level = temp;
            }
            return level;
        } else if (genericEvent instanceof AuditRevokeAllUserCertificates) {
            return Level.SEVERE;
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
            String entityId = entity.getId();
            String entityClassname = entity.getClass().getName();
            if (entity instanceof NamedEntity) name = ((NamedEntity)entity).getName();

            int ppos = entityClassname.lastIndexOf(".");
            String localClassname = ppos >= 0 ? entityClassname.substring(ppos + 1) : entityClassname;
            StringBuffer msg = new StringBuffer(localClassname);
            msg.append(" #").append(entityId);
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

                List<String> changeDescs = new ArrayList<String>();
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

            AdminInfo info = AdminInfo.find(!AuditContextUtils.isSystem());
            if (info == null) return null;

            Goid goid = PersistentEntity.DEFAULT_GOID;
            try {
                goid = Goid.parseGoid(entityId);
            } catch (IllegalArgumentException iae) {
                logger.log(Level.FINE, "Entity ID was not a Goid");
            }

            return new AdminAuditRecord(level(event), nodeId, goid, entityClassname, name, action, msg.toString(), info.identityProviderOid, info.login, info.id, info.ip);
        } else if (genericEvent instanceof BackupEvent) {
            final BackupEvent event = (BackupEvent)genericEvent;
            final User user = event.getUser();
            return new AdminAuditRecord(event.getLevel(),
                                        nodeId,
                                        null,
                                        "<none>",
                                        "Backup Service",
                                        AdminAuditRecord.ACTION_OTHER,
                                        event.getNote(),
                                        user == null ? null : user.getProviderId(),
                                        user == null ? "<none>" : user.getLogin(),
                                        user == null ? "<none>" : user.getId(),
                                        event.getClientAddr());
        } else if (genericEvent instanceof AdminEvent) {
            AdminEvent event = (AdminEvent)genericEvent;
            AdminInfo info = AdminInfo.find(!AuditContextUtils.isSystem());
            if (info == null) return null;

            return new AdminAuditRecord(level(event), nodeId, null, "<none>", "", AdminAuditRecord.ACTION_OTHER, event.getNote(), info.identityProviderOid, info.login, info.id, info.ip);
        } else if (genericEvent instanceof LogonEvent) {
            LogonEvent le = (LogonEvent)genericEvent;
            User admin = (User)le.getSource();
            String ip = null;
            try {
                ip = RemoteUtils.getClientHost();
            } catch (ServerNotActiveException e) {
                logger.log(Level.WARNING, "cannot get remote ip", e);
            }
            if ( le.getType() == LogonEvent.LOGON ) {
                return new AdminAuditRecord(Level.INFO, nodeId, null, "<none>", "", AdminAuditRecord.ACTION_LOGIN,
                                            "User logged in",
                                            admin.getProviderId(), admin.getLogin(), admin.getId(), ip);
            } else {
                return new AdminAuditRecord(Level.INFO, nodeId, null, "<none>", "", AdminAuditRecord.ACTION_LOGOUT,
                                            "User logged out",
                                            admin.getProviderId(), admin.getLogin(), admin.getId(), ip);
            }
        } else {
            throw new IllegalArgumentException("Can't handle events of type " + genericEvent.getClass().getName());
        }
    }

    private boolean isEmpty(Object o) {
        return o == null || o instanceof String && (((String) o).length() == 0);
    }

    private Map<Class<? extends Entity>, LevelMapping> levelMappings = new HashMap<Class<? extends Entity>, LevelMapping>();
    private static final Logger logger = Logger.getLogger(AdminAuditListener.class.getName());
}
