package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.POLICY;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Remote admin interface for managing {@link com.l7tech.policy.Policy} instances on the Gateway.
 * @author alex
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=POLICY)
@Administrative
public interface PolicyAdmin extends AliasAdmin<PolicyAlias> {
    /**
     * This is a container for a PolicyCheckpointState for the just saved policy and a map of the new policy
     * fragment names to their new OIDs.
     */
    public static class SavePolicyWithFragmentsResult implements Serializable {
        public PolicyCheckpointState policyCheckpointState;
        public Map<String, String> fragmentNameGuidMap;

        public SavePolicyWithFragmentsResult(PolicyCheckpointState policyCheckpointState, Map<String, String> fragmentNameGuidMap) {
            this.policyCheckpointState = policyCheckpointState;
            this.fragmentNameGuidMap = fragmentNameGuidMap;
        }
    }

    String ROLE_NAME_TYPE_SUFFIX = "Policy";
    String ROLE_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;

    /**
     * Finds a particular {@link com.l7tech.policy.Policy} with the specified GOID, or null if no such policy can be found.
     * @param goid the GOID of the Policy to retrieve
     * @return the Policy with the specified GOID, or null if no such policy can be found.
     */
    @Secured(stereotype=FIND_ENTITY)
    @Transactional(readOnly=true)
    @Administrative(licensed = false)
    Policy findPolicyByPrimaryKey(Goid goid) throws FindException;

    /**
     * Finds a particular {@link Policy} with the specified OID, or null if no such policy can be found.
     * @param name the unique name of the Policy to retrieve
     * @return the Policy with the specified OID, or null if no such policy can be found.
     */
    @Secured(stereotype=FIND_ENTITY)
    @Transactional(readOnly=true)
    @Administrative(licensed = false)
    Policy findPolicyByUniqueName(String name) throws FindException;

    /**
     * Finds a particular {@link Policy} with the specified GUID, or null if no such policy can be found.
     * @param guid the GUID of the Policy to retrieve
     * @return the Policy with the specified GUID, or null if no such policy can be found.
     */
    @Secured(stereotype=FIND_ENTITY)
    @Transactional(readOnly=true)
    @Administrative(licensed = false)
    Policy findPolicyByGuid(String guid) throws FindException;

    /**
     * Finds policies matching the specified GUIDs.  If a GUID does not match, it's not added to the list.
     * @param guids the GUIDs of the Policies to retrieve. Required.
     * @return the list of matching Policies.  If no GUIDs match return empty list.
     */
    @NotNull
    @Secured(stereotype=FIND_ENTITIES)
    @Transactional(readOnly=true)
    @Administrative(licensed = false)
    List<Policy> findPoliciesByGuids(@NotNull List<String> guids) throws FindException;

    /**
     * Finds policies with the specified policy type, tag, and sub tag.
     * @param policyType   policy type to find. Required.
     * @param internalTag  policy tag to find, or null to avoid filtering by policy tag.
     * @param internalSubTag policy sub tag to find, or null to avoid filtering by policy sub tag.
     * @return a collection of policies with the requested policy type, tag, and sub tag.  May be empty but never null.
     * @throws FindException on database error
     */
    @NotNull
    @Secured(stereotype=FIND_ENTITIES)
    @Transactional(readOnly=true)
    Collection<Policy> findPoliciesByTypeTagAndSubTag( @NotNull PolicyType policyType, @Nullable String internalTag, @Nullable String internalSubTag ) throws FindException;

    /**
     * Finds all policies in the system with the given type.
     * @param type the type of policies to find; pass <code>null</code> for policies of any type
     * @return the policies with the specified type
     */
    @Secured(stereotype=FIND_HEADERS)
    @Transactional(readOnly=true)
    @Administrative(licensed = false)
    Collection<PolicyHeader> findPolicyHeadersByType(PolicyType type) throws FindException;

