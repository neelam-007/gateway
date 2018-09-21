package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.AttributePredicate;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.RbacAttributeCollector;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.util.nameresolver.EntityNameResolver;

import java.util.Map;
/**
 * Name resolver for Attribute Predicate Entity
 */
public class AttributePredicateNameResolver extends EntityNameResolver {
    private AssertionRegistry assertionRegistry;

    public AttributePredicateNameResolver(AssertionRegistry assertionRegistry, FolderAdmin folderAdmin) {
        super(folderAdmin);
        this.assertionRegistry = assertionRegistry;
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof AttributePredicate;
    }
    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        final AttributePredicate predicate = (AttributePredicate) entity;
        final Permission permission = predicate.getPermission();
        final String mode = predicate.getMode();
        final String attribute = getDisplayNameForAttribute(predicate, permission);
        String name = null;
        String value = getDisplayNameForValue(predicate, permission);
        if (mode == null || AttributePredicate.EQUALS.equalsIgnoreCase(mode) || AttributePredicate.STARTS_WITH.equalsIgnoreCase(mode)) {
            final String operation = AttributePredicate.STARTS_WITH.equalsIgnoreCase(mode) ? "starts with" : "equals";
            name = attribute + " " + operation + " " + value;
        } else {
            // unknown mode
            name = "attribute=" + attribute + " mode=" + mode + " value=" + value;
        }
        return name;
    }

    private String getDisplayNameForValue(final AttributePredicate predicate, final Permission permission) {
        String value = predicate.getValue();
        if (permission != null &&
                permission.getEntityType() == EntityType.ASSERTION_ACCESS &&
                (predicate.getMode() == null || AttributePredicate.EQUALS.equalsIgnoreCase(predicate.getMode()))) {
            // we don't want to show the full class name
            final Assertion assertion = assertionRegistry.findByClassName(predicate.getValue());
            if (assertion != null) {
                value = getNameForAssertion(assertion, predicate.getValue());
            }
        }
        return value;
    }

    private String getDisplayNameForAttribute(final AttributePredicate predicate, final Permission permission) {
        String attribute = predicate.getAttribute();
        if (permission != null && permission.getEntityType() != null) {
            final Map<String, String> availableAttributes = RbacAttributeCollector.collectAttributes(permission.getEntityType());
            if (availableAttributes.containsKey(attribute)) {
                attribute = availableAttributes.get(attribute);
            }
        }
        return attribute;
    }
}
