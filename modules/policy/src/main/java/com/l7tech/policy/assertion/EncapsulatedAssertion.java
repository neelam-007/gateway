package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Assertion bean representing an invocation of server behavior for an EncapsulatedAssertionConfig.
 */
public class EncapsulatedAssertion extends MessageTargetableAssertion implements UsesEntities, UsesVariables, SetsVariables {
    private static final String DEFAULT_OID_STR = Long.toString(EncapsulatedAssertionConfig.DEFAULT_OID);
    private static final String META_INIT = EncapsulatedAssertion.class.getName() + ".metadataInitialized";

    private String encapsulatedAssertionConfigId = DEFAULT_OID_STR;

    private transient EncapsulatedAssertionConfig encapsulatedAssertionConfig;

    public EncapsulatedAssertionConfig config() {
        return encapsulatedAssertionConfig;
    }

    public void config(EncapsulatedAssertionConfig config) {
        this.encapsulatedAssertionConfig = config;
        // Force metadata to be rebuilt on next call to meta()
        defaultMeta().putNull(META_INIT);
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
        DefaultAssertionMetadata meta = defaultMeta();
        if (meta.get(META_INIT) != null)
            return meta;

        EncapsulatedAssertionConfig config = this.encapsulatedAssertionConfig != null
            ? this.encapsulatedAssertionConfig
            : new EncapsulatedAssertionConfig();

        // Copy over simple string-valued properties
        meta.put(BASE_NAME, config.getProperty(EncapsulatedAssertionConfig.PROP_META_BASE_NAME));
        meta.put(PALETTE_NODE_NAME, config.getProperty(EncapsulatedAssertionConfig.PROP_META_PALETTE_NODE_NAME));
        meta.put(PALETTE_NODE_ICON, config.getProperty(EncapsulatedAssertionConfig.PROP_META_PALETTE_NODE_ICON));

        // Copy over properties that require some adaptation
        meta.put(PALETTE_FOLDERS, new String[] { config.getProperty(EncapsulatedAssertionConfig.PROP_PALETTE_FOLDER) });

        meta.put(META_INIT, Boolean.TRUE);
        return meta;
    }
}
