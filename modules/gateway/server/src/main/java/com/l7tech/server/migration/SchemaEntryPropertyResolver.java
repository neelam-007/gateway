package com.l7tech.server.migration;

import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.server.communityschemas.SchemaEntryManager;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.GlobalResourceInfo;

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
public class SchemaEntryPropertyResolver extends AbstractPropertyResolver {
    private static final Logger logger = Logger.getLogger(SchemaEntryPropertyResolver.class.getName());

    private SchemaEntryManager schemaManager;

    public SchemaEntryPropertyResolver(PropertyResolverFactory factory, Type type, SchemaEntryManager manager) {
        super(factory, type);
        this.schemaManager = manager;
    }

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
                for (SchemaEntry schema : schemaManager.findByName(schemaName)) {
                    ExternalEntityHeader schemaHeader = EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(schema));
                    schemaHeader.setValueMapping(valueMappingType, valueType, schemaName);
                    result.put(schemaHeader, Collections.singleton(new MigrationDependency(source, schemaHeader, propertyName, getType(), mappingType, exported)));
                }
            }
            return result;
        } catch (Exception e) {
            throw new PropertyResolverException("Error retrieving schema entries for: " + propertyName, e);
        }
    }

    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {

        if (! (targetValue instanceof SchemaEntry))
            throw new PropertyResolverException("SchemaEntry target value expected, got: " + (targetValue != null ? targetValue.getClass() : null));

        logger.log(Level.FINEST, "Applying mapping for {0}.", sourceEntity);

        try {
            String schemName = ((SchemaEntry)targetValue).getName();
            Method method = MigrationUtils.setterForPropertyName(sourceEntity, propName, AssertionResourceInfo.class);
            GlobalResourceInfo gri = new GlobalResourceInfo();
            gri.setId(schemName);
            method.invoke(sourceEntity, gri);
        } catch (Exception e) {
            throw new PropertyResolverException("Error applying mapping for property name: " + propName, e);
        }

    }
}
