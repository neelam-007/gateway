package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyVersionTransformer;
import com.l7tech.gateway.api.PolicyVersionMO;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Inject;
import java.util.*;

/**
 * The policy version rest resource factory.
 */
@Component
public class PolicyVersionRestResourceFactory {

    @Inject
    private PlatformTransactionManager transactionManager;
    @Inject
    private PolicyVersionManager policyVersionManager;
    @Inject
    private PolicyAPIResourceFactory policyAPIResourceFactory;
    @Inject
    private ServiceAPIResourceFactory serviceAPIResourceFactory;
    @Inject
    private RbacAccessService rbacAccessService;
    @Inject
    private PolicyVersionTransformer transformer;

    private static final Collection<PolicyType> availablePolicyTypes = Collections.unmodifiableCollection(Arrays.asList(PolicyType.INCLUDE_FRAGMENT, PolicyType.INTERNAL, PolicyType.GLOBAL_FRAGMENT));
    private static final Map<String, String> propertyNamesMap = CollectionUtils.MapBuilder.<String, String>builder().put("name", "comment").map();

    public List<PolicyVersionMO> listPolicyVersions(@NotNull final Either<String, String> serviceOrPolicyId,
                                                    @Nullable final String sort,
                                                    @Nullable final Boolean order,
                                                    @Nullable final Map<String, List<Object>> filters) throws ResourceFactory.ResourceNotFoundException {
        return RestResourceFactoryUtils.transactional(transactionManager, true, new Functions.NullaryThrows<List<PolicyVersionMO>, ResourceFactory.ResourceNotFoundException>() {
            @Override
            public List<PolicyVersionMO> call() throws ResourceFactory.ResourceNotFoundException {
                try {
                    final String policyId = validateAccessAndGetPolicyId(serviceOrPolicyId, OperationType.READ);
                    final HashMap<String, List<Object>> filtersWithPolicyGoid = new HashMap<>(filters);
                    //add the policy Goid filter so that only policy versions from the given policy are listed.
                    filtersWithPolicyGoid.put("policyGoid", Arrays.<Object>asList(Goid.parseGoid(policyId)));
                    List<PolicyVersion> policyVersions = policyVersionManager.findPagedMatching(0, -1, sort, order, filtersWithPolicyGoid);
                    //filter the policy versions.
                    policyVersions = rbacAccessService.accessFilter(policyVersions, EntityType.POLICY_VERSION, OperationType.READ, null);
                    return Functions.map(policyVersions, new Functions.Unary<PolicyVersionMO, PolicyVersion>() {
                        @Override
                        public PolicyVersionMO call(PolicyVersion policyVersion) {
                            return transformer.convertToMO(policyVersion);
                        }
                    });
                } catch (FindException e) {
                    throw new ResourceFactory.ResourceNotFoundException(e.getMessage(), e);
                }
            }
        });
    }

    public PolicyVersionMO getPolicyVersion(@NotNull final Either<String, String> serviceOrPolicyId, @NotNull final Long versionOrdinal) throws ResourceFactory.ResourceNotFoundException {
        return RestResourceFactoryUtils.transactional(transactionManager, true, new Functions.NullaryThrows<PolicyVersionMO, ResourceFactory.ResourceNotFoundException>() {
            @Override
            public PolicyVersionMO call() throws ResourceFactory.ResourceNotFoundException {
                final String policyId = validateAccessAndGetPolicyId(serviceOrPolicyId, OperationType.READ);
                final PolicyVersion policyVersion = getPolicyVersionInternal(policyId, versionOrdinal);
                //check if the user has access to this policy version
                rbacAccessService.validatePermitted(policyVersion, OperationType.READ);
                //transform and return the policy version
                return transformer.convertToMO(policyVersion);
            }
        });
    }

    public PolicyVersionMO getActiveVersion(@NotNull final Either<String, String> serviceOrPolicyId) throws ResourceFactory.ResourceNotFoundException {
        return RestResourceFactoryUtils.transactional(transactionManager, true, new Functions.NullaryThrows<PolicyVersionMO, ResourceFactory.ResourceNotFoundException>() {
            @Override
            public PolicyVersionMO call() throws ResourceFactory.ResourceNotFoundException {
                final String policyId = validateAccessAndGetPolicyId(serviceOrPolicyId, OperationType.READ);
                final PolicyVersion policyVersion = getActivePolicyVersion(policyId);
                //check if the user has access to this policy version
                rbacAccessService.validatePermitted(policyVersion, OperationType.READ);
                //transform and return the policy version
                return transformer.convertToMO(policyVersion);
            }
        });
    }

