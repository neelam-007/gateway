package com.l7tech.gateway.common.entity;

import com.l7tech.gateway.common.custom.ClassNameToEntitySerializer;
import com.l7tech.gateway.common.custom.CustomEntitiesResolver;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for resolving external entities used by the specified assertion, using
 * {@link #getEntitiesUsed(com.l7tech.policy.assertion.Assertion)} and
 * {@link #replaceEntity(com.l7tech.policy.assertion.Assertion, com.l7tech.objectmodel.EntityHeader, com.l7tech.objectmodel.EntityHeader)}
 * methods.<br/>
 * The Assertion can be a modular assertion implementing {@link com.l7tech.policy.assertion.UsesEntities UsesEntities}
 * or a custom assertion implementing {@link com.l7tech.policy.assertion.ext.entity.CustomReferenceEntities}.<br/>
 * <p/>
 * Typically used during policy migration (both policy import and export).
 *
 * @see CustomEntitiesResolver
 */
public class EntitiesResolver {

    @Nullable
    private final CustomEntitiesResolver entitiesResolver;

    /**
     * Construct the EntitiesResolver using the {@link Builder} object
     */
    private EntitiesResolver(@NotNull final Builder builder) {
        this.entitiesResolver = (builder.keyValueStore != null)
                ? new CustomEntitiesResolver(
                        builder.keyValueStore,
                        builder.classNameToSerializer   // let it throw
                                                        // if keyValueStore is not null,
                                                        // then classNameToSerializer mustn't be null
                    )
                : null;
    }

    /**
     * Create the {@link Builder} object
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Used by the {@link com.l7tech.server.policy.validator.ServerPolicyValidator ServerPolicyValidator},
     * to report unsupported entity type.<br/>
     *
     * @param assertion             the {@code Assertion} object, must either be a custom assertion
     *                              (i.e. implementing {@link CustomAssertionHolder}) or a modular assertion
     *                              implementing {@link UsesEntities}.
     * @param type                  the {@code EntityType} object.
     * @param usesEntitiesTypeFn    function object for extracting entity type-name for modular assertions.
     * @return a user-friendly string describing the entity type-name.  Never {@code null}
     * @throws IllegalArgumentException if the specified {@code assertion} object is not a custom assertion
     * (i.e. implementing {@link CustomAssertionHolder}) or a modular assertion implementing {@link UsesEntities}
     */
    @SuppressWarnings("JavadocReference")
    @NotNull
    public static String getEntityTypeName(
            final Assertion assertion,
            @NotNull final EntityType type,
            @NotNull final Functions.Binary<String, UsesEntities, EntityType> usesEntitiesTypeFn
    ) {
        if (assertion instanceof CustomAssertionHolder) {
            return type.getName();
        } else if (assertion instanceof UsesEntities) {
            return usesEntitiesTypeFn.call((UsesEntities)assertion, type);
        }
        throw new IllegalArgumentException("Unsupported assertion type. Assertion must be superclass of either UsesEntities or CustomAssertionHolder");
    }

    /**
     * Extract entity dependencies for the specified <tt>assertion</tt>, which can also be a custom assertion,
     * in which case {@link #entitiesResolver} is used.
     *
     * @param assertion    assertion to extract entity dependencies {@link com.l7tech.policy.assertion.CustomAssertionHolder}, otherwise ignored.
     * @return a list of {@link EntityHeader}'s containing the <tt>assertion</tt> entity dependencies,
     * or an empty array when the <tt>assertion</tt> doesn't contain any dependencies or when the assertion is of type
     * {@link com.l7tech.policy.assertion.CustomAssertionHolder} and {@link #entitiesResolver} is not specified.
     */
    @NotNull
    public EntityHeader[] getEntitiesUsed (@Nullable final Assertion assertion) {
        if (assertion instanceof CustomAssertionHolder && entitiesResolver != null) {
            return entitiesResolver.getEntitiesUsed((CustomAssertionHolder)assertion);
        } else if (assertion instanceof UsesEntities) {
            return ((UsesEntities)assertion).getEntitiesUsed();
        }
        return new EntityHeader[0];
    }

    /**
     * For specified {@code assertion}, replace dependent entity, {@code oldEntity}, with {@code newEntity}.
     *
     * @param assertion    the {@code Assertion} object, must either be a custom assertion
     *                     (i.e. implementing {@link CustomAssertionHolder}) or a modular assertion
     *                     implementing {@link UsesEntities}.
     * @param oldEntity    old entity object
     * @param newEntity    new entity object
     */
    public void replaceEntity(
            @Nullable Assertion assertion,
            @NotNull EntityHeader oldEntity,
            @NotNull EntityHeader newEntity
    ) {
        if (assertion instanceof CustomAssertionHolder && entitiesResolver != null) {
            entitiesResolver.replaceEntity(oldEntity, newEntity, (CustomAssertionHolder) assertion);
        } else if (assertion instanceof UsesEntities) {
            ((UsesEntities)assertion).replaceEntity(oldEntity, newEntity);
        }
    }


    /**
     * For now we only expose KeyValueStore for Custom Assertions.
     * Use this builder to add more dependencies
     */
    public static class Builder {
        @Nullable
        private KeyValueStore keyValueStore;

        @Nullable
        private ClassNameToEntitySerializer classNameToSerializer;

        private Builder() {
        }

        public Builder keyValueStore(@Nullable final KeyValueStore keyValueStore) {
            this.keyValueStore = keyValueStore;
            return this;
        }

        public Builder classNameToSerializer(@Nullable final ClassNameToEntitySerializer classNameToSerializer) {
            this.classNameToSerializer = classNameToSerializer;
            return this;
        }

        public EntitiesResolver build() {
            return new EntitiesResolver(this);
        }
    }
}
