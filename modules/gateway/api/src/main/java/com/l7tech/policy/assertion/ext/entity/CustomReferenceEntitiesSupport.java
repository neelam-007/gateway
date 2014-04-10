package com.l7tech.policy.assertion.ext.entity;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@link CustomReferenceEntities} support class.<br/>
 * Use this class to identify referenced entities using attribute names (e.g. connectionKey), as shown below:
 * <blockquote><pre>
 *     {@code
 *     // get connection passwordId entity
 *     entitiesSupport.getReference("connectionPasswordId");
 *     // set connection passwordId entity
 *     entitiesSupport.setReference("connectionPasswordId", connectionPasswordId ... );
 *     }
 * </pre></blockquote>
 */
public final class CustomReferenceEntitiesSupport implements Serializable {
    private static final long serialVersionUID = -2840511468657076023L;

    /**
     * Get entity id/key, identified with {@code attributeName}.
     *
     * @param attributeName    referenced entity attribute name.
     * @return referenced entity unique identifier or {@code null} if there are no entities identified with the
     * {@code attributeName}
     */
    public String getReference(final String attributeName) {
        if (attributeName == null) throw new IllegalArgumentException("attributeName cannot be null");
        final ReferenceElement element = references.get(attributeName);
        return element != null ? element.getId() : null;
    }

    /**
     * Set an entity reference using only id.
     * Typically used for entity not depending on other entities, like Secure Password.
     *
     * @param attributeName    Mandatory.  Use this unique {@code attributeName} for accessing the referenced entity.
     *                         Must be unique for each entity. Consider the {@code attributeName} as a local variable name.
     *                         field name for this class.
     * @param id               Mandatory.  Entity unique identifier.
     * @param type             Mandatory.  Entity type.
     */
    public void setReference(final String attributeName, final String id, final CustomEntityType type) {
        if (attributeName == null) throw new IllegalArgumentException("attributeName cannot be null");
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (type == null) throw new IllegalArgumentException("type cannot be null");

        references.put(attributeName, new ReferenceElement(id, null, type, null));
    }

    /**
     * Set an key-value-store entity reference.
     *
     * @param attributeName       Mandatory. Use this unique {@code attributeName} for accessing the referenced entity.
     *                            Must be unique for each entity. Consider the {@code attributeName} as a local variable name.
     * @param key                 Mandatory. Represents a unique identifier for the entity in the custom-key-value-store.
     * @param keyPrefix           Mandatory. Represents the prefix portion of the key in the key-value-store.
     * @param entitySerializer    Optional. Represents entity serializer object in case when the entity depends on other
     *                            entities or when providing specific import implementation i.e. when the entity is of type
     *                            {@link CustomEntityDescriptor}.
     */
    public void setKeyValueStoreReference(
            final String attributeName,
            final String key,
            final String keyPrefix,
            final CustomEntitySerializer entitySerializer
    ) {
        if (attributeName == null) throw new IllegalArgumentException("attributeName cannot be null");
        if (key == null) throw new IllegalArgumentException("key cannot be null");
        if (keyPrefix == null) throw new IllegalArgumentException("keyPrefix cannot be null for custom-key-values");

        references.put(attributeName, new ReferenceElement(key, keyPrefix, CustomEntityType.KeyValueStore, entitySerializer));
    }

    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Entity Reference container.
     */
    final Map<String, ReferenceElement> references = new TreeMap<String, ReferenceElement>();

    /**
     * Entity Reference internal container class
     */
    final static class ReferenceElement implements Serializable {
        private static final long serialVersionUID = -1519739648961747529L;

        /**
         * Mutable. Specifies the entity id/key. Mandatory and cannot be {@code null}.<br/>
         * The <tt>id</tt> can be changed during import, if the user decides to use another entity.
         */
        private String id;

        /**
         * Immutable. Specifies the entity id/key prefix, if applicable.
         * Optional, except for entities of type {@link CustomEntityType#KeyValueStore}<br/>
         * For instance entities of type {@link CustomEntityType#KeyValueStore} define their key's as
         * <i>prefix</i>+<i>keyId</i>, therefore the <tt>keyPrefix</tt> is needed to extract the rest of entities
         * of the same type i.e. gather all SalesForce connections from the
         * {@link com.l7tech.policy.assertion.ext.store.KeyValueStore KeyValueStore}.
         *
         * @see com.l7tech.policy.assertion.ext.store.KeyValueStore KeyValueStore
         */
        private final String keyPrefix;

        /**
         * Immutable. Specifies the entity {@link com.l7tech.policy.assertion.ext.entity.CustomEntityType type}. Mandatory and cannot be {@code null}
         */
        private final CustomEntityType type;

        /**
         * Immutable. Entity serializer object in order to get entity object instance.
         * Applicable for entities dependent on other entities, so having {@code null} means this entity doesn't
         * have any dependent references e.g. it's a reference to a password.
         */
        private final CustomEntitySerializer entitySerializer;

        /**
         * Constructor
         * @param id                  entity id/key.
         * @param keyPrefix            entity id/key prefix, if applicable.
         * @param type                entity {@link com.l7tech.policy.assertion.ext.entity.CustomEntityType type}.
         * @param entitySerializer    entity serializer.
         */
        private ReferenceElement(
                final String id,
                final String keyPrefix,
                final CustomEntityType type,
                final CustomEntitySerializer entitySerializer
        ) {
            this.id = id;
            this.keyPrefix = keyPrefix;
            this.type = type;
            this.entitySerializer = entitySerializer;
        }

        /**
         * Getter for {@link #id}
         */
        public String getId() { return id; }

        /**
         * Setter for {@link #id}.<br/>
         * Setter is disabled for access outside the package.
         */
        void setId(final String id) {
            if (id == null) throw new IllegalArgumentException("id cannot be null");
            this.id = id;
        }

        /**
         * Getter for {@link #keyPrefix}
         */
        public String getKeyPrefix() { return keyPrefix; }

        /**
         * Getter for {@link #type}
         */
        public CustomEntityType getType() { return type; }

        /**
         * Getter for {@link #entitySerializer}
         */
        public CustomEntitySerializer getSerializer() { return entitySerializer; }
    }
}
