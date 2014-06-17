package com.l7tech.gateway.common.custom;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ReferenceEntityBytesException;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.entity.*;
import com.l7tech.policy.assertion.ext.security.SignerServices;
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
 * <p/>
 * Typically used during policy migration (both policy import and export).
 *
 * @see com.l7tech.policy.assertion.UsesEntities
 */
public class CustomEntitiesResolver {
    private static final Logger logger = Logger.getLogger(CustomEntitiesResolver.class.getName());

    /**
     * Access to CustomKeyValueStore entities.
     */
    @NotNull
    private final KeyValueStore keyValueStore;

    /**
     * {@link CustomReferenceEntitiesSupport} holds entity serializer class name, therefore this interface impl should
     * locate and instantiate {@link CustomEntitySerializer} registered with the specified class name.
     */
    @NotNull
    private final ClassNameToEntitySerializer classNameToSerializer;

    /**
     * Default constructor.
     */
    public CustomEntitiesResolver(
            @NotNull final KeyValueStore keyValueStore,
            @NotNull final ClassNameToEntitySerializer classNameToSerializer
    ) {
        this.keyValueStore = keyValueStore;
        this.classNameToSerializer = classNameToSerializer;
    }

    /**
     * Returns a list of {@link EntityHeader}'s containing all external entities used by the specified custom assertion.<br/>
     * Typically used during policy import and export while gathering {@code CustomAssertionHolder}'s referenced entities.
     */
    @NotNull
    public EntityHeader[] getEntitiesUsed(@NotNull CustomAssertionHolder assertionHolder) {
        final CustomAssertion customAssertion = assertionHolder.getCustomAssertion();
        if (customAssertion == null) {
            throw new IllegalStateException("CustomAssertion missing from assertionHolder");
        }

        final Collection<EntityHeader> entityHeaders = new LinkedHashSet<>();
        if (customAssertion instanceof CustomReferenceEntities) {
            processEntityReference(entityHeaders, (CustomReferenceEntities)customAssertion);
        }

        return (sortEntities(entityHeaders));
    }

    /**
     * Utility function for sorting specified {@link EntityHeader}'s collection based on the order they need to be
     * processed during import.
     * <p/>
     * In the resulting array first will be entities of type {@link EntityType#CUSTOM_KEY_VALUE_STORE}, then entities
     * of type {@link EntityType#SSG_KEY_ENTRY}, then {@link EntityType#SECURE_PASSWORD} and at the end any other entity type.
     * <p/>
     * Modify {@code entityTypeToOrdinal(...)} function below to add additional entity types.
     *
     * @param collection    collection
     * @return sorted {@link EntityHeader}'s array.  Never {@code null}.
     */
    private EntityHeader[] sortEntities(@NotNull final Collection<EntityHeader> collection) {
        final List<EntityHeader> list = new ArrayList<>(collection);
        Collections.sort(list, new Comparator<EntityHeader>() {
            @Override
            public int compare(@NotNull final EntityHeader o1, @NotNull final EntityHeader o2) {
                final int ordinal1 = entityTypeToOrdinal(o1.getType()),
                        ordinal2 = entityTypeToOrdinal(o2.getType());
                return (ordinal1 < ordinal2 ? -1 : (ordinal1 == ordinal2 ? 0 : 1));
            }

            /**
             * Order entities in the correct order.<br/>
             * First should be entities which doesn't depend on other entities, like PrivateKey's and SecurePassword's,
             * finally should be entities with other entity dependencies, like CustomKeyValueStore's.
             *
             * @param type    specified entity type
             * @return number corresponding the order (smallest number goes first) of the specified entity type.
             */
            private int entityTypeToOrdinal(@NotNull final EntityType type) {
                switch (type) {
                    case SSG_KEY_ENTRY:
                        return 0;
                    case SECURE_PASSWORD:
                        return 1;
                    case CUSTOM_KEY_VALUE_STORE:
                        return 2;
                }
                return 3; // unsupported type will go last, assumed to have dependencies
            }
        });
        return (list.toArray(new EntityHeader[list.size()]));
    }

