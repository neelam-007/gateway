package com.l7tech.policy.assertion.ext.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Utility class for accessing {@link CustomReferenceEntitiesSupport} fields and methods.
 * <p/>
 * Typically used during policy migration (both policy import and export).
 */
public final class CustomEntityReferenceSupportAccessor {

    /**
     * Get a read-only collection of all referenced entities for the specified support object.
     */
    @NotNull
    public static Collection<CustomReferenceEntitiesSupport.ReferenceElement> getAllReferencedEntities(
            @NotNull final CustomReferenceEntitiesSupport referenceSupport
    ) {
        return Collections.unmodifiableCollection(referenceSupport.references.values());
    }

    /**
     * For the specified support object, get a read-only map of all referenced entities and their corresponding attribute names.
     */
    @NotNull
    public static Map<String, CustomReferenceEntitiesSupport.ReferenceElement> getAllReferencedEntitiesMap(
            @NotNull final CustomReferenceEntitiesSupport referenceSupport
    ) {
        return Collections.unmodifiableMap(referenceSupport.references);
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
     *
     * @see com.l7tech.policy.assertion.ext.entity.CustomReferenceEntitiesSupport.ReferenceElement#getId()
     */
    @NotNull
    public static String getEntityId(final Object entityRef) {
        return extractEntityObject(entityRef).getId();
    }

    /**
     * Sets specified referenced entity Id.
     *
     * @see com.l7tech.policy.assertion.ext.entity.CustomReferenceEntitiesSupport.ReferenceElement#setId(String)
     */
    public static void setEntityId(final Object entityRef, @NotNull final String newId) {
        extractEntityObject(entityRef).setId(newId);
    }

    /**
     * Extract specified referenced custom-key-value-store entity key-prefix.
     *
     * @see com.l7tech.policy.assertion.ext.entity.CustomReferenceEntitiesSupport.ReferenceElement#getKeyPrefix()
     */
    @Nullable
    public static String getEntityKeyPrefix(final Object entityRef) {
        return extractEntityObject(entityRef).getKeyPrefix();
    }

    /**
     * Extract specified referenced entity type.
     *
     * @see com.l7tech.policy.assertion.ext.entity.CustomReferenceEntitiesSupport.ReferenceElement#getType()
     * @see CustomEntityType
     */
    @NotNull
    public static String getEntityType(final Object entityRef) {
        return extractEntityObject(entityRef).getType();
    }

    /**
     * Extract specified referenced entity serializer object.
     *
     * @see com.l7tech.policy.assertion.ext.entity.CustomReferenceEntitiesSupport.ReferenceElement#getSerializerClassName()
     * @see CustomEntitySerializer
     */
    @Nullable
    public static String getSerializerClassName(final Object entityRef) {
        return extractEntityObject(entityRef).getSerializerClassName();
    }
}
