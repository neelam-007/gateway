package com.l7tech.objectmodel.migration;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;
import java.io.IOException;

/**
 * Extracts the dependencies and mappings from policy_xml property of Policy objects.
 *
 * The property name to which each dependency is mapped encodes the assertion's ordinal within the policy,
 * in addition to the assertion's method name.
 * 
 * @author jbufu
 */
public class PolicyXmlPropertyResolver extends DefaultEntityPropertyResolver {

    private static final Logger logger = Logger.getLogger(PolicyXmlPropertyResolver.class.getName());

    @Override
    public Map<EntityHeader, Set<MigrationMapping>> getDependencies(EntityHeaderRef source, Object entity, final Method property) throws MigrationException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{property.getName(),source});
        Assertion assertion = getRootAssertion(entity, property);

        String propName = MigrationUtils.propertyNameFromGetter(property.getName());
        Map<EntityHeader, Set<MigrationMapping>> result = new HashMap<EntityHeader, Set<MigrationMapping>>();
        getHeadersRecursive(source, assertion, result, propName);

        logger.log(Level.FINE, "Found {0} headers for property {1}.", new Object[] { result.size(), property });

        return result;
    }

    public void applyMapping(Entity sourceEntity, String propName, Object targetValue, EntityHeader originalHeader) throws MigrationException {
        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{MigrationUtils.getHeaderFromEntity(sourceEntity), propName});
        String policyXmlPropName, assertionPropName;
        int targetOrdinal;
        try {
            String[] tokens = propName.split(":");
            policyXmlPropName = tokens[0];
            targetOrdinal = Integer.parseInt(tokens[1]);
            assertionPropName = tokens[2];
        } catch (Exception e) {
            throw new MigrationException("Error parsing property name: " + propName, e);
        }

        Assertion assertion = getRootAssertion(sourceEntity, MigrationUtils.getterForPropertyName(sourceEntity, policyXmlPropName));
        Iterator iter = assertion.preorderIterator();
        while (iter.hasNext() && !(assertion.getOrdinal() == targetOrdinal)) {
            assertion = (Assertion) iter.next();
        }

        if (!(assertion.getOrdinal() == targetOrdinal))
            throw new MigrationException("Assertion with ordinal " + targetOrdinal + " not found in poilcy.");

        logger.log(Level.FINEST, "Applying mapping for assertion {0} : {1}.", new Object[]{assertion, assertionPropName});
        try {
            Method setter;
            if ("EntitiesUsed".equals(assertionPropName)) {
                setter = MigrationUtils.setterForPropertyName(assertion, "replaceEntity", EntityHeader.class, EntityHeader.class);
                setter.invoke(assertion, originalHeader, MigrationUtils.getHeaderFromEntity((Entity) targetValue));
            } else {
                setter = MigrationUtils.setterForPropertyName(assertion, assertionPropName, targetValue.getClass());
                setter.invoke(assertion, targetValue);
            }
        } catch (Exception e) {
            throw new MigrationException("Error applying mapping for " + propName, e);
        }
    }

    private void getHeadersRecursive(EntityHeaderRef source, Assertion assertion, Map<EntityHeader, Set<MigrationMapping>> result, String topPropertyName) throws MigrationException {

        logger.log(Level.FINE, "Getting headers for assertion: " + assertion);

        // process direct dependencies of this assertion
        for (Method method : assertion.getClass().getMethods()) {
            // todo: handle non-default dependencies here?
            if (MigrationUtils.isDefaultDependency(method)) {
                Map<EntityHeader, Set<MigrationMapping>> deps = super.getDependencies(source, assertion, method);
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

    private Assertion getRootAssertion(Object entity, Method property) throws MigrationException {
        if (entity == null || property == null)
            throw new MigrationException("Error getting property value for entity: entity and property method cannot be null");

        final Object propertyValue;
        try {
            propertyValue = property.invoke(entity);
        } catch (Exception e) {
            throw new MigrationException("Error getting property value for entity: " + entity , e);
        }

        if (! (propertyValue instanceof String))
            throw new MigrationException("Policy_xml values should be of type String, found: " + propertyValue.getClass());

        Assertion assertion;
        try {
            assertion = WspReader.getDefault().parsePermissively((String) propertyValue);
        } catch (IOException e) {
            throw new MigrationException("Error parsing policy_xml.", e);
        }
        return assertion;
    }
}