    /**
     * Recursively go through all entities used by the custom assertion and insert them in the list.
     * <p/>
     * TODO: add extensive unit-testing here
     *
     * *** add logic for future entity types here ***
     *
     * @param entityHeaders     self sorted list of entities found so far.
     * @param parentEntity      starting entity reference object.
     */
    protected void processEntityReference(
            @NotNull final Collection<EntityHeader> entityHeaders,
            @NotNull final CustomReferenceEntities parentEntity
    ) {
        final CustomReferenceEntitiesSupport entityReferenceSupport = parentEntity.getReferenceEntitiesSupport();
        if (entityReferenceSupport != null) {
            // do getReferenceEntitiesSupport sanity-check, there should always be same instance
            if (entityReferenceSupport != parentEntity.getReferenceEntitiesSupport()) {
                throw new IllegalArgumentException("CustomReferenceEntities method getReferenceEntitiesSupport() returning non-singleton instance!");
            }
            // loop through all references
            for (final Object referenceObject : CustomEntityReferenceSupportAccessor.getAllReferencedEntities(entityReferenceSupport)) {
                // gather entity id and type
                final String entityId = CustomEntityReferenceSupportAccessor.getEntityId(referenceObject);
                // extract entity type
                final CustomEntityType entityType = extractEntityType(referenceObject);
                // check entity type
                switch (entityType) {
                    case SecurePassword:
                        // add reference entity, will throw with RuntimeException when the password-id is not a valid GOID.
                        final EntityHeader entityHeader = new SecurePasswordEntityHeader(
                                Goid.parseGoid(entityId),
                                EntityType.SECURE_PASSWORD,
                                null,
                                null,
                                "Password" // this is ignored
                                           // StoredPasswordReference will extract the correct type from the goid
                        );
                        // make sure this entity wasn't referenced before
                        if (!entityHeaders.contains(entityHeader)) {
                            entityHeaders.add(entityHeader);
                        } else {
                            logger.finer("Ignoring already referenced Secure Password: \"" + entityId + "\"");
                        }
                        break;

                    case PrivateKey:
                        // add reference entity, will throw with RuntimeException when the key-id is invalid.
                        if (!SignerServices.KEY_ID_SSL.equals(entityId)) {
                            // Add none default key only.
                            final String[] keyIdSplit = entityId.split(":");
                            if (keyIdSplit.length != 2) {
                                throw new IllegalArgumentException("Invalid key ID format.");
                            }
                            final Goid keyStoreId = Goid.parseGoid(keyIdSplit[0]);
                            final String keyAlias = keyIdSplit[1];
                            // create entity header
                            final SsgKeyHeader keyHeader = new SsgKeyHeader(entityId, keyStoreId, keyAlias, null);
                            // make sure this entity wasn't referenced before
                            if (!entityHeaders.contains(keyHeader)) {
                                entityHeaders.add(keyHeader);
                            } else {
                                logger.finer("Ignoring already referenced Private Key: \"" + entityId + "\"");
                            }
                        }
                        break;

                    case KeyValueStore:
                        // get the serializer in order to go through any potential reference dependencies
                        final CustomEntitySerializer entitySerializer = findEntitySerializerFromClassName(
                                CustomEntityReferenceSupportAccessor.getSerializerClassName(referenceObject)
                        );
                        // get entity prefix, mandatory for KeyValueStore
                        final String entityKeyPrefix = CustomEntityReferenceSupportAccessor.getEntityKeyPrefix(referenceObject);
                        if (entityKeyPrefix == null) {
                            throw new IllegalArgumentException(
                                    "Referenced entity with id: \"" + CustomEntityReferenceSupportAccessor.getEntityId(referenceObject) +
                                            "\" type: \"" + CustomEntityReferenceSupportAccessor.getEntityType(referenceObject) +
                                            "\" doesn't provide any prefix!"
                            );
                        }
                        // extract entity bytes
                        // afterwards deserialize the entity object from the bytes
                        Object entityObject = null;
                        byte[] entityBytes = null;
                        try {
                            entityBytes = extractEntityBytes(entityId, CustomEntityType.KeyValueStore);
                            entityObject = (entityBytes != null && entitySerializer != null) ? entitySerializer.deserialize(entityBytes) : null;
                        } catch (final Exception  e) {
                            logger.log(Level.WARNING, "Failed to extract key-val-store bytes for id: \"" + entityId + "\"", e);
                        }
                        // create entity header
                        final CustomKeyStoreEntityHeader keyStoreEntityHeader = new CustomKeyStoreEntityHeader(
                                entityId, // mandatory
                                entityKeyPrefix, // mandatory
                                entityBytes, // optional
                                // optionally add serializer only if the external reference entity implements CustomEntityDescriptor
                                entityObject instanceof CustomEntityDescriptor ? entitySerializer.getClass().getName() : null
                        );
                        // make sure this entity wasn't referenced before
                        if (!entityHeaders.contains(keyStoreEntityHeader)) {
                            entityHeaders.add(keyStoreEntityHeader);
                            // next process any potential child entity dependencies
                            if (entityObject instanceof CustomReferenceEntities) {
                                processEntityReference(entityHeaders, (CustomReferenceEntities)entityObject);
                            }
                        } else {
                            logger.finer("Ignoring already referenced CustomKeyValueStore: \"" + entityId + "\"");
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
     * Convert specified referenced entity type, as string, into {@link CustomEntityType} enum.
     *
     * @param referenceObject    referenced entity object.
     * @return corresponding {@code CustomEntityType}.
     * @throws IllegalArgumentException if specified entity contains unrecognized type
     * @see CustomEntityReferenceSupportAccessor#getEntityType(Object)
     * @see CustomEntityType
     */
    @NotNull
    private CustomEntityType extractEntityType(final Object referenceObject) throws IllegalArgumentException {
        final String entityType = CustomEntityReferenceSupportAccessor.getEntityType(referenceObject);
        try {
            return CustomEntityType.valueOf(entityType);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Referenced entity with id: \"" + CustomEntityReferenceSupportAccessor.getEntityId(referenceObject) +
                            "\" having unrecognized type \"" + entityType + "\"!"
            );
        }
    }

    /**
     * For the specified custom assertion (i.e. {@code assertionHolder}}, replace dependent entity, {@code oldEntityHeader},
     * with {@code newEntityHeader}.<br/>
     * Typically used during policy import while resolving {@code CustomAssertionHolder}'s missing entity.
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
        // cache for processed entities
        final Set<Pair<CustomEntityType, String>> processedEntities = new HashSet<>();
        // extract custom assertion from the holder
        final CustomAssertion customAssertion = assertionHolder.getCustomAssertion();
        if (customAssertion instanceof CustomReferenceEntities) {
            final EntityType entityType = newEntityHeader.getType();
            // *** add logic for future entity types here ***
            if (EntityType.SECURE_PASSWORD.equals(entityType)) {
                replaceEntityReference(
                        oldEntityHeader.getStrId(),
                        newEntityHeader.getStrId(),
                        CustomEntityType.SecurePassword,
                        (CustomReferenceEntities)customAssertion,
                        processedEntities
                ); // no need to save custom assertion
            } else if (EntityType.SSG_KEY_ENTRY.equals(entityType)) {
                replaceEntityReference(
                    oldEntityHeader.getStrId(),
                    newEntityHeader.getStrId(),
                    CustomEntityType.PrivateKey,
                    (CustomReferenceEntities)customAssertion,
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
    protected boolean replaceEntityReference(
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
            for (final Object referenceObject : CustomEntityReferenceSupportAccessor.getAllReferencedEntities(entityReferenceSupport)) {
                // gather entity id and type
                final String entityId = CustomEntityReferenceSupportAccessor.getEntityId(referenceObject);
                // extract entity type
                final CustomEntityType entityType;
                try {
                    entityType = extractEntityType(referenceObject);
                } catch (final IllegalArgumentException e) {
                    logger.warning(e.getMessage());
                    // this entity contains unknown type, skip it
                    continue;
                }
                // if the type and old value match, then change entity id/key
                if (typeForReplace.equals(entityType) && oldId.equals(entityId)) {
                    CustomEntityReferenceSupportAccessor.setEntityId(referenceObject, newId);
                    ret = true; // we've modify externalEntity object, continue further
                } else {
                    // check for circular references
                    if (processedEntities.contains(Pair.pair(entityType, entityId))) {
                        continue;  // skip entity if this is duplicate i.e. already processed
                    }
                    processedEntities.add(Pair.pair(entityType, entityId));
                    // this is not the entity we need to change, so process its child entities
                    final CustomEntitySerializer entitySerializer = findEntitySerializerFromClassName(
                            CustomEntityReferenceSupportAccessor.getSerializerClassName(referenceObject)
                    );
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
     *
     * *** add logic for future entity types here ***
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
     *
     * *** add logic for future entity types here ***
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

    /**
     * Helper method for locating specified <tt>entitySerializerClassName</tt> through our entity serializers registry.
     *
     * @param entitySerializerClassName    entity serializer classname
     * @return entity serializer object registered with the specified <tt>entitySerializerClassName</tt> or {@code null}
     * if the specified <tt>entitySerializerClassName</tt> is not registered or {@code null}
     */
    @Nullable
    private CustomEntitySerializer findEntitySerializerFromClassName(
            @Nullable final String entitySerializerClassName
    ) {
        return (entitySerializerClassName != null) ? classNameToSerializer.getSerializer(entitySerializerClassName) : null;
    }
}
