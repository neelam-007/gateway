package com.l7tech.policy.assertion.ext.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Utility class for accessing {@link CustomReferenceEntitiesSupport} fields and methods.
 * <p/>
 * Typically used during policy migration (both policy import and export).
 */
public final class CustomEntityReferenceSupportAccessor {

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
     * Convert specified referenced entity type, as string, into {@link CustomEntityType} enum.
     *
     * @return corresponding {@code CustomEntityType} or {@code null} if specified entity contains unrecognized type.
     * @see CustomEntityType
     */
    @Nullable
    public static CustomEntityType getEntityType(final Object entityRef) {
        final String entityTypeString = extractEntityObject(entityRef).getType();
        try {
            return entityTypeString != null ? CustomEntityType.valueOf(entityTypeString) : null;
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extract specified referenced entity serializer object.
     * @see CustomEntitySerializer
     */
    @Nullable
    public static String getSerializerClassName(final Object entityRef) {
        return extractEntityObject(entityRef).getSerializerClassName();
    }
}
