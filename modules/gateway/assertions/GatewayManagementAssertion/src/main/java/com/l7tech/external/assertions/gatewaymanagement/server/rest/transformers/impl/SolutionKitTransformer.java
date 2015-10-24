package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.solutionkit.EntityOwnershipDescriptor;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 */
@Component
public class SolutionKitTransformer implements EntityAPITransformer<SolutionKitMO, SolutionKit> {

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.SOLUTION_KIT.toString();
    }

    @NotNull
    public SolutionKitMO convertToMO(@NotNull final SolutionKit solutionKit) {
        return convertToMO(solutionKit, null);
    }

    @NotNull
    @Override
    public SolutionKitMO convertToMO(@NotNull final EntityContainer<SolutionKit> solutionKitEntityContainer, final SecretsEncryptor secretsEncryptor) {
        return convertToMO(solutionKitEntityContainer.getEntity(), secretsEncryptor);
    }

    @NotNull
    @Override
    public SolutionKitMO convertToMO(@NotNull final SolutionKit solutionKit, final SecretsEncryptor secretsEncryptor) {
        final SolutionKitMO solutionKitMO = ManagedObjectFactory.createSolutionKitMO();
        solutionKitMO.setId(solutionKit.getId());
        solutionKitMO.setVersion(solutionKit.getVersion());
        solutionKitMO.setName(solutionKit.getName());
        solutionKitMO.setSkGuid(solutionKit.getSolutionKitGuid());
        solutionKitMO.setSkVersion(solutionKit.getSolutionKitVersion());
        solutionKitMO.setProperties(gatherProperties(solutionKit, SolutionKit.getPropertyKeys()));
        solutionKitMO.setInstallProperties(gatherProperties(solutionKit, SolutionKit.getInstallPropertyKeys()));
        solutionKitMO.setUninstallBundle(solutionKit.getUninstallBundle());
        solutionKitMO.setMappings(solutionKit.getMappings());
        solutionKitMO.setLastUpdateTime(solutionKit.getLastUpdateTime());
        final Goid parentGoid = solutionKit.getParentGoid();
        if (parentGoid != null) {
            solutionKitMO.setParentReference(new ManagedObjectReference(SolutionKitMO.class, solutionKit.getParentGoid().toString()));
        }
        solutionKitMO.setEntityOwnershipDescriptors(asResource(solutionKit.getEntityOwnershipDescriptors()));

        return solutionKitMO;
    }

    @NotNull
    @Override
    public EntityContainer<SolutionKit> convertFromMO(@NotNull final SolutionKitMO solutionKitMO, final SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(solutionKitMO, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<SolutionKit> convertFromMO(
            @NotNull final SolutionKitMO solutionKitMO,
            final boolean strict,
            final SecretsEncryptor secretsEncryptor
    ) throws ResourceFactory.InvalidResourceException {
        final SolutionKit solutionKit = new SolutionKit();

        solutionKit.setId(solutionKitMO.getId());

        // set entity version (optional so only if present)
        if (solutionKitMO.getVersion() != null) {
            solutionKit.setVersion(solutionKitMO.getVersion());
        }

        // set name (mandatory so throw if missing)
        final String name = trimValue(solutionKitMO.getName());
        if (StringUtils.isEmpty(name)) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "SolutionKit Name must be set");
        }
        solutionKit.setName(name);

        // set SK Guid (mandatory so throw if missing)
        final String solutionKitGuid = trimValue(solutionKitMO.getSkGuid());
        if (StringUtils.isEmpty(solutionKitGuid)) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "SolutionKit Guid must be set");
        }
        solutionKit.setSolutionKitGuid(solutionKitGuid);

        // set SK Version (mandatory so throw if missing)
        final String solutionKitVersion = trimValue(solutionKitMO.getSkVersion());
        if (StringUtils.isEmpty(solutionKitVersion)) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "SolutionKit Version must be set");
        }
        solutionKit.setSolutionKitVersion(solutionKitVersion);

        // set properties (optional so only if present and non null values)
        setProperties(
                solutionKitMO.getProperties(),
                SolutionKit.getPropertyKeys(),
                new Functions.BinaryVoid<String, String>() {
                    @Override
                    public void call(@NotNull final String name, @NotNull final String value) {
                        solutionKit.setProperty(name, value);
                    }
                });

        // TODO : do we have install properties at all ?
        // set install properties (optional so only if present and non null values)
        setProperties(
                solutionKitMO.getInstallProperties(),
                SolutionKit.getInstallPropertyKeys(),
                new Functions.BinaryVoid<String, String>() {
                    @Override
                    public void call(@NotNull final String name, @NotNull final String value) {
                        solutionKit.setInstallationProperty(name, value);
                    }
                }
        );

        // set uninstall bundle (optional so only if present)
        final String uninstallBundle = solutionKitMO.getUninstallBundle();
        if (StringUtils.isNotBlank(uninstallBundle)) {
            solutionKit.setUninstallBundle(uninstallBundle);
        }

        // set Mappings (mandatory so throw if missing)
        final String mappings = trimValue(solutionKitMO.getMappings());
        if (StringUtils.isEmpty(mappings)) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "SolutionKit Mappings must be set");
        }
        solutionKit.setMappings(mappings);

        // set Last Update Time (mandatory so throw if missing)
        final Long lastUpdateTime = solutionKitMO.getLastUpdateTime();
        if (lastUpdateTime == null) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "SolutionKit Last Update Time must be set");
        }
        solutionKit.setLastUpdateTime(lastUpdateTime);

        // set parent goid
        final Goid parentGoid = asGoid(solutionKitMO.getParentReference());
        if (parentGoid != null) {
            solutionKit.setParentGoid(parentGoid);
        }

        solutionKit.setEntityOwnershipDescriptors(asEntityOwnershipDescriptors(solutionKit, solutionKitMO.getEntityOwnershipDescriptors()));

        return new EntityContainer<>(solutionKit);
    }

    @NotNull
    @Override
    public Item<SolutionKitMO> convertToItem(@NotNull final SolutionKitMO m) {
        return new ItemBuilder<SolutionKitMO>(
                m.getName(),
                m.getId(),
                EntityType.SOLUTION_KIT.name()
        ).setContent(m).build();
    }

    /**
     * Convenient method for trimming the specified {@code value}.
     *
     * @param value    the field value to trim.  Optional and can be {@code null}.
     * @return A {@code String} containing trimmed version of the specified {@code value} field
     * or {@code null} if {@code value} is {@code null}
     */
    @Nullable
    private static String trimValue(@Nullable final String value) {
        return value != null ? value.trim() : null;
    }

    /**
     * Utility method for converting {@link com.l7tech.gateway.common.solutionkit.SolutionKit#getXmlProperties()} entity properties or install properties
     * into {@link com.l7tech.gateway.api.SolutionKitMO#properties MO properties}.
     *
     * @param solutionKit    the {@code SolutionKit} entity to gather properties from.  Required and cannot be {@code null}.
     * @param keys           array of known {@code ServerModuleFile} properties (i.e. property keys).  Required and cannot be {@code null}.
     * @return read-only {@code Map} of all specified {@code solutionKit} properties, or {@code null} if the specified
     * {@code solutionKit} does not contain any known property keys.
     */
    @Nullable
    private static Map<String, String> gatherProperties(@NotNull final SolutionKit solutionKit, @NotNull final String[] keys) {
        final Map<String, String> props = new TreeMap<>();
        for (final String key : keys) {
            final String value = solutionKit.getProperty(key);
            if (value != null) {
                props.put(key, value);
            }
        }
        return props.isEmpty() ? null : Collections.unmodifiableMap(props);
    }

    /**
     * Loop through all registered properties of the source {@code props} and set them accordingly using the {@code propSetterCallback}.
     *
     * @param props                 Source {@code com.l7tech.gateway.api.ServerModuleFileMO} properties.  Optional and can be {@code null}.
     * @param keys                  List of known {@code SolutionKit} properties (i.e. property keys).
     * @param propSetterCallback    Called for each property Destination {@link com.l7tech.gateway.common.solutionkit.SolutionKit internal entity}.
     */
    private static void setProperties(
            @Nullable final Map<String, String> props,
            @NotNull final String[] keys,
            @NotNull final Functions.BinaryVoid<String, String> propSetterCallback
    ) throws ResourceFactory.InvalidResourceException {
        if (props != null && !props.isEmpty()) {
            for (final String key : keys) {
                final String value = props.get(key);
                if (value != null) {
                    propSetterCallback.call(key, value);
                }
            }
        }
    }

    /**
     * Extract the given {@code ManagedObjectReference} internal identifier (i.e. {@code Goid}).
     *
     * @param reference    The specified {@code ManagedObjectReference}.  Optional and can be {@code null}.
     * @return The identifier as a {@code Goid} or {@code null} if the specified {@code reference} is {@code null}.
     */
    @Nullable
    private static Goid asGoid(@Nullable final ManagedObjectReference reference) {
        return reference != null ? Goid.parseGoid(reference.getId()) : null;
    }

    /**
     * Convert a collection of {@code EntityOwnershipDescriptor}'s into List of {@link EntityOwnershipDescriptorMO Managed Objects}.
     *
     * @param entityOwnershipDescriptors    collection of {@code EntityOwnershipDescriptor}'s.  Optional and can be {@code null}.
     * @return a Read-only list of {@link EntityOwnershipDescriptorMO Managed Objects} or {@code null} if the specified
     * {@code entityOwnershipDescriptors} is empty or {@code null}.
     */
    @Nullable
    private static List<EntityOwnershipDescriptorMO> asResource(@Nullable final Collection<EntityOwnershipDescriptor> entityOwnershipDescriptors) {
        if (entityOwnershipDescriptors != null) {
            final List<EntityOwnershipDescriptorMO> entityOwnershipDescriptorMOs = new ArrayList<>(entityOwnershipDescriptors.size());
            for (final EntityOwnershipDescriptor entityOwnershipDescriptor : entityOwnershipDescriptors) {
                final EntityOwnershipDescriptorMO entityOwnershipDescriptorMO = ManagedObjectFactory.createEntityOwnershipDescriptorMO();
                entityOwnershipDescriptorMO.setId(entityOwnershipDescriptor.getId());
                entityOwnershipDescriptorMO.setEntityId(entityOwnershipDescriptor.getEntityId());
                entityOwnershipDescriptorMO.setEntityType(entityOwnershipDescriptor.getEntityType().name());
                entityOwnershipDescriptorMO.setReadOnly(entityOwnershipDescriptor.isReadOnly());
                entityOwnershipDescriptorMOs.add(entityOwnershipDescriptorMO);
            }
            return entityOwnershipDescriptorMOs.isEmpty() ? null : Collections.unmodifiableList(entityOwnershipDescriptorMOs);
        }
        return null;
    }

    /**
     * Convert a collection of {@code EntityOwnershipDescriptorMO}'s into List of {@link EntityOwnershipDescriptor}'s.
     *
     * @param entityOwnershipDescriptorMOs    collection of {@code EntityOwnershipDescriptorMO}'s.  Optional and can be {@code null}.
     * @return a {@code list} of {@link EntityOwnershipDescriptor}'s or {@code null} if the specified {@code entityOwnershipDescriptorMOs} is empty or {@code null}.
     */
    @Nullable
    private static List<EntityOwnershipDescriptor> asEntityOwnershipDescriptors(
            @NotNull final SolutionKit solutionKitOwner,
            @Nullable final Collection<EntityOwnershipDescriptorMO> entityOwnershipDescriptorMOs
    ) throws ResourceFactory.InvalidResourceException {
        if (entityOwnershipDescriptorMOs != null) {
            final List<EntityOwnershipDescriptor> entityDescriptors = new ArrayList<>(entityOwnershipDescriptorMOs.size());
            for (final EntityOwnershipDescriptorMO entityOwnershipDescriptorMO : entityOwnershipDescriptorMOs) {
                // ignore ids to avoid StaleStateException (practice for other entities as well)
//                final String id = trimValue(entityOwnershipDescriptorMO.getId());
//                if (StringUtils.isEmpty(id)) {
//                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "EntityOwnershipDescriptor Id must be set");
//                }

                // get entity id (mandatory so throw if missing)
                final String entityId = trimValue(entityOwnershipDescriptorMO.getEntityId());
                if (StringUtils.isEmpty(entityId)) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "EntityOwnershipDescriptor EntityId must be set");
                }

                // get entity type (mandatory so throw if missing)
                final String entityTypeString = trimValue(entityOwnershipDescriptorMO.getEntityType());
                if (StringUtils.isEmpty(entityTypeString)) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "EntityOwnershipDescriptor EntityType must be set");
                }
                final EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeString);
                } catch (IllegalArgumentException e) {
                    throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "EntityOwnershipDescriptor unknown EntityType: " + entityTypeString);
                }

                // by default all entities are not read-only
                boolean readOnly = false;
                if (entityOwnershipDescriptorMO.isReadOnly() != null) {
                    readOnly = entityOwnershipDescriptorMO.isReadOnly();
                }

                assert entityId != null;
                assert entityType != null;

                // create the EntityOwnershipDescriptor object
                final EntityOwnershipDescriptor entityOwnershipDescriptor = new EntityOwnershipDescriptor(
                        solutionKitOwner,
                        entityId,
                        entityType,
                        readOnly
                );

                // ignore ids to avoid StaleStateException (practice for other entities as well)
                //entityOwnershipDescriptor.setId(id);

                // add toi the returning list
                entityDescriptors.add(entityOwnershipDescriptor);
            }
            return entityDescriptors.isEmpty() ? null : entityDescriptors;
        }
        return null;
    }
}
