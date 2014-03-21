package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyVersionMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This was created: 11/26/13 as 4:49 PM
 *
 * @author Victor Kazakov
 */
@Component
public class PolicyVersionRestResourceFactory {

    @Inject
    private PolicyVersionManager policyVersionManager;

    @Inject
    private RbacAccessService rbacAccessService;

    public List<PolicyVersionMO> listResources(@NotNull String policyId, @NotNull Integer offset, @NotNull Integer count, @Nullable String sort, @Nullable Boolean order, @Nullable Map<String, List<Object>> filters) {
        try {
            HashMap<String, List<Object>> filtersWithPolicyGoid = new HashMap<>(filters);
            filtersWithPolicyGoid.put("policyGoid", Arrays.<Object>asList(RestResourceFactoryUtils.goidConvert.call(policyId)));
            List<PolicyVersion> policyVersions = policyVersionManager.findPagedMatching(offset, count, sort, order, filtersWithPolicyGoid);
            policyVersions = rbacAccessService.accessFilter(policyVersions, EntityType.POLICY_VERSION, OperationType.READ, null);

            return Functions.map(policyVersions, new Functions.Unary<PolicyVersionMO, PolicyVersion>() {
                @Override
                public PolicyVersionMO call(PolicyVersion policyVersion) {
                    PolicyVersionMO policyVersionMO = ManagedObjectFactory.createPolicyVersionMO();
                    policyVersionMO.setId(policyVersion.getId());
                    policyVersionMO.setOrdinal(policyVersion.getOrdinal());
                    return policyVersionMO;
                }
            });
        } catch (FindException e) {
            throw new ResourceFactory.ResourceAccessException(e.getMessage(), e);
        }
    }

    public PolicyVersionMO getResource(@NotNull String policyId, @NotNull String id) throws FindException {
        PolicyVersion policyVersion = policyVersionManager.findPolicyVersionForPolicy(Goid.parseGoid(policyId), Long.parseLong(id));
        rbacAccessService.validatePermitted(policyVersion, OperationType.READ);
        return buildMO(policyVersion);
    }

    public PolicyVersionMO getActiveVersion(String policyId) throws FindException {
        PolicyVersion policyVersion = policyVersionManager.findActiveVersionForPolicy(Goid.parseGoid(policyId));
        rbacAccessService.validatePermitted(policyVersion, OperationType.READ);
        return buildMO(policyVersion);
    }

    public void updateComment(@NotNull String policyId, @NotNull String id, @Nullable String comment) throws FindException, UpdateException {
        PolicyVersion policyVersion = policyVersionManager.findPolicyVersionForPolicy(Goid.parseGoid(policyId), Long.parseLong(id));
        rbacAccessService.validatePermitted(policyVersion, OperationType.UPDATE);
        policyVersion.setName(comment);
        policyVersionManager.update(policyVersion);
    }

    public void updateActiveComment(String policyId, String comment) throws FindException, UpdateException {
        PolicyVersion policyVersion = policyVersionManager.findActiveVersionForPolicy(Goid.parseGoid(policyId));
        rbacAccessService.validatePermitted(policyVersion, OperationType.UPDATE);
        policyVersion.setName(comment);
        policyVersionManager.update(policyVersion);
    }

    /**
     * Builds a policy version MO
     *
     * @param policyVersion The plicy version to build the MO from
     * @return The policy Version MO
     */
    private PolicyVersionMO buildMO(PolicyVersion policyVersion) {
        PolicyVersionMO policyVersionMO = ManagedObjectFactory.createPolicyVersionMO();
        policyVersionMO.setActive(policyVersion.isActive());
        policyVersionMO.setComment(policyVersion.getName());
        policyVersionMO.setId(policyVersion.getId());
        policyVersionMO.setPolicyId(policyVersion.getPolicyGoid().toString());
        policyVersionMO.setTime(policyVersion.getTime());
        policyVersionMO.setOrdinal(policyVersion.getOrdinal());
        policyVersionMO.setXml(policyVersion.getXml());
        return policyVersionMO;
    }
}
