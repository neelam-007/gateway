package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

/**
 *
 */
@ResourceFactory.ResourceType(type=EncapsulatedAssertionMO.class)
public class EncapsulatedAssertionResourceFactory extends SecurityZoneableEntityManagerResourceFactory<EncapsulatedAssertionMO, EncapsulatedAssertionConfig,GuidEntityHeader> {

    //- PUBLIC

    public EncapsulatedAssertionResourceFactory( final RbacServices services,
                                                 final SecurityFilter securityFilter,
                                                 final PlatformTransactionManager transactionManager,
                                                 final EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager,
                                                 final PolicyManager policyManager,
                                                 final SecurityZoneManager securityZoneManager) {
        super(false, true, true, services, securityFilter, transactionManager, encapsulatedAssertionConfigManager, securityZoneManager);
        this.policyManager = policyManager;
    }

    //- PROTECTED

    @Override
    protected EncapsulatedAssertionConfig fromResource(Object resource) throws InvalidResourceException {
        if ( !(resource instanceof EncapsulatedAssertionMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected encapsulated assertion");

        final EncapsulatedAssertionMO encassResource = (EncapsulatedAssertionMO) resource;

        final EncapsulatedAssertionConfig encassEntity;
        try {
            encassEntity = new EncapsulatedAssertionConfig();
            encassEntity.setName(asName(encassResource.getName()));

            String guid = encassResource.getGuid();
            if (guid == null)
                guid = UUID.randomUUID().toString();
            encassEntity.setGuid(guid);

            encassEntity.setProperties(new HashMap<String, String>(encassResource.getProperties()));
            encassEntity.setArgumentDescriptors(getArgumentDescriptorSet(encassResource, encassEntity));
            encassEntity.setResultDescriptors(getResultDescriptorSet(encassResource, encassEntity));

            Policy policy = policyManager.findByPrimaryKey(toInternalId(EntityType.POLICY, encassResource.getPolicyReference().getId(), "Policy Resource Identifier")) ;
            if (policy == null)
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "unknown policy reference");
            checkPermitted(OperationType.READ, null, policy);
            encassEntity.setPolicy(policy);

        } catch (FindException e) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown policy reference");
        }

        // handle SecurityZone
        doSecurityZoneFromResource( encassResource, encassEntity );

        return encassEntity;

    }

    @Override
    protected EncapsulatedAssertionMO asResource(EncapsulatedAssertionConfig ec) {
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
}
