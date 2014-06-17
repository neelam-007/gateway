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
     * @throws IllegalArgumentException when {@code attributeName} is {@code null}
     */
    public String getReference(final String attributeName) {
        if (attributeName == null) { throw new IllegalArgumentException("attributeName cannot be null"); }
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
     * @throws IllegalArgumentException when {@code attributeName} or {@code id} is {@code null}
     */
    public void setReference(final String attributeName, final String id, final CustomEntityType type) {
        if (attributeName == null) { throw new IllegalArgumentException("attributeName cannot be null"); }
        if (id == null) { throw new IllegalArgumentException("id cannot be null"); }

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
     * @throws IllegalArgumentException when {@code attributeName} or {@code key} or {@code keyPrefix} is {@code null}
     */
    public void setKeyValueStoreReference(
            final String attributeName,
            final String key,
            final String keyPrefix,
            final CustomEntitySerializer entitySerializer
    ) {
        if (attributeName == null) { throw new IllegalArgumentException("attributeName cannot be null"); }
        if (key == null) { throw new IllegalArgumentException("key cannot be null"); }
        if (keyPrefix == null) { throw new IllegalArgumentException("keyPrefix cannot be null for custom-key-values"); }

        references.put(attributeName, new ReferenceElement(key, keyPrefix, CustomEntityType.KeyValueStore, entitySerializer));
    }

    /**
     * Remove entity reference identified with the specified {@code attributeName}
     *
     * @param attributeName    Mandatory. Use this unique {@code attributeName} for accessing the referenced entity.
     * @return {@code true} if there was entity associated with {@code attributeName}, or {@code false} otherwise.
     * @throws IllegalArgumentException when {@code attributeName} is {@code null}
     */
    public boolean removeReference(final String attributeName) {
        if (attributeName == null) { throw new IllegalArgumentException("attributeName cannot be null"); }
        return references.remove(attributeName) != null;
    }

    /**
     * @return {@code true} if both {@link #references} containers are equal, {@code false} otherwise.
     *
     * @see TreeMap#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof CustomReferenceEntitiesSupport && references.equals(((CustomReferenceEntitiesSupport) obj).references);
    }

    /**
     * @return {@link #references} container {@code hashCode}
     * @see java.util.TreeMap#hashCode()
     */
    @Override
    public int hashCode() {
        return references.hashCode();
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
         * Immutable. Specifies string representation of the entity {@link CustomEntityType type}.
         * Mandatory and cannot be {@code null}
         *
         * @see CustomEntityType
         */
        private final String type;

        /**
         * Immutable. Entity serializer, represented by it's classname, used to get entity object instance.
         * Applicable for entities dependent on other entities, so having {@code null} means this entity doesn't
         * have any dependent references e.g. it's a reference to a password.
         */
        private final String entitySerializerClassName;

        /**
         * Constructor
         * @param id                  entity id/key.
         * @param keyPrefix           entity id/key prefix, if applicable.
         * @param type                entity {@link com.l7tech.policy.assertion.ext.entity.CustomEntityType type}.
         * @param entitySerializer    entity serializer.
         * @throws IllegalArgumentException when {@code type} is {@code null}
         */
        private ReferenceElement(
                final String id,
                final String keyPrefix,
                final CustomEntityType type,
                final CustomEntitySerializer entitySerializer
        ) {
            if (type == null) { throw new IllegalArgumentException("type cannot be null"); }

            this.id = id;
            this.keyPrefix = keyPrefix;
            this.type = type.name();
            this.entitySerializerClassName = entitySerializer != null ? entitySerializer.getClass().getName() : null;
        }

        /**
         * Getter for {@link #id}
         */
        public String getId() { return id; }

        /**
         * Setter for {@link #id}.<br/>
         * Setter is disabled for access outside the package.
         *
         * @throws IllegalArgumentException when {@code id} is {@code null}
         */
        void setId(final String id) {
            if (id == null) { throw new IllegalArgumentException("id cannot be null"); }
            this.id = id;
        }

        /**
         * Getter for {@link #keyPrefix}
         */
        public String getKeyPrefix() { return keyPrefix; }

        /**
         * Getter for {@link #type}
         */
        public String getType() { return type; }

        /**
         * Getter for {@link #entitySerializerClassName}
         */
        public String getSerializerClassName() { return entitySerializerClassName; }

        /**
         * Compares this {@link ReferenceElement} to the specified {@code object}.<br/>
         * The result is {@code true} if and only if the argument is not {@code null} and is a {@code ReferenceElement}
         * object that has the same {@link #id}, {@link #keyPrefix}, {@link #type} and {@link #entitySerializerClassName}
         * as this object.
         */
        @Override
        public boolean equals(final Object object) {
            if (this == object) { return true; }
            if (object instanceof ReferenceElement) {
                final ReferenceElement other = (ReferenceElement)object;
                return id.equals(other.id) // mandatory
                        && (keyPrefix != null ? keyPrefix.equals(other.keyPrefix) : other.keyPrefix == null) // optional
                        && type.equals(other.type) // mandatory
                        && (entitySerializerClassName != null ? entitySerializerClassName.equals(other.entitySerializerClassName) : other.entitySerializerClassName == null); // optional
            }
            return false;
        }

        /**
         * Returns a hash code for this {@link ReferenceElement}.<br/>
         * The hash code for a {@code ReferenceElement} object is computed as:
         * <blockquote><pre>
         * id_hash + keyPrefix_hash*31^(1) + type_hash*31^(2) + entitySerializerClassName_hash*31^(3)
         * </pre></blockquote>
         * using {@code int} arithmetic, where {@code id_hash}, {@code keyPrefix_hash}, {@code type_hash} and {@code entitySerializerClassName_hash}
         * represent this object {@link #id}, {@link #keyPrefix}, {@link #type} and {@link #entitySerializerClassName} hash codes,
         * and {@code ^} indicates exponentiation.
         */
        @Override
        public int hashCode() {
            int result = id.hashCode(); // mandatory
            result = 31 * result + (keyPrefix != null ? keyPrefix.hashCode() : 0);  // optional
            result = 31 * result + type.hashCode(); // mandatory
            result = 31 * result + (entitySerializerClassName != null ? entitySerializerClassName.hashCode() : 0); // optional
            return result;
        }
    }
}
