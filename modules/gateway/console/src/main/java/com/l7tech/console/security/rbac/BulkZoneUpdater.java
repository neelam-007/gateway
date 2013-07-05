package com.l7tech.console.security.rbac;

import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs bulk Security Zone updates for entities by calling admin interfaces.
 */
public class BulkZoneUpdater {
    private static final Logger logger = Logger.getLogger(BulkZoneUpdater.class.getName());
    private final RbacAdmin rbacAdmin;
    private final UDDIRegistryAdmin uddiAdmin;
    private final JmsAdmin jmsAdmin;
    private final PolicyAdmin policyAdmin;

    public BulkZoneUpdater(@NotNull final RbacAdmin rbacAdmin, @NotNull final UDDIRegistryAdmin uddiAdmin, @NotNull final JmsAdmin jmsAdmin, @NotNull final PolicyAdmin policyAdmin) {
        this.rbacAdmin = rbacAdmin;
        this.uddiAdmin = uddiAdmin;
        this.jmsAdmin = jmsAdmin;
        this.policyAdmin = policyAdmin;
    }

    /**
     * Performs bulk SecurityZone updates for the given entities in one admin call.
     * If the given entities have other entities which should share the same security zone, these 'zone dependent' entities will also have their zone updated to the given zone.
     *
     * @param securityZoneOid the oid of the SecurityZone to set on the entities.
     * @param entityType      the EntityType of the entities to update.
     * @param entities        a collection of EntityHeader which represent entities to update.
     * @throws FindException   if an error occurs looking up 'zone dependencies'
     * @throws UpdateException if an error occurs updating the SecurityZones on the entities.
     */
    public void bulkUpdate(@Nullable final Long securityZoneOid, @NotNull final EntityType entityType, @NotNull final Collection<EntityHeader> entities) throws FindException, UpdateException {
        if (!entities.isEmpty()) {
            final Map<EntityType, Collection<Serializable>> entitiesToUpdate = collectEntitiesToUpdate(entityType, entities);
            rbacAdmin.setSecurityZoneForEntities(securityZoneOid, entitiesToUpdate);
        }
    }

    /**
     * Collect a map of all entities that require a Security Zone update where key = entity type and value = ids of the entities to update.
     * The Ids are either long oids or Goids
     * <p/>
     * Can contain more entities than the given selected entities if the selected entities have 'dependencies'.
     *
     * @param entityType the user selected entity type
     * @param entities   the user selected entities of the selected entity type
     * @return a map of all entities that require a Security Zone update where key = entity type and value = ids of the entities to update
     * @throws com.l7tech.objectmodel.FindException
     *          if there was an error retrieving the dependencies of the given selected entities.
     */
    private Map<EntityType, Collection<Serializable>> collectEntitiesToUpdate(final EntityType entityType, final Collection<EntityHeader> entities) throws FindException {
        final Map<EntityType, Collection<Serializable>> entitiesToUpdate = new HashMap<>();
        final List<Serializable> selectedIds = new ArrayList<>(entities.size());
        for (final EntityHeader selectedHeader : entities) {
            if(GoidEntity.class.isAssignableFrom(selectedHeader.getType().getEntityClass()))
                selectedIds.add(selectedHeader.getGoid());
            else if(PersistentEntity.class.isAssignableFrom(selectedHeader.getType().getEntityClass())){
                selectedIds.add(selectedHeader.getOid());
            } else {
                selectedIds.add(selectedHeader.getStrId());
            }
        }
        logger.log(Level.FINE, selectedIds.size() + " " + entityType.getPluralName() + " require update.");
        entitiesToUpdate.put(entityType, selectedIds);
        if (SecurityZoneUtil.getEntityTypesWithInheritedZones().keySet().contains(entityType)) {
            // selected entity type is a 'parent' type
            // we need to also update the entities that inherit the zone from this entity type
            switch (entityType) {
                case SERVICE:
                    final List<Serializable> proxiedServiceInfoOids = new ArrayList<>();
                    final List<Serializable> serviceControlOids = new ArrayList<>();
                    final List<Serializable> policyOids = new ArrayList<>();
                    for (final EntityHeader service : entities) {
                        final UDDIServiceControl uddiServiceControl = uddiAdmin.getUDDIServiceControl(service.getOid());
                        if (uddiServiceControl != null) {
                            serviceControlOids.add(uddiServiceControl.getOid());
                        }
                        final UDDIProxiedServiceInfo proxiedServiceInfo = uddiAdmin.findProxiedServiceInfoForPublishedService(service.getOid());
                        if (proxiedServiceInfo != null) {
                            proxiedServiceInfoOids.add(proxiedServiceInfo.getOid());
                        }
                        if (service instanceof ServiceHeader) {
                            final Long policyOid = ((ServiceHeader) service).getPolicyOid();
                            if (policyOid != null) {
                                final Policy policy = policyAdmin.findPolicyByPrimaryKey(((ServiceHeader) service).getPolicyOid());
                                if (policy != null) {
                                    policyOids.add(policy.getOid());
                                }
                            }
                        }
                    }
                    entitiesToUpdate.put(EntityType.UDDI_PROXIED_SERVICE_INFO, proxiedServiceInfoOids);
                    logger.log(Level.FINE, proxiedServiceInfoOids.size() + " " + EntityType.UDDI_PROXIED_SERVICE_INFO.getPluralName() + " require update.");
                    entitiesToUpdate.put(EntityType.UDDI_SERVICE_CONTROL, serviceControlOids);
                    logger.log(Level.FINE, serviceControlOids.size() + " " + EntityType.UDDI_SERVICE_CONTROL.getPluralName() + " require update.");
                    entitiesToUpdate.put(EntityType.POLICY, policyOids);
                    logger.log(Level.FINE, policyOids.size() + " " + EntityType.POLICY.getPluralName() + " require update.");
                    break;
                case JMS_CONNECTION:
                    final List<Serializable> endpointOids = new ArrayList<>();
                    for (final EntityHeader jmsConnection : entities) {
                        final JmsEndpoint[] found = jmsAdmin.getEndpointsForConnection(jmsConnection.getOid());
                        if (found != null) {
                            for (int i = 0; i < found.length; i++) {
                                endpointOids.add(found[i].getOid());
                            }
                        }
                    }
                    entitiesToUpdate.put(EntityType.JMS_ENDPOINT, endpointOids);
                    logger.log(Level.FINE, endpointOids.size() + " " + EntityType.JMS_ENDPOINT.getPluralName() + " require update.");
                    break;
                default:
                    throw new IllegalArgumentException("Handling of parent entity type " + entityType + " is not supported.");
            }
        }
        return entitiesToUpdate;
    }
}
