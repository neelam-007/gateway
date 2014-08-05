package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.EncassImportContext;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Eithers.*;

/**
 *
 */
@ResourceFactory.ResourceType(type=EncapsulatedAssertionMO.class)
public class EncapsulatedAssertionResourceFactory extends SecurityZoneableEntityManagerResourceFactory<EncapsulatedAssertionMO, EncapsulatedAssertionConfig,ZoneableGuidEntityHeader> {

    //- PUBLIC

    public EncapsulatedAssertionResourceFactory( final RbacServices services,
                                                 final SecurityFilter securityFilter,
                                                 final PlatformTransactionManager transactionManager,
                                                 final EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager,
                                                 final PolicyManager policyManager,
                                                 final EncapsulatedAssertionHelper encapsulatedAssertionHelper,
                                                 final PolicyHelper policyHelper,
                                                 final SecurityZoneManager securityZoneManager) {
        super(false, true, true, services, securityFilter, transactionManager, encapsulatedAssertionConfigManager, securityZoneManager);
        this.policyManager = policyManager;
        this.encapsulatedAssertionHelper = encapsulatedAssertionHelper;
        this.policyHelper = policyHelper;
        this.encapsulatedAssertionConfigManager = encapsulatedAssertionConfigManager;
    }

    /**
     * The import encass method. This will import an encass into the gateway.
     * @param selectorMap The selector map to import into an existing encass. This is optional
     * @param resource The Encass export to import. Required.
     * @return Returns the import results
     * @throws ResourceNotFoundException
     * @throws InvalidResourceException
     */
    @ResourceMethod(name="ImportEncass", selectors=true, resource=true)
    public EncapsulatedAssertionImportResult importEncass( final Map<String,String> selectorMap,
                                            final EncassImportContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException,EncapsulatedAssertionImportResult>>(){
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, EncapsulatedAssertionImportResult> execute() throws ObjectModelException {
                try {
                    //extract the encass from the resource
                    EncapsulatedAssertionConfig encass = EncapsulatedAssertionHelper.importFromNode(resource, true);
                    final Policy policy;
                    final PolicyImportResult policyImportResult;

                    //check if selectors are provided
                    if(selectorMap != null && !selectorMap.isEmpty()) {
                        //If selectors are provided we are importing into an existing encass.
                        EncapsulatedAssertionConfig existingEncass = selectEntity(selectorMap);
                        checkPermitted( OperationType.UPDATE, null, encass );
                        //set the goid of the given encass policy so the the update can happen properly
                        encass.getPolicy().setGoid(existingEncass.getPolicy().getGoid());
                        updateEntity(existingEncass, encass);
                        encass = existingEncass;
                        //use the existing encassed policy
                        policy = encass.getPolicy();
                        policyHelper.checkPolicyAssertionAccess( policy );
                        policyImportResult = policyHelper.importPolicy( policy, resource );
                    } else {
                        //import into a new encass
                        //check if an encass with the same name or guid exists
                        if(encapsulatedAssertionConfigManager.findByGuid(encass.getGuid())!=null || encapsulatedAssertionConfigManager.findByUniqueName(encass.getName()) != null){
                            throw new DuplicateResourceAccessException("EncapsulatedAssertion already exists: Guid: " + encass.getGuid() + " Name: " + encass.getName());
                        }

                        //check if policy exists.
                        if(encass.getPolicy() == null || (encass.getPolicy().getGuid() == null && encass.getPolicy().getName() == null)) {
                            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "EncapsulatedAssertion is missing Policy element");
                        }

                        // Get any provided PolicyReferenceInstruction
                        PolicyReferenceInstruction policyReferenceInstructions = findPolicyReferenceInstructions(resource.getPolicyReferenceInstructions(), encass.getPolicy().getGuid());
                        if(policyReferenceInstructions!=null) {
                            switch(policyReferenceInstructions.getPolicyReferenceInstructionType()){
                                case RENAME: // This will create a new policy with the given name
                                    //check that a policy does not already exist with the name given.
                                    if(policyManager.findByUniqueName(policyReferenceInstructions.getMappedName())!=null) {
                                        throw new DuplicateResourceAccessException("EncapsulatedAssertion backing policy already exists: Guid: " + encass.getPolicy().getGuid() + " Name: " + encass.getPolicy().getName());
                                    }
                                    //create a new policy and import into it.
                                    policy = new Policy(PolicyType.INCLUDE_FRAGMENT, policyReferenceInstructions.getMappedName(), "", false);
                                    policyImportResult = policyHelper.importPolicy( policy, resource );
                                    UUID guid = UUID.randomUUID();
                                    policy.setGuid(guid.toString());
                                    policyManager.save(policy);
                                    break;
                                default: // not opther operations are supported.
                                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "EncapsulatedAssertion PolicyReferenceInstruction uses unsupported instruction type: " + policyReferenceInstructions.getPolicyReferenceInstructionType());
                            }
                        } else if(policyManager.findByGuid(encass.getPolicy().getGuid())!=null|| policyManager.findByUniqueName(encass.getPolicy().getName())!=null) {
                            // if no PolicyReferenceInstruction is specified and the policy exists then fail
                            throw new DuplicateResourceAccessException("EncapsulatedAssertion backing policy already exists: Guid: " + encass.getPolicy().getGuid() + " Name: " + encass.getPolicy().getName());
                        } else {
                            //There is no existing policy with the same name or guid so create a new one.
                            policy = new Policy(PolicyType.INCLUDE_FRAGMENT, encass.getPolicy().getName(), "", false);
                            policy.setGuid(encass.getPolicy().getGuid());
                            policyImportResult = policyHelper.importPolicy( policy, resource );
                            policyManager.save(policy);
                        }
                    }

                    //set the encass's policy
                    encass.setPolicy(policy);
                    //save or update the encass
                    if(Goid.isDefault(encass.getGoid())){
                        encapsulatedAssertionConfigManager.save( encass );
                    } else {
                        encapsulatedAssertionConfigManager.update( encass );
                    }

                    final EncapsulatedAssertionImportResult result = ManagedObjectFactory.createEncapsulatedAssertionImportResult();
                    result.setImportedEncapsulatedAssertion(identify(asResource(encass), encass));
                    result.setImportedPolicyReferences(policyImportResult.getImportedPolicyReferences());
                    result.setWarnings(policyImportResult.getWarnings());

                    //return the result
                    return right2( result );
                } catch ( ResourceNotFoundException e ) {
                    return left2_1( e );
                } catch ( InvalidResourceException e ) {
                    return left2_2( e );
                }
            }
        }, false ) );
    }

    /**
     * This will find a PolicyReferenceInstruction of type IncludedPolicyReference referencing a policy with the given Guid.
     * @param policyReferenceInstructions The list of PolicyReferenceInstruction's to search
     * @param guid The Guid of the referenced policy to find
     * @return The PolicyReferenceInstruction for the IncludedPolicyReference with a reference to the given policy guid. Or null if no such reference exists.
     */
    private static PolicyReferenceInstruction findPolicyReferenceInstructions(List<PolicyReferenceInstruction> policyReferenceInstructions, final String guid) {
        return guid == null || policyReferenceInstructions ==null ? null : Functions.grepFirst(policyReferenceInstructions, new Functions.Unary<Boolean, PolicyReferenceInstruction>() {
            @Override
            public Boolean call(PolicyReferenceInstruction policyReferenceInstruction) {
                return guid.equals(policyReferenceInstruction.getReferenceId()) && "com.l7tech.policy.exporter.IncludedPolicyReference".equals(policyReferenceInstruction.getReferenceType());
            }
        });
    }


    /**
     * This will export an encass. It will return the same export xml generated by the policy manager encass export
     *
     * @param selectorMap The selector map used to select the encass to export
     * @return The encass export result
     * @throws ResourceNotFoundException
     */
    @ResourceMethod(name="ExportEncass", selectors=true)
    public EncapsulatedAssertionExportResult exportEncass( final Map<String,String> selectorMap ) throws ResourceNotFoundException {
        return extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException,EncapsulatedAssertionExportResult>>(){
            @Override
            public Either<ResourceNotFoundException,EncapsulatedAssertionExportResult> execute() throws ObjectModelException {
                try {
                    final EncapsulatedAssertionConfig encass = selectEntity( selectorMap );
                    checkPermitted( OperationType.READ, null, encass );
                    return right( encapsulatedAssertionHelper.exportEncass(encass) );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, true ) );
    }

    //- PROTECTED

    @Override
    public EncapsulatedAssertionConfig fromResource(Object resource, boolean strict) throws InvalidResourceException {
        if ( !(resource instanceof EncapsulatedAssertionMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected encapsulated assertion");

        final EncapsulatedAssertionMO encassResource = (EncapsulatedAssertionMO) resource;

        final EncapsulatedAssertionConfig encassEntity;
            encassEntity = new EncapsulatedAssertionConfig();
            encassEntity.setName(asName(encassResource.getName()));

            String guid = encassResource.getGuid();
            if (guid == null)
                guid = UUID.randomUUID().toString();
            encassEntity.setGuid(guid);

            encassEntity.setProperties(encassResource.getProperties() != null ? new HashMap<>(encassResource.getProperties()) : new HashMap<String, String>());
            encassEntity.setArgumentDescriptors(getArgumentDescriptorSet(encassResource, encassEntity));
            encassEntity.setResultDescriptors(getResultDescriptorSet(encassResource, encassEntity));

        Policy policy = null;
        try {
            policy = policyManager.findByPrimaryKey(toInternalId(EntityType.POLICY, encassResource.getPolicyReference().getId(), "Policy Resource Identifier")) ;
        } catch (FindException e) {
            if(strict)
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown policy reference");
        }
        if (policy == null && strict)
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "unknown policy reference");
        else if(policy == null){
            policy = new Policy(PolicyType.INCLUDE_FRAGMENT, null, null, false);
            policy.setId(encassResource.getPolicyReference().getId());
        }
        checkPermitted(OperationType.READ, null, policy);
        encassEntity.setPolicy(policy);

        // handle SecurityZone
        doSecurityZoneFromResource( encassResource, encassEntity, strict );

        return encassEntity;

    }

    @Override
    public EncapsulatedAssertionMO asResource(EncapsulatedAssertionConfig ec) {
        EncapsulatedAssertionMO er = ManagedObjectFactory.createEncapsulatedAssertion();

        er.setName(ec.getName());
        er.setGuid(ec.getGuid());
        if (ec.getProperties() != null)
            er.setProperties(ec.getProperties());
        final List<EncapsulatedAssertionMO.EncapsulatedArgument> args = getEncapsulatedArgumentsList(ec);
        if (args != null)
            er.setEncapsulatedArguments(args);
        final List<EncapsulatedAssertionMO.EncapsulatedResult> results = getEncapsulatedResultsList(ec);
        if (results != null)
            er.setEncapsulatedResults(results);
        er.setPolicyReference(new ManagedObjectReference(PolicyMO.class, ec.getPolicy().getId()));

        //handle SecurityZone
        doSecurityZoneAsResource( er, ec );

        return er;
    }

    @Override
    protected void updateEntity(EncapsulatedAssertionConfig oldEntity, EncapsulatedAssertionConfig newEntity) throws InvalidResourceException {

        oldEntity.setName(newEntity.getName());

        // For now we disallow changing GUID of an existing encass config to avoid people coming to depend upon this behavior.
        // If we decide this should be allowed after all we can relax this restriction.
        final String newEntityGuid = newEntity.getGuid();
        if (newEntityGuid == null)
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "new encapsulated assertion configuration must include a GUID");
        if (!newEntityGuid.equals(oldEntity.getGuid()))
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "unable to change GUID of existing encapsulated assertion config");

        // For now we disallow changing the backing policy of an existing encass config to avoid people coming to depend upon this behavior.
        // If we later decide this can be allowed after all we can relax this restriction.
        if (newEntity.getPolicy() == null)
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "new encapsulated assertion configuration must reference a backing policy");
        if (oldEntity.getPolicy() != null) {
            if (!Goid.equals(newEntity.getPolicy().getGoid(), oldEntity.getPolicy().getGoid()))
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "unable to change backing policy of an existing encapsulated assertion config");
        } else {
            oldEntity.setPolicy(newEntity.getPolicy());
        }

        for (String name : oldEntity.getPropertyNames()) {
            oldEntity.removeProperty(name);
        }
        for (String name : newEntity.getPropertyNames()) {
            final String value = newEntity.getProperty(name);
            if (value != null)
                oldEntity.putProperty(name, value);
        }

        oldEntity.setArgumentDescriptors(copyArgumentDescriptors(newEntity, oldEntity));
        oldEntity.setResultDescriptors(copyResultDescriptors(newEntity, oldEntity));
        oldEntity.setSecurityZone(newEntity.getSecurityZone());
    }

    //- PRIVATE

    private Set<EncapsulatedAssertionArgumentDescriptor> getArgumentDescriptorSet(EncapsulatedAssertionMO encassResource, EncapsulatedAssertionConfig entity) {
        Set<EncapsulatedAssertionArgumentDescriptor> ret = entity.getArgumentDescriptors();
        if (ret == null)
            ret = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
        ret.clear();

        List<EncapsulatedAssertionMO.EncapsulatedArgument> args = encassResource.getEncapsulatedArguments();
        if (args != null) {
            for (EncapsulatedAssertionMO.EncapsulatedArgument arg : args) {
                EncapsulatedAssertionArgumentDescriptor r = new EncapsulatedAssertionArgumentDescriptor();
                r.setArgumentName(arg.getArgumentName());
                r.setArgumentType(arg.getArgumentType());
                r.setGuiLabel(arg.getGuiLabel());
                r.setGuiPrompt(arg.isGuiPrompt());
                r.setOrdinal(arg.getOrdinal());
                r.setEncapsulatedAssertionConfig(entity);
                ret.add(r);
            }
        }

        return ret;
    }

    private Set<EncapsulatedAssertionResultDescriptor> getResultDescriptorSet(EncapsulatedAssertionMO encassResource, EncapsulatedAssertionConfig entity) {
        Set<EncapsulatedAssertionResultDescriptor> ret = entity.getResultDescriptors();
        if (ret == null)
            ret = new HashSet<EncapsulatedAssertionResultDescriptor>();
        ret.clear();

        List<EncapsulatedAssertionMO.EncapsulatedResult> results = encassResource.getEncapsulatedResults();
        if (results != null) {
            for (EncapsulatedAssertionMO.EncapsulatedResult result : results) {
                EncapsulatedAssertionResultDescriptor r = new EncapsulatedAssertionResultDescriptor();
                r.setResultName(result.getResultName());
                r.setResultType(result.getResultType());
                r.setEncapsulatedAssertionConfig(entity);
                ret.add(r);
            }
        }

        return ret;
    }

    private List<EncapsulatedAssertionMO.EncapsulatedArgument> getEncapsulatedArgumentsList(EncapsulatedAssertionConfig entity) {
        List<EncapsulatedAssertionMO.EncapsulatedArgument> ret = new ArrayList<EncapsulatedAssertionMO.EncapsulatedArgument>();

        Set<EncapsulatedAssertionArgumentDescriptor> args = entity.getArgumentDescriptors();
        if (args != null) {
            for (EncapsulatedAssertionArgumentDescriptor arg : args) {
                EncapsulatedAssertionMO.EncapsulatedArgument ea = new EncapsulatedAssertionMO.EncapsulatedArgument();
                ea.setArgumentName(arg.getArgumentName());
                ea.setArgumentType(arg.getArgumentType());
                ea.setGuiLabel(arg.getGuiLabel());
                ea.setGuiPrompt(arg.isGuiPrompt());
                ea.setOrdinal(arg.getOrdinal());
                ret.add(ea);
            }
        }

        return ret;
    }

    private List<EncapsulatedAssertionMO.EncapsulatedResult> getEncapsulatedResultsList(EncapsulatedAssertionConfig entity) {
        List<EncapsulatedAssertionMO.EncapsulatedResult> ret = new ArrayList<EncapsulatedAssertionMO.EncapsulatedResult>();

        Set<EncapsulatedAssertionResultDescriptor> results = entity.getResultDescriptors();
        if (results != null) {
            for (EncapsulatedAssertionResultDescriptor result : results) {
                EncapsulatedAssertionMO.EncapsulatedResult er = new EncapsulatedAssertionMO.EncapsulatedResult();
                er.setResultName(result.getResultName());
                er.setResultType(result.getResultType());
                ret.add(er);
            }
        }

        return ret;
    }

    private Set<EncapsulatedAssertionArgumentDescriptor> copyArgumentDescriptors(EncapsulatedAssertionConfig source, EncapsulatedAssertionConfig dest) {
        Set<EncapsulatedAssertionArgumentDescriptor> ret = dest.getArgumentDescriptors();
        if (ret == null)
            ret = new LinkedHashSet<EncapsulatedAssertionArgumentDescriptor>();
        ret.clear();

        Set<EncapsulatedAssertionArgumentDescriptor> args = source.getArgumentDescriptors();
        if (args != null) {
            for (EncapsulatedAssertionArgumentDescriptor arg : args) {
                ret.add(arg.getCopy(dest, false));
            }
        }

        return ret;
    }

    private Set<EncapsulatedAssertionResultDescriptor> copyResultDescriptors(EncapsulatedAssertionConfig source, EncapsulatedAssertionConfig dest) {
        Set<EncapsulatedAssertionResultDescriptor> ret = dest.getResultDescriptors();
        if (ret == null)
            ret = new LinkedHashSet<EncapsulatedAssertionResultDescriptor>();
        ret.clear();

        Set<EncapsulatedAssertionResultDescriptor> results = source.getResultDescriptors();
        if (results != null) {
            for (EncapsulatedAssertionResultDescriptor result : results) {
                ret.add(result.getCopy(dest, false));
            }
        }

        return ret;
    }

    private final PolicyManager policyManager;
    private EncapsulatedAssertionHelper encapsulatedAssertionHelper;
    private PolicyHelper policyHelper;
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;
}
