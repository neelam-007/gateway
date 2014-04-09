package com.l7tech.objectmodel;

import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is entity header impl for a custom-key-value-store entity.
 */
public class CustomKeyStoreEntityHeader extends EntityHeader {
    private static final long serialVersionUID = 528163664043260635L;

    /**
     * Mandatory.  Represents the prefix portion of the key in the custom-key-value-store.<br/>
     * The prefix is used to identify all entities belonging to the Assertion.
     * For example: com.l7tech.custom.salesforce.partner.v26.assertion.SalesForceConnection.
     */
    @NotNull
    private final String entityKeyPrefix;

    /**
     * Getter for {@link #entityKeyPrefix}
     */
    @NotNull
    public String getEntityKeyPrefix() { return entityKeyPrefix; }

    /**
     * Mandatory.  Represents the base64 encoded entity bytes. This is the custom-key-value-store value.
     */
    @Nullable
    private final String entityBase64Value;

    /**
     * Getter for {@link #entityBase64Value}
     */
    @Nullable
    public String getEntityBase64Value() { return entityBase64Value; }

    /**
     * Optional. Represents the custom-key-value-store entity serializer class name.<br/>
     * If the assertion developer decides to implement custom summery and/or create dialogs, by implementing
     * {@link com.l7tech.policy.assertion.ext.entity.CustomEntityDescriptor CustomEntityDescriptor} interface,
     * then we need the serializer to get the external entity object from the bytes.
     */
    @Nullable
    private final String entitySerializer;

    /**
     * Getter for {@link #entitySerializer}
     */
    @Nullable
    public String getEntitySerializer() { return entitySerializer; }

    /**
     * Used during policy export, to make sure that custom-key-value-store entity, which is about to be exported,
     * can be found in the source Gateway.<br/>
     * We have to enforce that external entities must exist in the source Gateway prior export,
     * otherwise, for one we cannot determine if this entity depends on another, therefore the export will be incomplete,
     * and second we cannot re-create the missing entity on the target Gateway without the bytes.
     *
     * @return {@code true} if the header contains any bytes
     */
    public boolean hasBytes() {
        return getEntityBase64Value() != null;
    }

    /**
     * Default constructor.
     *
     * @param entityKey          external entity id, prefix+name
     * @param entityKeyPrefix    external entity prefix
     * @param entityBytes        external entity row-bytes
     * @param entitySerializer   external entity serializer class name
     */
    public CustomKeyStoreEntityHeader(
            @NotNull final String entityKey,
            @NotNull final String entityKeyPrefix,
            @Nullable final byte[] entityBytes,
            @Nullable final String entitySerializer
    ) {
        setType(EntityType.CUSTOM_KEY_VALUE_STORE);

        // for custom-key-value-store we do not use the goid, instead we use the name as entity id.
        setName(entityKey);

        this.entityKeyPrefix = entityKeyPrefix;
        this.entityBase64Value = entityBytes != null ? HexUtils.encodeBase64(entityBytes, true) : null;
        this.entitySerializer = entitySerializer;
    }

    @Override
    public String toString() {
        return "CustomKeyStoreEntityHeader [" +
                "name: \"" + getName() +
                "\", entityKeyPrefix: \"" + getEntityKeyPrefix() +
                "\", entityBase64Value: \"" + getEntityBase64Value() +
                "\", hasBytes: \"" + String.valueOf(hasBytes()) +
                "\", entitySerializer: " + getEntitySerializer() +
                "]";
    }
}
