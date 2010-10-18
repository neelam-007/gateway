package com.l7tech.server.migration;

import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.GlobalResourceInfo;
import com.l7tech.server.globalresources.ResourceEntryManager;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;

/**
 * @author jbufu
 */
public class ResourceEntryPropertyResolver extends AbstractPropertyResolver {
    private static final Logger logger = Logger.getLogger( ResourceEntryPropertyResolver.class.getName());

    private ResourceEntryManager resourceEntryManager;

    public ResourceEntryPropertyResolver(PropertyResolverFactory factory, Type type, ResourceEntryManager manager) {
        super(factory, type);
        this.resourceEntryManager = manager;
    }

    @Override
    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{property.getName(),source});

        final MigrationMappingSelection mappingType = MigrationUtils.getMappingType(property);
        final MigrationMappingSelection valueMappingType = MigrationUtils.getValueMappingType(property);
        final ExternalEntityHeader.ValueType valueType = MigrationUtils.getValueType(property);
        final boolean exported = MigrationUtils.isExported(property);

        try {
            AssertionResourceInfo res = (AssertionResourceInfo) getPropertyValue(entity, property);
            Map<ExternalEntityHeader,Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();
            if (AssertionResourceType.GLOBAL_RESOURCE == res.getType()) {
                final String schemaName = ((GlobalResourceInfo)res).getId();
                final ResourceEntry schema = resourceEntryManager.findResourceByUriAndType(schemaName, ResourceType.XML_SCHEMA);
                final ExternalEntityHeader schemaHeader = EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(schema));
                schemaHeader.setValueMapping(valueMappingType, valueType, schemaName);
                result.put(schemaHeader, Collections.singleton(new MigrationDependency(source, schemaHeader, propertyName, getType(), mappingType, exported)));
            }
            return result;
        } catch (Exception e) {
            throw new PropertyResolverException("Error retrieving schema entries for: " + propertyName, e);
        }
    }

    @Override
    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {

        if (! (targetValue instanceof ResourceEntry))
            throw new PropertyResolverException("ResourceEntry target value expected, got: " + (targetValue != null ? targetValue.getClass() : null));

        logger.log(Level.FINEST, "Applying mapping for {0}.", sourceEntity);

        try {
            String schemName = ((ResourceEntry)targetValue).getUri();
            Method method = MigrationUtils.setterForPropertyName(sourceEntity, propName, AssertionResourceInfo.class);
            GlobalResourceInfo gri = new GlobalResourceInfo();
            gri.setId(schemName);
            method.invoke(sourceEntity, gri);
        } catch (Exception e) {
            throw new PropertyResolverException("Error applying mapping for property name: " + propName, e);
        }

    }
}