    /**
     * Finds all policies in the system with any of the given types.
     * @param types the types of policies to find; pass <code>null</code> for policies of any type
     * @return the policies with the specified type
     * @throws FindException
     */
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    @Administrative(licensed = false)
    Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types) throws FindException;

    /**
     * Finds all policies in the system with any of the given types. Overridden to allow client to specify if
     * results should contain aliases or not
     * @param types the types of policies to find; pass <code>null</code> for policies of any type
     * @param includeAliases true if you want aliases to be included, false otherwise
     * @return the policies with the specified type
     * @throws FindException
     */
    @Transactional(readOnly = true)
    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    @Administrative(licensed = false)
    Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types, boolean includeAliases)
            throws FindException;

    /**
     * Deletes the policy with the specified GOID.  An impact analysis will be performed prior to deleting the policy,
     * and if any other entity references the policy, {@link PolicyDeletionForbiddenException} will be thrown.
     * @param goid the GOID of the policy to be deleted.
     * @throws PolicyDeletionForbiddenException if the Policy with the specified GOID cannot be deleted because it is
     *         referenced by another entity (e.g. a {@link com.l7tech.gateway.common.service.PublishedService})
     */
    @Secured(stereotype=DELETE_BY_ID)
    void deletePolicy(Goid goid) throws PolicyDeletionForbiddenException, DeleteException, FindException, ConstraintViolationException;

    /**
     * Saves or updates the specified policy.
     * <p/>
     * The policy XML will be made the active version of this policy.
     * <p/>
     * This method is the same as {@link #savePolicy(Policy, boolean)} with <b>true</b> passed
     * as the second argument.
     *
     * @param policy the policy to be saved.
     * @return the GOID/GUID pair for he policy that was saved.
     * @throws PolicyAssertionException if there is a problem with the policy
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Pair<Goid,String> savePolicy(Policy policy) throws PolicyAssertionException, SaveException;

    /**
     * Saves or updates the specified PolicyAlias.
     *
     * @param policyAlias the PolicyAlias to be saved.
     * @return the GOID of the PolicyAlias that was saved.
     * @throws SaveException
     */
    @Override
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid saveAlias(PolicyAlias policyAlias) throws SaveException;

    /**
     * Saves or updates the specified policy.
     * <p/>
     * The policy XML will be made the active version of this policy if activateAsWell is true.
     *
     * @param policy the policy to be saved.
     * @param activateAsWell if true, the new version of the policy XML will take effect immediately
     *                       as the active version of the policy XML.
     *                       if false, the new version of the policy XML will be stored as a new revision
     *                       but will not take effect.
     *                       (<b>NOTE:</b> Any other changes to the Policy bean, aside from policy XML, will
     *                       ALWAYS take effect immediately.)
     * @return PolicyCheckpointState describing the status of the policy after the save operation completed
     * @throws PolicyAssertionException if there is a problem with the policy
     */
    @Secured(stereotype=SAVE_OR_UPDATE, relevantArg=0)
    PolicyCheckpointState savePolicy(Policy policy, boolean activateAsWell) throws PolicyAssertionException, SaveException;

    /**
     * Saves or updates the specified policy.
     * <p/>
     * The policy XML will be made the active version of this policy if activateAsWell is true.
     *
     * @param policy the policy to be saved.
     * @param activateAsWell if true, the new version of the policy XML will take effect immediately
     *                       as the active version of the policy XML.
     *                       if false, the new version of the policy XML will be stored as a new revision
     *                       but will not take effect.
     *                       (<b>NOTE:</b> Any other changes to the Policy bean, aside from policy XML, will
     *                       ALWAYS take effect immediately.)
     * @param fragments the policy fragments that may need to be saved along with this policy
     * @return PolicyCheckpointState describing the status of the policy after the save operation completed
     * @throws PolicyAssertionException if there is a problem with the policy
     */
    @Secured(stereotype=SAVE_OR_UPDATE, relevantArg=0)
    SavePolicyWithFragmentsResult savePolicy(Policy policy, boolean activateAsWell, HashMap<String, Policy> fragments) throws PolicyAssertionException, SaveException;

    @Secured(stereotype = FIND_ENTITIES)
    Set<Policy> findUsages(Goid goid) throws FindException;

    /**
     * Find a particular policy revision by its GOID.
     *
     * @param policyGoid the GOID of the policy this is a version of.  Required.
     * @param versionGoid the GOID to find.  Required.
     * @return the requested PolicyVersion, or null if that GOID wasn't found.
     * @throws FindException if there is a database problem
     */
    @Secured(stereotype=GET_PROPERTY_BY_ID, relevantArg=0)
    @Transactional(readOnly=true)
    PolicyVersion findPolicyVersionByPrimaryKey(Goid policyGoid, Goid versionGoid) throws FindException;

    /**
     * Get summary information about all revisions tracked for the specified policy.
     * <p/>
     * This returns a list of PolicyVersion instances whose "xml" properties are all null.
     *
     * @param policyGoid the GOID of the owning Policy.  Required.
     * @return a List of PolicyVersion instances whose "xml" properties are all omitted (null).  May be empty but never null.
     * @throws FindException if there is a database problem
     */
    @Secured(stereotype=GET_PROPERTY_BY_ID, relevantArg=0)
    @Transactional(readOnly=true)
    List<PolicyVersion> findPolicyVersionHeadersByPolicy(Goid policyGoid) throws FindException;

    /**
     * Set the comment for the specified policy revision.  Setting the comment to anything but null will
     * protect this revision from being deleted automatically.
     *
     * @param policyGoid the GOID of the owning Policy.  Required.
     * @param versionGoid the GOID of the revision whose comment to set.  Required.
     * @param comment the comment to assign to this revision, or null to clear any comment.
     * @throws FindException if the specified policy or revision doesn't exist, or if the specified revision
     *                       is not owned by the specified policy.
     * @throws UpdateException if there is a problem setting the comment.
     */
    @Secured(stereotype=SET_PROPERTY_BY_ID, relevantArg=0)
    void setPolicyVersionComment(Goid policyGoid, Goid versionGoid, String comment) throws FindException, UpdateException;

    /**
     * Set the active version for the specified policy to the specified version.
     * This will mutate the current Policy but without adding any new entries to this policy's revision history.
     *
     * @param policyGoid the GOID of the Policy to alter.  Required.
     * @param versionGoid the GOID of the revision to set as the active revision,
     *                   or {@link PolicyVersion#DEFAULT_GOID} to clear the active revision.  Required.
     * @throws FindException if the specified policy or revision doesn't exist, or if the specified revision
     *                       is not owned by the specified policy.
     */
    @Secured(stereotype=SET_PROPERTY_BY_ID, relevantArg=0)
    void setActivePolicyVersion(Goid policyGoid, Goid versionGoid) throws FindException, UpdateException;

    /**
     * Get the active PolicyVersionfor the specified policy.
     *
     * @param policyGoid the GOID of the Policy whose active version to look up.
     * @return the PolicyVersion that is active for this Policy, or null if no active version was found for the specified policy GOID.
     * @throws FindException if there is a problem looking up the requested information
     */
    @Secured(stereotype=GET_PROPERTY_BY_ID, relevantArg=0)
    PolicyVersion findActivePolicyVersionForPolicy(Goid policyGoid) throws FindException;

    /**
     * Get the particular PolicyVersion matching two keys: policyGoid and ordinal.
     *
     * @param policyGoid: the policy GOID
     * @param versionOrdinal: the policy version ordinal
     * @return the PolicyVersion with full details
     * @throws FindException: thrown if found more than one PolicyVersion.
     */
    @Secured(stereotype=GET_PROPERTY_BY_ID, relevantArg=0)
    PolicyVersion findPolicyVersionForPolicy(Goid policyGoid, long versionOrdinal) throws FindException;

    /**
     * Clear the active version for the specified policy.
     * This will have the effect of disabling the current Policy, but without adding any new entries to this policy's
     * revision history.
     *
     * @param policyGoid the GOID of the Policy to disable.  Required.
     * @throws FindException if the specified policy doesn't exist.
     */
    @Secured(stereotype=SET_PROPERTY_BY_ID, relevantArg=0)
    void clearActivePolicyVersion(Goid policyGoid) throws FindException, UpdateException;

    /**
     * Get the default policy XML for a policy. User must be able to create a policy to consume this method.
     * Default policy XML can be configured elsewhere and the client does not need to use this method when creating
     * a policy.
     * This method can be checked to see if there is default XML, if a client is interested in using it.
     *
     * @param type the type of policy
     * @param internalTag the internal tag if the policy type has a relevant one
     * @return policy XML, null if no relevant policy xml for the type and tag.
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    @Transactional(readOnly = true)
    String getDefaultPolicyXml(PolicyType type, String internalTag);

    /**
     * Represents a description of a policy tag.
     */
    public static final class PolicyTagInfo implements Serializable {
        private static final long serialVersionUID = 4436438769393913753L;

        /** The policy tag name.  Required. */
        @NotNull
        public final String policyTag;

        /** Default policy XML for this tag, or null. */
        @Nullable
        public final String defaultPolicyXml;

        /** Sub tag names for this tag, or empty if no sub tags are supported. */
        @NotNull
        public final Set<String> policySubTags;

        public PolicyTagInfo( @NotNull String policyTag, @Nullable String defaultPolicyXml, @Nullable Set<String> policySubTags ) {
            this.policyTag = policyTag;
            this.defaultPolicyXml = defaultPolicyXml;
            this.policySubTags = policySubTags == null
                                        ? Collections.<String>emptySet()
                                        : policySubTags;
        }
    }

    /**
     * Get any dynamically-registered policy tag (and sub tag) values that should be offered for the specified policy type (if any).
     *
     * @param policyType policy type whose tags to obtain.  Required.
     * @return a Collection of PolicyTagInfo instances, each describing a policy tag to add to the available set
     *         for the specified policy type.  May be empty, but never null.
     */
    @NotNull
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    @Transactional(readOnly = true)
    Collection<PolicyTagInfo> getPolicyTags( @NotNull PolicyType policyType );

    /**
     * Get the xml part max bytes value set in the io.xmlPartMaxBytes cluster property
     * @return the xml part max bytes value set in the io.xmlPartMaxBytes cluster property
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    long getXmlMaxBytes();

    /**
     * Find all ExternalReferenceFactory's, which have been registered when the gateway loads modular assertions.
     * @return a set of ExternalReferenceFactory's
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    Set<ExternalReferenceFactory> findAllExternalReferenceFactories();

    /**
     * Finds the latest PolicyVersion for the given Policy Goid.
     *
     * @param policyGoid the GOID of the policy whose versions will be searched. Required.
     * @return the latest PolicyVersion.
     */
    @Secured(stereotype=GET_PROPERTY_BY_ID, relevantArg=0)
    @Transactional(readOnly=true)
    PolicyVersion findLatestRevisionForPolicy(Goid policyGoid);

    @Transactional(readOnly = true)
    @Secured(stereotype = FIND_ENTITY, types = EntityType.POLICY)
    Policy findByAlias(final Goid aliasGoid) throws FindException;
}