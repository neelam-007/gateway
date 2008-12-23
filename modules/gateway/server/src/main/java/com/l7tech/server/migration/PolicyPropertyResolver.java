package com.l7tech.server.migration;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.Policy;
import com.l7tech.policy.wsp.WspWriter;
//import com.l7tech.policy.wsp.WspReader;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.gateway.common.service.PublishedService;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;
import java.io.IOException;

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

    @Override
    public Map<EntityHeader, Set<MigrationMapping>> getDependencies(EntityHeaderRef source, Object entity, final Method property) throws PropertyResolverException {
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

        String propName = MigrationUtils.propertyNameFromGetter(property.getName());
        Map<EntityHeader, Set<MigrationMapping>> result = new HashMap<EntityHeader, Set<MigrationMapping>>();
        getHeadersRecursive(source, assertion, result, propName);

        logger.log(Level.FINE, "Found {0} headers for property {1}.", new Object[] { result.size(), property });

        return result;
    }

    public void applyMapping(Entity sourceEntity, String propName, Object targetValue, EntityHeader originalHeader) throws PropertyResolverException {
        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{EntityHeaderUtils.fromEntity(sourceEntity), propName});

        if (sourceEntity instanceof PublishedService && ! propName.contains(":")) {
            // set the policy in the targetValue, but keep the existing service's policy's oid/version
            Policy originalPolicy = ((PublishedService) sourceEntity).getPolicy();
            long originalPolicyOid = originalPolicy.getOid();
            int originalPolicyVersion = originalPolicy.getVersion();

            super.applyMapping(sourceEntity, propName, targetValue, originalHeader);

            ((PublishedService) sourceEntity).getPolicy().setOid(originalPolicyOid);
            ((PublishedService) sourceEntity).getPolicy().setVersion(originalPolicyVersion);

        } else if (sourceEntity instanceof PublishedService) {
            applyMappingToPolicy(((PublishedService)sourceEntity).getPolicy(), propName, targetValue, originalHeader);
        } else if (sourceEntity instanceof Policy) {
            applyMappingToPolicy((Policy)sourceEntity, propName, targetValue, originalHeader);
        } else {
            throw new PropertyResolverException("Cannot handle entity of type: " + (sourceEntity != null ? sourceEntity.getClass().getName() : null));
        }
    }

    private void applyMappingToPolicy(Policy policy, String propName, Object targetValue, EntityHeader originalHeader) throws PropertyResolverException {
        String assertionPropName;
        int targetOrdinal;
        try {
            String[] tokens = propName.split(":");
            targetOrdinal = Integer.parseInt(tokens[1]);
            assertionPropName = tokens[2];
        } catch (Exception e) {
            throw new PropertyResolverException("Error parsing property name: " + propName, e);
        }

        Assertion rootAssertion;
        try {
            rootAssertion = policy.getAssertion();
        } catch (Exception e) {
            throw new PropertyResolverException("Error getting root assertion from policy.", e);
        }

        Assertion assertion = rootAssertion;
        Iterator iter = assertion.preorderIterator();
        while (iter.hasNext() && !(assertion.getOrdinal() == targetOrdinal)) {
            assertion = (Assertion) iter.next();
        }

        if (!(assertion.getOrdinal() == targetOrdinal))
            throw new PropertyResolverException("Assertion with ordinal " + targetOrdinal + " not found in poilcy.");

        logger.log(Level.FINEST, "Applying mapping for assertion {0} : {1}.", new Object[]{assertion, assertionPropName});
        try {
            Method setter;
            if ("EntitiesUsed".equals(assertionPropName)) {
                setter = MigrationUtils.setterForPropertyName(assertion, "replaceEntity", EntityHeader.class, EntityHeader.class);
                setter.invoke(assertion, originalHeader, EntityHeaderUtils.fromEntity((Entity) targetValue));
            } else {
                setter = MigrationUtils.setterForPropertyName(assertion, assertionPropName, targetValue.getClass());
                setter.invoke(assertion, targetValue);
            }
        } catch (Exception e) {
            throw new PropertyResolverException("Error applying mapping for " + propName, e);
        }
        policy.setXml(WspWriter.getPolicyXml(rootAssertion));
    }

    private void getHeadersRecursive(EntityHeaderRef source, Assertion assertion, Map<EntityHeader, Set<MigrationMapping>> result, String topPropertyName) throws PropertyResolverException {

        logger.log(Level.FINE, "Getting headers for assertion: " + assertion);

        // process direct dependencies of this assertion
        for (Method method : assertion.getClass().getMethods()) {
            if (MigrationUtils.isDependency(method)) {
                // todo: figure out how to get a hold of resolvers specified through Migration.targetType()
                PropertyResolver resolver = MigrationUtils.getResolver(method);
                Map<EntityHeader, Set<MigrationMapping>> deps = resolver.getDependencies(source, assertion, method);
                for (EntityHeader depHeader : deps.keySet()) {
                    for (MigrationMapping mapping : deps.get(depHeader)) {
                        // use assertion's ordinal to identify where in the policy xml each dependency can be mapped
                        mapping.setPropName(topPropertyName + ":" + Integer.toString(assertion.getOrdinal()) + ":" + mapping.getPropName());
                        addToResult(depHeader, mapping, result);
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
