package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyVersionMO;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
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

    private Map<String, String> sortKeys = CollectionUtils.MapBuilder.<String, String>builder()
            .put("id", "id")
            .put("version", "ordinal").map();
    private Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> filters = CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
            .put("version", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("ordinal", RestResourceFactoryUtils.longConvert))
            .put("active", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("active", RestResourceFactoryUtils.booleanConvert))
            .put("comment", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
            .map();

    public String getSortKey(String sort) {
        return sortKeys.get(sort);
    }

    public Map<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>> getFiltersInfo() {
        return filters;
    }

    public List<PolicyVersionMO> listResources(@NotNull String policyId, @NotNull Integer offset, @NotNull Integer count, @Nullable String sort, @Nullable Boolean order, @Nullable Map<String, List<Object>> filters) {
        try {
            HashMap<String, List<Object>> filtersWithPolicyGoid = new HashMap<>(filters);
            filtersWithPolicyGoid.put("policyGoid", Arrays.<Object>asList(RestResourceFactoryUtils.goidConvert.call(policyId)));
            List<PolicyVersion> policyVersions = policyVersionManager.findPagedMatching(offset, count, sort, order, filtersWithPolicyGoid);
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
        return buildMO(policyVersionManager.findPolicyVersionForPolicy(Goid.parseGoid(policyId),Long.parseLong(id)));
    }

    public PolicyVersionMO getActiveVersion(String policyId) throws FindException {
        return buildMO(policyVersionManager.findActiveVersionForPolicy(Goid.parseGoid(policyId)));
    }

    public void updateComment(@NotNull String policyId, @NotNull String id, @Nullable String comment) throws FindException, UpdateException {
        PolicyVersion policyVersion = policyVersionManager.findPolicyVersionForPolicy(Goid.parseGoid(policyId), Long.parseLong(id));
        policyVersion.setName(comment);
        policyVersionManager.update(policyVersion);
    }

    public void updateActiveComment(String policyId, String comment) throws FindException, UpdateException {
        PolicyVersion policyVersion = policyVersionManager.findActiveVersionForPolicy(Goid.parseGoid(policyId));
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
