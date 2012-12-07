package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Assertion bean representing an invocation of server behavior for an EncapsulatedAssertionConfig.
 */
public class EncapsulatedAssertion extends Assertion implements UsesEntities, UsesVariables, SetsVariables {
    private static final String DEFAULT_OID_STR = Long.toString(EncapsulatedAssertionConfig.DEFAULT_OID);

    private DefaultAssertionMetadata meta = null;
    private String encapsulatedAssertionConfigId = DEFAULT_OID_STR;

    private transient EncapsulatedAssertionConfig encapsulatedAssertionConfig;

    public EncapsulatedAssertion() {
    }

    public EncapsulatedAssertion(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
        config(encapsulatedAssertionConfig);
    }

    public EncapsulatedAssertionConfig config() {
        return encapsulatedAssertionConfig;
    }

    public void config(EncapsulatedAssertionConfig config) {
        this.encapsulatedAssertionConfig = config;
        if (config != null)
            this.encapsulatedAssertionConfigId = config.getId();
        meta = null; // Force metadata to be rebuilt
    }

    public String getEncapsulatedAssertionConfigId() {
        return encapsulatedAssertionConfigId;
    }

    public void setEncapsulatedAssertionConfigId(String encapsulatedAssertionConfigId) {
        this.encapsulatedAssertionConfigId = encapsulatedAssertionConfigId;
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        return encapsulatedAssertionConfigId == null || DEFAULT_OID_STR.equals(encapsulatedAssertionConfigId)
            ? new EntityHeader[0]
            : new EntityHeader[] { new EntityHeader(encapsulatedAssertionConfigId, EntityType.ENCAPSULATED_ASSERTION, null, null) };
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (newEntityHeader != null && EntityType.ENCAPSULATED_ASSERTION.equals(newEntityHeader.getType())) {
            encapsulatedAssertionConfigId = newEntityHeader.getStrId();
        }
    }

    @Override
    public AssertionMetadata meta() {
        if (this.meta != null)
            return this.meta;

        // Avoid using super.defaultMeta() because this assertion uses different metadata for each instance
        DefaultAssertionMetadata meta = new DefaultAssertionMetadata(this);

        EncapsulatedAssertionConfig config = this.encapsulatedAssertionConfig != null
            ? this.encapsulatedAssertionConfig
            : new EncapsulatedAssertionConfig();

        meta.put(SHORT_NAME, config.getName());
        meta.put(PALETTE_NODE_ICON, findIconResourcePath(config));
        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.console.tree.policy.EncapsulatedAssertionPolicyNode");

        meta.put(ASSERTION_FACTORY, new Functions.Unary< EncapsulatedAssertion, EncapsulatedAssertion >() {
            @Override
            public EncapsulatedAssertion call(EncapsulatedAssertion encapsulatedAssertion) {
                EncapsulatedAssertionConfig config = encapsulatedAssertion.config();
                if (config == null)
                    return new EncapsulatedAssertion();
                return new EncapsulatedAssertion(config.getReadOnlyCopy());
            }
        });

        // Copy over properties that require some adaptation
        meta.put(PALETTE_FOLDERS, new String[] { config.getProperty(EncapsulatedAssertionConfig.PROP_PALETTE_FOLDER) });

        this.meta = meta;
        return meta;
    }

    private String findIconResourcePath(EncapsulatedAssertionConfig config) {
        String filename = config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_RESOURCE_FILENAME);
        if (filename != null)
            return EncapsulatedAssertionConfig.ICON_RESOURCE_DIRECTORY + filename;

        // TODO support custom base64 image bytes
        return EncapsulatedAssertionConfig.ICON_RESOURCE_DIRECTORY + EncapsulatedAssertionConfig.DEFAULT_ICON_RESOURCE_FILENAME;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        // TODO get from config
        return new VariableMetadata[0];
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        // TODO get from config
        return new String[0];
    }
}
