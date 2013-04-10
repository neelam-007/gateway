package com.l7tech.server;

import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsBinDetail;
import com.l7tech.gateway.common.transport.email.EmailListenerState;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.identity.GroupMembership;
import com.l7tech.identity.cert.CertEntryRow;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.*;
import com.l7tech.server.event.admin.*;
import com.l7tech.identity.LogonInfo;
import com.l7tech.server.wsdm.subscription.Subscription;
import com.l7tech.server.uddi.UDDIRegistrySubscription;
import com.l7tech.server.uddi.UDDIBusinessServiceStatus;
import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.collection.PersistentCollection;
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
 */
public class PersistenceEventInterceptor extends ApplicationObjectSupport implements Interceptor {
    private static Logger logger = Logger.getLogger(PersistenceEventInterceptor.class.getName());

    public PersistenceEventInterceptor() {
        // High traffic entities that should neither generate application events nor be audited
        ignoredClassNames = new HashSet<String>();
        ignoredClassNames.add(AdminAuditRecord.class.getName());
        ignoredClassNames.add(SystemAuditRecord.class.getName());
        ignoredClassNames.add(MessageSummaryAuditRecord.class.getName());
        ignoredClassNames.add(AuditDetail.class.getName());
        ignoredClassNames.add(MetricsBin.class.getName());
        ignoredClassNames.add(MetricsBinDetail.class.getName());
        ignoredClassNames.add(ServiceUsage.class.getName());
        ignoredClassNames.add(Subscription.class.getName());
        ignoredClassNames.add(MessageContextMappingKeys.class.getName());
        ignoredClassNames.add(MessageContextMappingValues.class.getName());
        ignoredClassNames.add(LogonInfo.class.getName());
        ignoredClassNames.add(EmailListenerState.class.getName());
        ignoredClassNames.add(RoleAssignment.class.getName());
        ignoredClassNames.add("com.l7tech.server.ems.standardreports.StandardReportArtifact");
        ignoredClassNames.add("com.l7tech.server.ems.migration.MigrationMappingRecord");
        ignoredClassNames.add(UDDIRegistrySubscription.class.getName());
        ignoredClassNames.add(UDDIBusinessServiceStatus.class.getName());

        // System entities that should generate application events but should not be audited
        noAuditClassNames = new HashSet<String>();
        noAuditClassNames.add(ClusterNodeInfo.class.getName());
        noAuditClassNames.add(Permission.class.getName());
        noAuditClassNames.add(ScopePredicate.class.getName());
        noAuditClassNames.add(ObjectIdentityPredicate.class.getName());
        noAuditClassNames.add(AttributePredicate.class.getName());
        noAuditClassNames.add(FolderPredicate.class.getName());
        noAuditClassNames.add(EntityFolderAncestryPredicate.class.getName());
        noAuditClassNames.add(SecurityZonePredicate.class.getName());
        noAuditClassNames.add(UDDIProxiedService.class.getName());
        noAuditClassNames.add("com.l7tech.server.ems.monitoring.SystemMonitoringNotificationRule");
        noAuditClassNames.add("com.l7tech.server.ems.monitoring.SsgClusterNotificationSetup");
        noAuditClassNames.add("com.l7tech.server.ems.monitoring.EntityMonitoringPropertySetup");
    }

    private final Set<String> ignoredClassNames; // don't fire an event at all
    private final Set<String> noAuditClassNames; // fire an event, but mark it "system" so it doesn't get audited

    private boolean ignored(Object entity) {
        return !(entity instanceof PersistentEntity) || ignoredClassNames.contains(entity.getClass().getName());
    }

    public Set<String> getIgnoredClassNames() {
        return ignoredClassNames;
    }

    public Set<String> getNoAuditClassNames() {
        return noAuditClassNames;
    }

    private AdminEvent setsys(Object entity, AdminEvent event) {
        if (AuditContextUtils.isSystem() || noAuditClassNames.contains(entity.getClass().getName())) {
            event.setAuditIgnore(true);
        }
        return event;
    }

    /**
     * Detects saves and fires a {@link com.l7tech.server.event.admin.Created} event if the entity isn't {@link #ignored} and the save is committed
     */
    @Override
    public boolean onSave(final Object entity, final Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        if (!ignored(entity)) {
            logger.log(Level.FINE, "Created " + entity.getClass().getName() + " " + id);
            getApplicationContext().publishEvent(setsys(entity, createdEvent(entity)));
        }
        return false;
    }

    /**
     * Detects deletes and fires a {@link Deleted} event if the entity isn't {@link #ignored} and the deletion is committed
     */
    @Override
    public void onDelete(final Object entity, final Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        if (!ignored(entity)) {
            logger.log(Level.FINE, "Deleted " + entity.getClass().getName() + " " + id);
            getApplicationContext().publishEvent(setsys(entity, deletedEvent(entity)));
        }
    }

