package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.ServerModuleFileAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServerModuleFileMO;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.bundling.EntityContainer;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;
import java.util.TreeMap;

/**
 * Transform {@link ServerModuleFile internal entity} to a {@link ServerModuleFileMO managed object} and vise versa.
 */
@Component
public class ServerModuleFileTransformer implements EntityAPITransformer<ServerModuleFileMO, ServerModuleFile> {

    @Inject
    private ServerModuleFileAPIResourceFactory factory;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.SERVER_MODULE_FILE.toString();
    }

    @NotNull
    public ServerModuleFileMO convertToMO(@NotNull final ServerModuleFile serverModuleFile) {
        return convertToMO(serverModuleFile, null);
    }

    @NotNull
    public ServerModuleFileMO convertToMO(@NotNull final ServerModuleFile serverModuleFile, final boolean includeData) {
        return convertToMO(serverModuleFile, null, includeData);
    }

    @NotNull
    @Override
    public ServerModuleFileMO convertToMO(
            @NotNull final EntityContainer<ServerModuleFile> serverModuleFileEntityContainer,
            final SecretsEncryptor secretsEncryptor
    ) {
        return convertToMO(serverModuleFileEntityContainer.getEntity(), secretsEncryptor);
    }

    @NotNull
    @Override
    public ServerModuleFileMO convertToMO(
            @NotNull final ServerModuleFile moduleFile,
            final SecretsEncryptor secretsEncryptor
    ) {
        return convertToMO(moduleFile, secretsEncryptor, false);
    }

    /**
     * Transforms the {@link ServerModuleFile internal entity} to a {@link ServerModuleFileMO managed object}.
     * <p/>
     * Note that this might fail with {@code ResourceNotFoundException} in case when include data is set to {@code true}
     * and the entity to transform have been deleted before downloading it's data bytes.<br/>
     * Unfortunately there is nothing much that can be done in this case but throw runtime exception (i.e. ResourceAccessException)
     * as this entity cannot be skipped from here.
     *
     * @param moduleFile          The entity to transform.
     * @param secretsEncryptor    To encrypt passwords. {@code null} indicates do not include password.
     * @param includeData         A flag indicating whether to include module data along with the meta info.
     * @return The resulting managed object.
     */
    @NotNull
    public ServerModuleFileMO convertToMO(
            @NotNull final ServerModuleFile moduleFile,
            @SuppressWarnings("UnusedParameters") final SecretsEncryptor secretsEncryptor,
            final boolean includeData
    ) {
        final ServerModuleFileMO serverModuleFileMO = ManagedObjectFactory.createServerModuleFileMO();
        serverModuleFileMO.setId(moduleFile.getId());
        serverModuleFileMO.setVersion(moduleFile.getVersion());
        serverModuleFileMO.setName(asName(moduleFile.getName()));
        serverModuleFileMO.setModuleType(convertModuleType(moduleFile.getModuleType()));
        serverModuleFileMO.setModuleSha256(moduleFile.getModuleSha256());
        serverModuleFileMO.setProperties(gatherProperties(moduleFile, ServerModuleFile.getPropertyKeys()));

        if (includeData) {
            try {
                // Note that this might fail with ResourceNotFoundException in case when the ServerModuleFile entity
                // have been deleted before downloading it's bytes.
                // Unfortunately there is nothing much that can be done in this case but throw runtime exception (i.e. ResourceAccessException)
                // as this entity cannot be skipped from here
                factory.setModuleData(serverModuleFileMO);
            } catch (final ResourceFactory.ResourceNotFoundException e) {
                // if the ServerModuleFile entity was deleted or we fail to download its data throw ResourceAccessException
                throw new ResourceFactory.ResourceAccessException(e);
            }
        }

        return serverModuleFileMO;
    }

    @NotNull
    @Override
    public EntityContainer<ServerModuleFile> convertFromMO(
            @NotNull final ServerModuleFileMO serverModuleFileMO,
            final SecretsEncryptor secretsEncryptor
    ) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(serverModuleFileMO, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<ServerModuleFile> convertFromMO(
            @NotNull final ServerModuleFileMO serverModuleFileMO,
            final boolean strict,
            final SecretsEncryptor secretsEncryptor
    ) throws ResourceFactory.InvalidResourceException {

        final ServerModuleFile serverModuleFile = new ServerModuleFile();
        serverModuleFile.setId(serverModuleFileMO.getId());
        if (serverModuleFileMO.getVersion() != null) {
            serverModuleFile.setVersion(serverModuleFileMO.getVersion());
        }
        serverModuleFile.setName(asName(serverModuleFileMO.getName()));

        serverModuleFile.setModuleType(convertModuleType(serverModuleFileMO.getModuleType()));

        if (StringUtils.isBlank(serverModuleFileMO.getModuleSha256())) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "Module SHA-256 must be set");
        }
        if (serverModuleFileMO.getModuleData() == null) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES, "Module data bytes must be set");
        }
        serverModuleFile.createData(serverModuleFileMO.getModuleData(), serverModuleFileMO.getModuleSha256());
        final String calculatedDigest = ServerModuleFile.calcBytesChecksum(serverModuleFile.getData().getDataBytes());
        if (!calculatedDigest.equals(serverModuleFile.getModuleSha256())) {
            throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Module data bytes doesn't match SHA-256");
        }

        setProperties(serverModuleFileMO, serverModuleFile, ServerModuleFile.getPropertyKeys());

        return new EntityContainer<>(serverModuleFile);
    }

    @NotNull
    @Override
    public Item<ServerModuleFileMO> convertToItem(@NotNull final ServerModuleFileMO m) {
        return new ItemBuilder<ServerModuleFileMO>(
                m.getName(),
                m.getId(),
                EntityType.SERVER_MODULE_FILE.name()
        ).setContent(m).build();
    }

    /**
     * Convenient method for trimming the name field.
     *
     * @param name    the name field value to trim.  Optional and can be {@code null}.
     * @return A {@code String} containing trimmed version of the specified {@code name} field
     * or {@code null} if {@code name} is {@code null}
     */
    @Nullable
    static String asName(@Nullable final String name) {
        return name != null ? name.trim() : null;
    }

    /**
     * Utility method for converting between {@link ServerModuleFile} and {@link ServerModuleFileMO} module type.
     *
     * @param serverModuleType    Input {@link com.l7tech.gateway.api.ServerModuleFileMO.ServerModuleFileModuleType ServerModuleFileModuleType} for converting.  Required and cannot be {@code null}
     * @return A {@link ModuleType} from the specified {@code moduleType}, never {@code null}.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException if the specified {@code moduleType} is not recognized.
     */
    @NotNull
    static ModuleType convertModuleType(final ServerModuleFileMO.ServerModuleFileModuleType serverModuleType) throws ResourceFactory.InvalidResourceException {
        switch (serverModuleType) {
            case MODULAR_ASSERTION:
                return ModuleType.MODULAR_ASSERTION;
            case CUSTOM_ASSERTION:
                return ModuleType.CUSTOM_ASSERTION;
            default:
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Unknown Module Type '" + serverModuleType + "'.");
        }
    }

    /**
     * Utility method for converting between {@link ServerModuleFile} and {@link ServerModuleFileMO} module type.
     *
     * @param moduleType    Input {@link ModuleType} for converting.  Required and cannot be {@code null}
     * @return A {@link com.l7tech.gateway.api.ServerModuleFileMO.ServerModuleFileModuleType ServerModuleFileModuleType} from the specified {@code moduleType}, never {@code null}.
     * @throws ResourceFactory.ResourceAccessException if the specified {@code moduleType} is not recognized.
     */
    @NotNull
    static ServerModuleFileMO.ServerModuleFileModuleType convertModuleType(final ModuleType moduleType) throws ResourceFactory.ResourceAccessException {
        switch (moduleType) {
            case MODULAR_ASSERTION:
                return ServerModuleFileMO.ServerModuleFileModuleType.MODULAR_ASSERTION;
            case CUSTOM_ASSERTION:
                return ServerModuleFileMO.ServerModuleFileModuleType.CUSTOM_ASSERTION;
            default:
                throw new ResourceFactory.ResourceAccessException("Unknown Module Type '" + moduleType + "'.");
        }
    }

    /**
     * Utility method for converting {@link com.l7tech.gateway.common.module.ServerModuleFile#getXmlProperties()} entity properties}
     * into {@link ServerModuleFileMO#properties MO properties}.
     *
     * @param moduleFile    the {@code ServerModuleFile} entity to gather properties from.
     * @param keys          the list of known {@code ServerModuleFile} properties (i.e. property keys).
     * @return a {@code Map} of all specified {@code moduleFile} properties, or {@code null} if the
     * specified {@code moduleFile} does not contain any properties.
     */
    @Nullable
    static Map<String, String> gatherProperties(@NotNull final ServerModuleFile moduleFile, @NotNull final String[] keys) {
        final Map<String, String> props = new TreeMap<>();
        for (final String key : keys) {
            final String value = moduleFile.getProperty(key);
            if (value != null) {
                props.put(key, value);
            }
        }
        return props.isEmpty() ? null : props;
    }

    /**
     * Loop through all registered properties of the input {@code moduleFileMO} and set them accordingly into the
     * output {@code moduleFile}.
     *
     * @param moduleFileMO    input {@link ServerModuleFileMO MO}.
     * @param moduleFile      output {@link ServerModuleFile internal entity}.
     * @param keys            the list of known {@code ServerModuleFile} properties (i.e. property keys).
     */
    static void setProperties(
            @NotNull final ServerModuleFileMO moduleFileMO,
            @NotNull final ServerModuleFile moduleFile,
            @NotNull final String[] keys
    ) {
        final Map<String, String> props = moduleFileMO.getProperties();
        if (props != null) {
            for (final String key : keys) {
                final String value = props.get(key);
                if (value != null) {
                    moduleFile.setProperty(key, value);
                }
            }
        }
    }
}
