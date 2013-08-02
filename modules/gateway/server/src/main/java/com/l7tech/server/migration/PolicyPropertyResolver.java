package com.l7tech.server.migration;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.MigrationUtils;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.migration.PropertyResolverException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.EntityHeaderUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extracts the dependencies and mappings from a Policy object belonging to a service,
 * or from a policy_xml property of Policy objects.
 *
 * The property name to which each dependency is mapped encodes the assertion's ordinal within the policy,
 * in addition to the assertion's method name.
 * 
 * When applying mappings for Service entities, and if the Service needs to up updated (rather than copied),
 * the OID and version of the policy from the mapped/target entity must be preserved.
 *
 * @author jbufu
 */
public class PolicyPropertyResolver extends DefaultEntityPropertyResolver {

    private static final Logger logger = Logger.getLogger(PolicyPropertyResolver.class.getName());

    public PolicyPropertyResolver(PropertyResolverFactory factory, Type type) {
        super(factory, type);
    }

    @Override
    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, final Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{property.getName(),source});

        Policy policy;
        if (entity instanceof PublishedService) {
            policy = ((PublishedService)entity).getPolicy();
        } else if (entity instanceof Policy) {
            policy = (Policy) entity;
        } else {
            throw new PropertyResolverException("Cannot handle entity of type: " + (entity != null ? entity.getClass().getName() : null));
        }

        Assertion assertion;
        try {
            assertion = policy.getAssertion();
        } catch (Exception e) {
            throw new PropertyResolverException("Error getting root assertion from policy.", e);
        }

        Map<ExternalEntityHeader, Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();
        getHeadersRecursive(source, assertion, result, propertyName);

        logger.log(Level.FINE, "Found {0} headers for property {1}.", new Object[] { result.size(), property });

        return result;
    }

    @Override
    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {
        if (! (sourceEntity instanceof Entity))
            throw new PropertyResolverException("Cannot handle non-entities; received: " + (sourceEntity == null ? null : sourceEntity.getClass()));

        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{EntityHeaderUtils.fromEntity((Entity) sourceEntity), propName});

        if (sourceEntity instanceof PublishedService && ! propName.contains(":")) {
            // set the policy in the targetValue, but keep the existing service's policy's oid/version
            Policy originalPolicy = ((PublishedService) sourceEntity).getPolicy();
            Goid originalPolicyGoid = originalPolicy.getGoid();
            int originalPolicyVersion = originalPolicy.getVersion();

            super.applyMapping(sourceEntity, propName, targetHeader, targetValue, originalHeader);

            ((PublishedService) sourceEntity).getPolicy().setGoid(originalPolicyGoid);
            ((PublishedService) sourceEntity).getPolicy().setVersion(originalPolicyVersion);

        } else if (sourceEntity instanceof PublishedService) {
            applyMappingToPolicy(((PublishedService)sourceEntity).getPolicy(), propName, targetHeader, targetValue, originalHeader);
        } else if (sourceEntity instanceof Policy) {
            applyMappingToPolicy((Policy)sourceEntity, propName, targetHeader, targetValue, originalHeader);
        } else {
            throw new PropertyResolverException("Cannot handle entity of type: " + sourceEntity.getClass().getName());
        }
    }

    private void applyMappingToPolicy(Policy policy, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {

        Assertion assertion = MigrationUtils.getAssertion(policy, propName);

        String assertionPropName;
        try {
            String[] tokens = propName.split(":");
            assertionPropName = tokens[2];
        } catch (Exception e) {
            throw new PropertyResolverException("Error parsing property name: " + propName, e);
        }

        Method getter = MigrationUtils.getterForPropertyName(assertion, assertionPropName);
        PropertyResolver resolver = getResolver(getter);
        resolver.applyMapping(assertion, assertionPropName, targetHeader, targetValue, originalHeader);

        // The policy XML will be updated from the root assertion prior to being stored
    }

    private void getHeadersRecursive(ExternalEntityHeader source, @Nullable Assertion assertion, Map<ExternalEntityHeader, Set<MigrationDependency>> result, String topPropertyName) throws PropertyResolverException {
        if (assertion == null)
            return;

        logger.log(Level.FINE, "Getting headers for assertion: " + assertion);

        // process direct dependencies of this assertion
        for (Method method : assertion.getClass().getMethods()) {
            if (MigrationUtils.isDependency(method)) {
                PropertyResolver resolver = getResolver(method);
                // use assertion's ordinal to identify where in the policy xml each dependency can be mapped
                String propertyName = topPropertyName + ":" + Integer.toString(assertion.getOrdinal()) + ":" + MigrationUtils.propertyNameFromGetter(method.getName());
                Map<ExternalEntityHeader, Set<MigrationDependency>> deps = resolver.getDependencies(source, assertion, method, propertyName);
                for (ExternalEntityHeader depHeader : deps.keySet()) {
                    for (MigrationDependency dep : deps.get(depHeader)) {
                        addToResult(depHeader, dep, result);
                    }
                }
                logger.log(Level.FINE, "Extracted {0} headers for assertion {1}.", new Object[]{deps.size(), assertion});
            }
        }

        // recurse
        if (assertion instanceof CompositeAssertion) {
            List children = ((CompositeAssertion)assertion).getChildren();
            for(Object child : children) {
                getHeadersRecursive(source, (Assertion) child, result, topPropertyName);
            }
        }
    }
}