    public void updateComment(@NotNull final Either<String, String> serviceOrPolicyId, @NotNull final Long versionOrdinal, @Nullable final String comment) throws ResourceFactory.ResourceNotFoundException {
        RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryVoidThrows<ResourceFactory.ResourceNotFoundException>() {
            @Override
            public void call() throws ResourceFactory.ResourceNotFoundException {
                final String policyId = validateAccessAndGetPolicyId(serviceOrPolicyId, OperationType.READ);
                final PolicyVersion policyVersion = getPolicyVersionInternal(policyId, versionOrdinal);
                //check if the user has access to this policy version
                rbacAccessService.validatePermitted(policyVersion, OperationType.UPDATE);
                //Updates the policy version comment
                policyVersion.setName(comment);
                //validate the policy version, this should thrown an exception if the comment is too long.
                RestResourceFactoryUtils.validate(policyVersion, propertyNamesMap);
                try {
                    policyVersionManager.update(policyVersion);
                } catch (UpdateException e) {
                    throw new ResourceFactory.ResourceAccessException("Could not update policy version '" + versionOrdinal + "' for policy " + policyId, e);
                }
            }
        });
    }

    public void updateActiveComment(@NotNull final Either<String, String> serviceOrPolicyId, @Nullable final String comment) throws ResourceFactory.ResourceNotFoundException {
        RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryVoidThrows<ResourceFactory.ResourceNotFoundException>() {
            @Override
            public void call() throws ResourceFactory.ResourceNotFoundException {
                final String policyId = validateAccessAndGetPolicyId(serviceOrPolicyId, OperationType.UPDATE);
                final PolicyVersion policyVersion = getActivePolicyVersion(policyId);
                //check if the user has access to this policy version
                rbacAccessService.validatePermitted(policyVersion, OperationType.UPDATE);
                //Updates the policy version comment
                policyVersion.setName(comment);
                //validate the policy version, this should thrown an exception if the comment is too long.
                RestResourceFactoryUtils.validate(policyVersion, propertyNamesMap);
                try {
                    policyVersionManager.update(policyVersion);
                } catch (UpdateException e) {
                    throw new ResourceFactory.ResourceAccessException("Could not update active policy version comment for policy " + policyId, e);
                }
            }
        });
    }

    public void activate(@NotNull final Either<String, String> serviceOrPolicyId, @NotNull final Long versionOrdinal) throws ResourceFactory.ResourceNotFoundException {
        RestResourceFactoryUtils.transactional(transactionManager, false, new Functions.NullaryVoidThrows<ResourceFactory.ResourceNotFoundException>() {
            @Override
            public void call() throws ResourceFactory.ResourceNotFoundException {
                final String policyId = validateAccessAndGetPolicyId(serviceOrPolicyId, OperationType.UPDATE);

                final PolicyVersion policyVersion = getPolicyVersionInternal(policyId, versionOrdinal);
                //check if the user has access to this policy version
                rbacAccessService.validatePermitted(policyVersion, OperationType.UPDATE);
                try {
                    //deactivate other policy version
                    policyVersionManager.deactivateVersions(Goid.parseGoid(policyId), policyVersion.getGoid());
                    //set the selected policy version as active
                    policyVersion.setActive(true);
                    //update the policy version.
                    policyVersionManager.update(policyVersion);
                } catch (UpdateException e) {
                    throw new ResourceFactory.ResourceAccessException("Could not activate policy version '" + versionOrdinal + "' for policy " + policyId, e);
                }
            }
        });
    }

    /**
     * This will check that the service or policy exists and that the currently authenticated user has the required
     * access to the service or policy.
     *
     * @param serviceOrPolicyId The service or policy id. Left is the service Id, right is the policy id.
     * @param operationType     The operation type required on the service or policy
     * @return The policy id that the policy versions will reference
     */
    @NotNull
    private String validateAccessAndGetPolicyId(@NotNull final Either<String, String> serviceOrPolicyId, @NotNull final OperationType operationType) throws ResourceFactory.ResourceNotFoundException {
        if (serviceOrPolicyId.isLeft()) {
            //this is a service ID
            //validate service exists and that the user has access to the service
            serviceAPIResourceFactory.validateExistsAndHasAccess(serviceOrPolicyId.left(), operationType);
            return serviceAPIResourceFactory.getPolicyIdForService(serviceOrPolicyId.left());
        } else {
            //This is a policy ID
            //validate policy exists and that the user has access to the policy
            policyAPIResourceFactory.validateExistsAndHasAccess(serviceOrPolicyId.right(), operationType, availablePolicyTypes);
            return serviceOrPolicyId.right();
        }
    }

    /**
     * Returns a policy version for the given policy and version ordinal.
     *
     * @param policyId       The policyID to find the version for
     * @param versionOrdinal The version number
     * @return The policy version.
     * @throws ResourceFactory.ResourceNotFoundException This is thrown if no such version can be found.
     */
    private PolicyVersion getPolicyVersionInternal(@NotNull final String policyId, final long versionOrdinal) throws ResourceFactory.ResourceNotFoundException {
        final PolicyVersion policyVersion;
        try {
            policyVersion = policyVersionManager.findPolicyVersionForPolicy(Goid.parseGoid(policyId), versionOrdinal);
        } catch (FindException e) {
            throw new ResourceFactory.ResourceNotFoundException("Could not find policy version '" + versionOrdinal + "' for policy " + policyId, e);
        }
        if (policyVersion == null) {
            throw new ResourceFactory.ResourceNotFoundException("Could not find policy version '" + versionOrdinal + "' for policy " + policyId);
        }
        return policyVersion;
    }

    /**
     * Returns the active policy version for the given policy
     *
     * @param policyId The policyID to find the active version for
     * @return The active policy version
     * @throws ResourceFactory.ResourceNotFoundException This is thrown if the active policy version cannot be found
     */
    private PolicyVersion getActivePolicyVersion(@NotNull final String policyId) throws ResourceFactory.ResourceNotFoundException {
        final PolicyVersion policyVersion;
        try {
            policyVersion = policyVersionManager.findActiveVersionForPolicy(Goid.parseGoid(policyId));
        } catch (FindException e) {
            throw new ResourceFactory.ResourceNotFoundException("Could not find active policy version for policy " + policyId, e);
        }
        if (policyVersion == null) {
            throw new ResourceFactory.ResourceNotFoundException("Could not find active policy version for policy " + policyId);
        }
        return policyVersion;
    }
}
