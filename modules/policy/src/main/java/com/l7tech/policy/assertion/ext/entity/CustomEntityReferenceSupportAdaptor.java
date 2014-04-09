package com.l7tech.policy.assertion.ext.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Utility class for accessing {@link CustomReferenceEntitiesSupport} fields and methods
 */
public final class CustomEntityReferenceSupportAdaptor {

    /**
     * Get a read-only collection of all referenced entities in the specified support object.
     */
    @NotNull
    public static Collection<CustomReferenceEntitiesSupport.ReferenceElement> getAllReferencedEntities(
            @NotNull final CustomReferenceEntitiesSupport referenceSupport
    ) {
        return Collections.unmodifiableCollection(referenceSupport.references.values());
    }

    /**
     * Performs a simple type-cast and throws if the specified {@code entityRef} object is of invalid type.
     * @see com.l7tech.policy.assertion.ext.entity.CustomReferenceEntitiesSupport.ReferenceElement
     */
    @NotNull
    private static CustomReferenceEntitiesSupport.ReferenceElement extractEntityObject(final Object entityRef) {
        if (!(entityRef instanceof CustomReferenceEntitiesSupport.ReferenceElement)) {
            throw new IllegalArgumentException(
                    "Illegal entity reference type, should be \"" +
                            CustomReferenceEntitiesSupport.ReferenceElement.class.getName() + "\""
            );
        }
        return ((CustomReferenceEntitiesSupport.ReferenceElement) entityRef);
    }

    /**
     * Extract specified referenced entity Id.
     */
    @NotNull
    public static String getEntityId(final Object entityRef) {
        return extractEntityObject(entityRef).getId();
    }

    /**
     * Sets specified referenced entity Id.
     */
    public static void setEntityId(final Object entityRef, @NotNull final String newId) {
        extractEntityObject(entityRef).setId(newId);
    }

    /**
     * Extract specified referenced entity custom-key-value-store entity key-prefix.
     */
    @NotNull
    public static String getEntityKeyPrefix(final Object entityRef) {
        final String entityIdPrefix = extractEntityObject(entityRef).getKeyPrefix();
        if (entityIdPrefix == null) {
            throw new IllegalArgumentException(
                    "Referenced entity with id: \"" + getEntityId(entityRef) +
                            "\" type: \"" + getEntityType(entityRef) +
                            "\" doesn't provide any prefix!"
            );
        }
        return entityIdPrefix;
    }

    /**
     * Extract specified referenced entity type.
     * @see CustomEntityType
     */
    @NotNull
    public static CustomEntityType getEntityType(final Object entityRef) {
        return extractEntityObject(entityRef).getType();
    }

    /**
     * Extract specified referenced entity serializer object.
     * @see CustomEntitySerializer
     */
    @Nullable
    public static CustomEntitySerializer getEntitySerializer(final Object entityRef) {
        return extractEntityObject(entityRef).getSerializer();
    }
}