    /**
     * Detects updates and fires an {@link com.l7tech.server.event.admin.Updated} event if the entity isn't {@link #ignored} and the update is committed
     */
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
        if (!ignored(entity)) {
            logger.log(Level.FINE, "Updated " + entity.getClass().getName() + " " + id);
            EntityChangeSet changes = new EntityChangeSet(propertyNames, previousState, currentState);
            getApplicationContext().publishEvent(setsys(entity, updatedEvent(entity, changes)));
        }
        return false;
    }

    /**
     * Detects creation or rereation of a PersistentCollection and fires an {@link Updated}
     * event for the entity owning the collection
     * if the entity isn't {@link #ignored} and the change is committed.
     */
    @Override
    public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
        onCollectionChangedInAnyWay(collection);
    }

    /**
     * Detects deletion of a PersistentCollection and fires an {@link Updated}
     * event for the entity owning the collection
     * if the entity isn't {@link #ignored} and the change is committed.
     */
    @Override
    public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
        onCollectionChangedInAnyWay(collection);
    }

    /**
     * Detects changes to the contents of a PersistentCollection and fires an {@link Updated}
     * event for the entity owning the collection
     * if the entity isn't {@link #ignored} and the change is committed.
     */
    @Override
    public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
        onCollectionChangedInAnyWay(collection);
    }

    private void onCollectionChangedInAnyWay(Object collection) {
        if (collection instanceof PersistentCollection) {
            PersistentCollection pc = (PersistentCollection)collection;
            Object owner = pc.getOwner();
            if (owner instanceof Entity) {
                Entity entity = (Entity) owner;
                if (!ignored(entity)) {
                    logger.log(Level.FINE, "Updated collection in " + entity.getClass().getName() + " " + entity.getId());
                    final String[] propertyNames = new String[] { pc.getRole() };
                    final Object[] oldValues = new String[] { "[collection]" };
                    final Object[] newValues = new String[] { "[collection]" };
                    EntityChangeSet changes = new EntityChangeSet(propertyNames, oldValues, newValues);
                    final AdminEvent event = updatedEvent(entity, changes);
                    event.setAuditIgnore(true); // TODO figure out why auditing one of these events triggers org.hibernate.AssertionFailure
                    getApplicationContext().publishEvent(setsys(entity, event));
                }
            }
        }
    }

    private AdminEvent deletedEvent(Object obj) {
        if (obj instanceof GroupMembership) {
            return new GroupMembershipEvent(new GroupMembershipEventInfo((GroupMembership)obj, "removed", getApplicationContext()));
        } else if (obj instanceof CertEntryRow) {
            return new UserCertEvent(new UserCertEventInfo((CertEntryRow)obj, "removed", null, getApplicationContext()));
        } else if (obj instanceof Entity) {
            return new Deleted<Entity>((Entity)obj);
        } else
            throw new IllegalStateException("Can't make a Deleted event for a " + obj.getClass().getName());
    }

    private AdminEvent createdEvent(Object obj) {
        if (obj instanceof CertEntryRow) {
            return new UserCertEvent(new UserCertEventInfo((CertEntryRow)obj, "added", null, getApplicationContext()));
        } else if (obj instanceof GroupMembership) {
            return new GroupMembershipEvent(new GroupMembershipEventInfo((GroupMembership)obj, "added", getApplicationContext()));
        } else if (obj instanceof PolicyVersion) {
            return new PolicyVersionCreated((PolicyVersion)obj);
        } else if (obj instanceof Entity) {
            return new Created<Entity>((Entity)obj);
        } else
            throw new IllegalStateException("Can't make a Created event for a " + obj.getClass().getName());
    }

    private AdminEvent updatedEvent(Object obj, EntityChangeSet changes) {
        if (obj instanceof CertEntryRow && !AuditContextUtils.isSystem()) {
            return new UserCertEvent(new UserCertEventInfo((CertEntryRow)obj, "updated", changes, getApplicationContext()));
        } else if (obj instanceof GroupMembership) {
            return new GroupMembershipEvent(new GroupMembershipEventInfo((GroupMembership)obj, "updated", getApplicationContext()));
        } else if (obj instanceof PolicyVersion ) {
            return new PolicyVersionUpdated((PolicyVersion)obj, changes);
        } else if (obj instanceof Entity) {
            return new Updated<Entity>((Entity)obj, changes);
        } else
            throw new IllegalStateException("Can't make an Updated event for a " + obj.getClass().getName());
    }

    /** Ignored */
    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        return false;
    }

    /** Ignored */
    @Override
    public void preFlush(Iterator entities) throws CallbackException {
    }

    /** Ignored */
    @Override
    public void postFlush(Iterator entities) throws CallbackException {
    }

    /** Ignored */
    @Override
    public Boolean isTransient(Object entity) {
        return null;
    }

    /** Ignored */
    @Override
    public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        return null;
    }

    /** Ignored */
    @Override
    public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
        return null;
    }

    /** Ignored */
    @Override
    public String getEntityName(Object object) throws CallbackException {
        return null;
    }

    /** Ignored */
    @Override
    public Object getEntity(String entityName, Serializable id) throws CallbackException {
        return null;
    }

    /** Ignored */
    @Override
    public void afterTransactionBegin(Transaction tx) {
    }

    /** Ignored */
    @Override
    public void beforeTransactionCompletion(Transaction tx) {
    }

    /** Ignored */
    @Override
    public void afterTransactionCompletion(Transaction tx) {
    }

    /** Ignored */
    @Override
    public String onPrepareStatement(String sql) {
        return sql;
    }
}