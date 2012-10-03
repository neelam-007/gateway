package com.l7tech.external.assertions.apiportalintegration.server.upgrade;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Administrative interface for executing API Portal upgrades.
 */
@Transactional(propagation = REQUIRED, rollbackFor = Throwable.class)
@Administrative
public interface UpgradePortalAdmin {
    /**
     * Upgrades service policies. Requires RBAC permissions.
     *
     * @return a list of upgraded services.
     */
    @Secured(types = {EntityType.SERVICE}, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    public List<UpgradedEntity> upgradeServicesTo2_1();

    /**
     * Upgrades keys (stored as generic entities). Requires RBAC permissions.
     *
     * @return a list of upgraded keys.
     */
    @Secured(types = {EntityType.GENERIC}, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    public List<UpgradedEntity> upgradeKeysTo2_1();

    /**
     * Deletes unused portal-related cluster properties. Requires RBAC permissions.
     */
    @Secured(types = {EntityType.CLUSTER_PROPERTY}, stereotype = MethodStereotype.DELETE_MULTI)
    public void deleteUnusedClusterProperties();
}
