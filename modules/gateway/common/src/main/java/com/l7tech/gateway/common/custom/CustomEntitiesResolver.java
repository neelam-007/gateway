package com.l7tech.gateway.common.custom;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ReferenceEntityBytesException;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.entity.*;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreException;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * External entities resolver for Custom Assertions.<br/>
 * Provides means to extract (using {@link #getEntitiesUsed(com.l7tech.policy.assertion.CustomAssertionHolder)}) and
 * modify (using {@link #replaceEntity(com.l7tech.objectmodel.EntityHeader, com.l7tech.objectmodel.EntityHeader, com.l7tech.policy.assertion.CustomAssertionHolder)}
 * Custom Assertion external entities, similar as {@code UsesEntities} for modular assertions.
 *
 * @see com.l7tech.policy.assertion.UsesEntities
 */
public class CustomEntitiesResolver {
    private static final Logger logger = Logger.getLogger(CustomEntitiesResolver.class.getName());

    /**
     * Access to CustomKeyValueStore entity.
     */
    @NotNull
    private final KeyValueStore keyValueStore;

    /**
     * Default constructor.
     */
    public CustomEntitiesResolver(@NotNull final KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    /**
     * Returns a list of {@link EntityHeader}'s containing all external entities used by the specified custom assertion.
     */
    @NotNull
    public EntityHeader[] getEntitiesUsed(@NotNull CustomAssertionHolder assertionHolder) {
        final CustomAssertion customAssertion = assertionHolder.getCustomAssertion();
        if (customAssertion == null) {
            throw new IllegalStateException("CustomAssertion missing from assertionHolder");
        }

        final Collection<EntityHeader> entityHeaders = createSortedEntitiesList();
        if (customAssertion instanceof CustomReferenceEntities) {
            processEntityReference(entityHeaders, (CustomReferenceEntities)customAssertion);
        }

        return (entityHeaders.toArray(new EntityHeader[entityHeaders.size()]));
    }

    /**
     * Utility function for creating an ordered list of {@link EntityHeader}'s based on {@link EntityType}.
     */
    private Collection<EntityHeader> createSortedEntitiesList() {
        //noinspection serial
        class TmpSortedArrayList extends ArrayList<EntityHeader> {
            private final Comparator<EntityHeader> comparator;

            public TmpSortedArrayList(@NotNull final Comparator<EntityHeader> comparator) {
                super();
                this.comparator = comparator;
            }

            @Override
            public boolean add(final EntityHeader value) {
                int insertionPoint = Collections.binarySearch(this, value, comparator);
                super.add((insertionPoint > -1) ? insertionPoint : (-insertionPoint) - 1, value);
                return true;
            }
        }

        return new TmpSortedArrayList(
                new Comparator<EntityHeader>() {
                    @Override
                    public int compare(final EntityHeader o1, final EntityHeader o2) {
                        final int ordinal1 = entityTypeToOrdinal(o1.getType()),
                                ordinal2 = entityTypeToOrdinal(o2.getType());
                        return (ordinal1 < ordinal2 ? -1 : (ordinal1 == ordinal2 ? 0 : 1));
                    }

                    private int entityTypeToOrdinal(@NotNull final EntityType type) {
                        switch (type) {
                            case SECURE_PASSWORD:
                                return 1;
                            case CUSTOM_KEY_VALUE_STORE:
                                return 0;
                        }
                        return 2;
                    }
                }
        );
    }

    /**
     * Recursively go through all entities used by the custom assertion and insert them in the list.
     * <p/>
     * TODO: add extensive unit-testing here
     *
     * TODO: add support for future types here
     *
     * @param entityHeaders     self sorted list of entities found so far.
     * @param externalEntity    starting entity reference object.
     */
    private void processEntityReference(
            @NotNull final Collection<EntityHeader> entityHeaders,
            @NotNull final CustomReferenceEntities externalEntity
    ) {
        final CustomReferenceEntitiesSupport entityReferenceSupport = externalEntity.getReferenceEntitiesSupport();
        if (entityReferenceSupport != null) {
            // do getReferenceEntitiesSupport sanity-check, there should always be same instance
            if (entityReferenceSupport != externalEntity.getReferenceEntitiesSupport()) {
                throw new IllegalArgumentException("CustomReferenceEntities method getReferenceEntitiesSupport() returning non-singleton instance!");
            }
            // loop through all references
            for (final Object referenceObject : CustomEntityReferenceSupportAdaptor.getAllReferencedEntities(entityReferenceSupport)) {
                // gather entity id and type
                final String entityId = CustomEntityReferenceSupportAdaptor.getEntityId(referenceObject);
                final CustomEntityType entityType = CustomEntityReferenceSupportAdaptor.getEntityType(referenceObject);
                // check whether we've processed this reference already
                // should also guard against circular references
                if (findDuplicateReference(entityHeaders, entityType, entityId)) {
                    continue; // skip entity if this is duplicate i.e. already processed
                }
                // check entity type
                switch (entityType) {
                    case SecurePassword:
                        // add reference entity, will throw with RuntimeException when the password-id is not a valid GOID.
                        addPasswordEntity(entityHeaders, entityId);
                        break;
                    case KeyValueStore:
                        // get the serializer in order to go through any potential reference dependencies
                        final CustomEntitySerializer entitySerializer = CustomEntityReferenceSupportAdaptor.getEntitySerializer(referenceObject);
                        // get entity prefix, cannot be null so it will throw
                        final String entityKeyPrefix = CustomEntityReferenceSupportAdaptor.getEntityKeyPrefix(referenceObject);
                        // first add the reference entity, never throws
                        final Object entityObject = addCustomKeyValuedEntity(entityHeaders, entityId, entityKeyPrefix, entitySerializer);
                        // next process any potential child entity dependencies
                        if (entityObject instanceof CustomReferenceEntities) {
                            processEntityReference(entityHeaders, (CustomReferenceEntities)entityObject);
                        }
                        break;
                    default:
                        logger.warning("Unsupported custom reference EntityType: " + entityType);
                        break;
                }
            }
        }
    }

    /**
     * Check whether the specified <tt>referenceElement</tt> has already been processed, i.e. has been added to our entity headers list.<br/>
     * This function should also guard against circular references.
     * <p/>
     * TODO: add support for future types here
     *
     * @param entityHeaders    self sorted list of entities found so far.
     * @param entityType       entity type
     * @param entityId         entity id
     * @return {@code true} if the specified
     */
    private boolean findDuplicateReference(
            @NotNull final Collection<EntityHeader> entityHeaders,
            @NotNull final CustomEntityType entityType,
            @NotNull final String entityId
    ) {
        for (final EntityHeader entityHeader : entityHeaders) {
            // check for supported types only
            switch (entityHeader.getType()) {
                case SECURE_PASSWORD:
                    if (CustomEntityType.SecurePassword.equals(entityType) && entityId.equals(entityHeader.getStrId())) {
                        return true;
                    }
                    break;
                case CUSTOM_KEY_VALUE_STORE:
                    if (CustomEntityType.KeyValueStore.equals(entityType) && entityId.equals(entityHeader.getName())) {
                        return true;
                    }
                    break;
                default:
                    // Should never happen, so throw for now
                    throw new RuntimeException("Unsupported Header Type: \"" + entityHeader.getType() + "\"");
            }
        }

        return false;
    }

    /**
     * Utility function for creating a {@link CustomKeyStoreEntityHeader}, from the specified <tt>entityKey</tt>,
     * <tt>entityKeyPrefix</tt> and optional <tt>entitySerializer</tt>, adding the header to our <tt>entityHeaders</tt>
     * collection and finally returning the entity object associated with the specified <tt>entityKey</tt>,
     * in case when <tt>entitySerializer</tt> is specified.
     *
     * @param entityHeaders     self sorted list of entities found so far. Mandatory.
     * @param entityKey         the custom-key-value-store name. Mandatory.
     * @param entityKeyPrefix   the custom-key-value-store name prefix. Mandatory.
     * @param entitySerializer  entity serializer object. Optional.
     * @return the entity object, if we can serialize the reference id, or {@code null} otherwise.
     */
    @Nullable
    private Object addCustomKeyValuedEntity(
            @NotNull final Collection<EntityHeader> entityHeaders,
            @NotNull final String entityKey,
            @NotNull final String entityKeyPrefix,
            @Nullable final CustomEntitySerializer entitySerializer
    ) {
        Object entityObject = null;
        byte[] entityBytes = null;
        try {
            entityBytes = extractEntityBytes(entityKey, CustomEntityType.KeyValueStore);
            entityObject = (entityBytes != null && entitySerializer != null) ? entitySerializer.deserialize(entityBytes) : null;
        } catch (final Exception  e) {
            logger.log(Level.WARNING, "Failed to extract key-val-store bytes for id: \"" + entityKey + "\"", e);
        }
        entityHeaders.add(
                new CustomKeyStoreEntityHeader(
                        entityKey, // mandatory
                        entityKeyPrefix, // mandatory
                        entityBytes, // optional
                        // optionally add sterilizer only if the external reference entity implements CustomEntityDescriptor
                        entityObject instanceof CustomEntityDescriptor ? entitySerializer.getClass().getName() : null
                )
        );
        return entityObject;
    }

    /**
     * Utility function for creating a {@link SecurePasswordEntityHeader}, from the specified <tt>passwordId</tt> and
     * adding the header to our <tt>entityHeaders</tt> collection.
     *
     * @param entityHeaders    self sorted list of entities found so far. Mandatory.
     * @param passwordId       password goid.
     * @throws IllegalArgumentException if the password-id cannot be converted to a goid.
     */
    private void addPasswordEntity(
            @NotNull final Collection<EntityHeader> entityHeaders,
            @NotNull final String passwordId
    ) {
        entityHeaders.add(
                new SecurePasswordEntityHeader(
                        Goid.parseGoid(passwordId),
                        EntityType.SECURE_PASSWORD,
                        null,
                        null,
                        "Password"
                )
        );
    }

    /**
     * For the specified custom assertion (i.e. {@code assertionHolder}}, replace dependent entity, {@code oldEntityHeader},
     * with {@code newEntityHeader}.
     */
    public void replaceEntity(
            @NotNull EntityHeader oldEntityHeader, 
            @NotNull EntityHeader newEntityHeader, 
            @NotNull CustomAssertionHolder assertionHolder
    ) {
        // ignore same values
        if (oldEntityHeader == newEntityHeader) {
            return;
        }
        // make sure types are same
        if (!oldEntityHeader.getType().equals(newEntityHeader.getType())) {
            throw new IllegalArgumentException("Invalid header types. Old entity type \"" +
                    oldEntityHeader.getType() + "\" differs from new entity type \"" +
                    newEntityHeader.getType() + "\"");
        }

        // TODO: add support for future types here
        final Set<Pair<CustomEntityType, String>> processedEntities = new HashSet<>();
        final CustomAssertion customAssertion = assertionHolder.getCustomAssertion();
        if (customAssertion instanceof CustomReferenceEntities) {
            final EntityType entityType = newEntityHeader.getType();
            if (EntityType.SECURE_PASSWORD.equals(entityType)) {
                replaceEntityReference(
                        oldEntityHeader.getStrId(),
                        newEntityHeader.getStrId(),
                        CustomEntityType.SecurePassword,
                        (CustomReferenceEntities) customAssertion,
                        processedEntities
                ); // no need to save custom assertion
            } else if (EntityType.CUSTOM_KEY_VALUE_STORE.equals(entityType)) {
                replaceEntityReference(
                        oldEntityHeader.getName(),
                        newEntityHeader.getName(),
                        CustomEntityType.KeyValueStore,
                        (CustomReferenceEntities)customAssertion,
                        processedEntities
                ); // no need to save custom assertion
            } else {
                logger.log(Level.WARNING, "Unsupported header type \"" + newEntityHeader.getType() + "\".");
            }
        }
    }

    /**
     * Recursively go through all entities used by the custom assertion and replace their ids/keys with the new value.
     * <p/>
     *
     * TODO: add extensive unit-testing here
     *
     * @param oldId                old value of the reference id
     * @param newId                new value of the reference id
     * @param typeForReplace       entity type to replace old with new values
     * @param externalEntity       starting entity reference object
     * @param processedEntities    a hash-set of all processed entities so far, safeguard against circular references
     * @return {@code true} if the specified <tt>externalEntity</tt> was modified with the new id, {@code false} otherwise.
     */
    private boolean replaceEntityReference(
            @NotNull final String oldId,
            @NotNull final String newId,
            @NotNull final CustomEntityType typeForReplace,
            @NotNull final CustomReferenceEntities externalEntity,
            @NotNull final Set<Pair<CustomEntityType, String>> processedEntities
    ) {
        // assume no updates
        boolean ret = false;
        // extract entity references support object
        final CustomReferenceEntitiesSupport entityReferenceSupport = externalEntity.getReferenceEntitiesSupport();
        if (entityReferenceSupport != null) {
            // do getReferenceEntitiesSupport sanity-check, there should always be same instance
            if (entityReferenceSupport != externalEntity.getReferenceEntitiesSupport()) {
                throw new IllegalArgumentException("CustomReferenceEntities method getReferenceEntitiesSupport() returning non-singleton instance!");
            }
            // loop through all references
            for (final Object referenceObject : CustomEntityReferenceSupportAdaptor.getAllReferencedEntities(entityReferenceSupport)) {
                // gather entity id and type
                final String entityId = CustomEntityReferenceSupportAdaptor.getEntityId(referenceObject);
                final CustomEntityType entityType = CustomEntityReferenceSupportAdaptor.getEntityType(referenceObject);
                // check for circular references
                if (processedEntities.contains(Pair.pair(entityType, entityId))) {
                    continue;  // skip entity if this is duplicate i.e. already processed
                }
                processedEntities.add(Pair.pair(entityType, entityId));
                // if the type and old value match, then change entity id/key
                if (typeForReplace.equals(entityType) && oldId.equals(entityId)) {
                    CustomEntityReferenceSupportAdaptor.setEntityId(referenceObject, newId);
                    ret = true; // we've modify externalEntity object, continue further
                } else {
                    // this is not the entity we need to change, so process its child entities
                    final CustomEntitySerializer entitySerializer = CustomEntityReferenceSupportAdaptor.getEntitySerializer(referenceObject);
                    if (entitySerializer != null) {
                        try {
                            final byte[] bytes = extractEntityBytes(entityId, entityType);
                            if (bytes != null) {
                                final Object entityObject = entitySerializer.deserialize(bytes);
                                if (entityObject instanceof CustomReferenceEntities) {
                                    if (replaceEntityReference(oldId, newId, typeForReplace, (CustomReferenceEntities) entityObject, processedEntities)) {
                                        //noinspection unchecked
                                        saveExternalEntity(entityType, entityId, entitySerializer.serialize(entityObject));
                                        ret = true; // we've modify externalEntity object, continue further
                                    }
                                }
                            }
                        } catch (final ReferenceEntityBytesException e) {
                            logger.log(Level.WARNING, "Failed to extract entity id: \"" + entityId + "\", type: \"" + entityType + "\"", e);
                        } catch (final Throwable e) {
                            logger.log(Level.WARNING, "Failed to replace entity id: \"" + entityId + "\", type: \"" + entityType + "\"", e);
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Utility function for extracting reference entity bytes, specified with id and type.<br/>
     * Currently we support only custom key value store.
     * <p/>
     * TODO: add support for future types here
     *
     * @param entityId     referenced entity id
     * @param entityType   referenced entity type
     * @return the bytes associated with the reference entity id, or {@code null} if the reference doesn't exist in the system.
     * @throws ReferenceEntityBytesException if an error happens during extraction.
     */
    @Nullable
    private byte[] extractEntityBytes(
            @NotNull String entityId,
            @NotNull CustomEntityType entityType
    ) throws ReferenceEntityBytesException {
        switch (entityType) {
            case KeyValueStore:
                try {
                    return keyValueStore.get(entityId);
                } catch (KeyValueStoreException | IllegalStateException e) {
                    throw new ReferenceEntityBytesException("Failed to extract entity bytes! id: \"" + entityId + "\", type: \"" + entityType + "\".", e);
                }
            default:
                throw new ReferenceEntityBytesException("Unsupported entity type: " + entityType);
        }
    }

    /**
     * Exported entity was modified during import, therefore save/update the modified entity using it's owner storage.
     * <p/>
     * TODO: add support for future types here
     *
     * @param entityType    entity type. Mandatory
     * @param entityKey     entity key. Mandatory
     * @param entityBytes   entity row-bytes. Optional, we'll let KeyValueStore to throw if {@code null} bytes are not allowed.
     */
    private void saveExternalEntity(
            @NotNull final CustomEntityType entityType,
            @NotNull final String entityKey,
            @Nullable final byte[] entityBytes
    ) {
        if (entityBytes == null) {
            logger.warning("saveExternalEntity called with null bytes! Cannot save entity: \"" + entityKey + "\" with type: \"" + entityType + "\"");
        }

        switch (entityType) {
            case KeyValueStore:
                keyValueStore.saveOrUpdate(entityKey, entityBytes);
                break;
            default:
                // we only support CustomKeyValue for now
                logger.log(Level.WARNING, "saveExternalEntity called for unsupported type: \"" + entityType + "\", entity: \"" + entityKey + "\" will not be saved!");
                break;
        }
    }
}
