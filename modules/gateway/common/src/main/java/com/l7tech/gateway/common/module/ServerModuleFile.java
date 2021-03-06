package com.l7tech.gateway.common.module;

import com.l7tech.objectmodel.imp.NamedEntityWithPropertiesImp;
import com.l7tech.search.Dependency;
import com.l7tech.security.rbac.RbacAttribute;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Server Module File (such as a modular or custom assertion) that an SSM administrator adds to the
 * Gateway database in the hope that every cluster node will install or upgrade it.
 */
@Entity
@Proxy(lazy = false)
@Table(name = "server_module_file")
public class ServerModuleFile extends NamedEntityWithPropertiesImp implements Serializable {
    private static final long serialVersionUID = 8274036580628116861L;

    /**
     * Maximum allowed length of the name field.
     */
    public static final int NAME_FIELD_MAX_LENGTH = 128;

    /**
     * Holds the module file-name.
     */
    public static final String PROP_FILE_NAME = "moduleFileName";
    /**
     * Holds the size of the data-bytes in bytes, if known, as a decimal string.
     * Used to report module size when the data-bytes array is being omitted
     * (from being fetched eagerly from the DB or from being downloaded by the SSM).
     * <p/>
     * This value is currently kept up-to-date by the admin layer on a best-effort basis (rather than, for example,
     * being enforced in the manager layer, on in the DB using a computed column or some such), should not be
     * assumed to be 100% reliable, and is intended to be used for display purposes.
     */
    public static final String PROP_SIZE = "moduleSize";
    /**
     * Holds comma separated list of module assertion ClassNames.
     * Used to report readable summary of the module contents.
     * <p/>
     * This value is currently kept up-to-date by the admin layer on a best-effort basis, should not be
     * assumed to be 100% reliable, and is intended to be used for display purposes.
     */
    public static final String PROP_ASSERTIONS = "moduleAssertions";

    /**
     * Convenient field holding all known property keys used by the {@link ServerModuleFile}.<br/>
     * Currently the following property keys are used:
     * <ul>
     *     <li>{@link #PROP_FILE_NAME}</li>
     *     <li>{@link #PROP_SIZE}</li>
     *     <li>{@link #PROP_ASSERTIONS}</li>
     * </ul>
     *
     * Note: When adding new property keys, update this array!
     */
    private static final String[] ALL_PROPERTY_KEYS = {
            PROP_FILE_NAME,
            PROP_SIZE,
            PROP_ASSERTIONS
    };

    /**
     * Specifies the module {@link ModuleType type}.
     */
    private ModuleType moduleType;

    /**
     * Module {@link ServerModuleFileData#dataBytes data-bytes} SHA-256 digest hex-encoded.
     */
    private String moduleSha256;

    // When adding fields, update copyFrom() method

    /**
     * A List of module {@link ServerModuleFileState state}'s for each cluster node.
     */
    private List<ServerModuleFileState> states;
    /**
     * {@link ServerModuleFileData Data} object, holding module bytes and signature properties.
     */
    private ServerModuleFileData data;

    /**
     * {@inheritDoc}
     * <p/>
     * Overridden to set the name field length constraints.
     */
    @Transient
    @RbacAttribute
    @Size(min = 1, max = NAME_FIELD_MAX_LENGTH)
    @Override
    public String getName() {
        return super.getName();
    }


    /**
     * Get the module {@link ModuleType type}.
     */
    @RbacAttribute
    @Enumerated(EnumType.STRING)
    @Column(name = "module_type", length = 64, nullable = false)
    public ModuleType getModuleType() {
        return moduleType;
    }
    public void setModuleType(final ModuleType moduleType) {
        this.moduleType = moduleType;
    }


    /**
     * Get the SHA-256 of the {@link ServerModuleFileData#dataBytes data-bytes} as a hex string.<br/>
     * Note that executing this property getter method might cause the object to be fetched from the Database.
     *
     * @return Hex-encoded SHA-256 digest of data-bytes, or {@link null} if not yet saved.
     */
    @Column(name = "module_sha256", length = 255, nullable = false, unique = true)
    public String getModuleSha256() {
        return moduleSha256;
    }
    public void setModuleSha256(final String moduleSha256) {
        this.moduleSha256 = moduleSha256;
    }


    /**
     * Get the list of all module {@link #states} (from all nodes) for this module.
     * <p/>
     * Note that the collection is fetched eagerly and it includes states from all nodes.
     * <p/>
     * This list is kept up-to-date at the admin and/or manager layer, based on the current node,
     * therefore it is essential not to update this list manually.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "serverModuleFile")
    @Fetch(FetchMode.SUBSELECT)
    @Dependency(isDependency = false)
    public List<ServerModuleFileState> getStates() {
        return states;
    }
    public void setStates(final List<ServerModuleFileState> states) {
        this.states = states;
    }


    /**
     * Represents the module bytes and signature properties.
     * <p/>
     * This property have to be fetch lazy through hibernate proxy objects.
     * This is ONLY possible for a mandatory association between {@code server_module_file} and {@code server_module_file_data} tables.
     * Therefore {@code optional} parameter of {@code @OneToOne} annotation is set to {@code false}.
     * <p/>
     * Note that accessing this property if OK, it will return the proxy object,
     * however accessing {@link ServerModuleFileData} properties will lead to fetching the whole object, including the bytes, from the DB.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, optional = false)
    @JoinColumn(name = "data_goid", nullable = false)
    @Fetch(FetchMode.SELECT)
    @Dependency(isDependency = false)
    public ServerModuleFileData getData() {
        return data;
    }
    public void setData(final ServerModuleFileData data) {
        this.data = data;
    }

    /**
     * Convenient method for setting module data-bytes, calculating data-bytes checksum and setting signature properties.
     *
     * @param dataBytes              new module bytes.  Required and cannot be {@code null}.
     * @param signatureProperties    new module signature properties.  Optional and can be {@code null} if the module is not signed.
     * @see #createData(byte[], String)
     */
    public void createData(@NotNull final byte[] dataBytes, @Nullable final String signatureProperties) {
        createData(dataBytes, ModuleDigest.hexEncodedDigest(dataBytes), signatureProperties);
    }

    /**
     * Creates a new module {@link ServerModuleFileData data}, using the specified {@code dataBytes}, {@code moduleSha256} and {@code signatureProperties}.
     * Existing module data will be updated.
     *
     * @param dataBytes              new module bytes.  Required and cannot be {@code null}.
     * @param moduleSha256           new module bytes digest hex-encoded.  Required and cannot be {@code null}.
     * @param signatureProperties    new module signature properties.  Optional and can be {@code null} if the module is not signed.
     * @see #updateData(byte[], String, String)
     */
    public void createData(
            @NotNull final byte[] dataBytes,
            @NotNull final String moduleSha256,
            @Nullable final String signatureProperties
    ) {
        if (data == null) {
            data = new ServerModuleFileData(this);
        }
        updateData(dataBytes, moduleSha256, signatureProperties);
    }

    /**
     * Updates the module {@link ServerModuleFileData data}, using the specified {@code dataBytes}, {@code moduleSha256} and {@code signatureProperties}.<br/>
     * It is expected that {@link #data} property is set before calling this method.
     *
     * @param dataBytes              updated module bytes.
     * @param moduleSha256           updated module bytes digest hex-encoded.
     * @param signatureProperties    updated module signature properties.
     * @throws IllegalStateException if the {@link #data} property is not set.
     */
    public void updateData(final byte[] dataBytes, final String moduleSha256, final String signatureProperties) {
        if (data == null) {
            throw new IllegalStateException("updateData cannot be called on uninitialized data object");
        }

        data.setDataBytes(dataBytes);
        data.setSignatureProperties(signatureProperties);
        setModuleSha256(moduleSha256);
    }

    /**
     * Creates a new module {@link ServerModuleFileData data}, using the specified {@code data} object.
     * Existing module data will be updated.<br/>
     * If the specified module {@code data} is {@code null}, then this module data will not be updated.
     * However, if this module doesn't have a data object then a blank one will be created.
     *
     * @param data    The {@link ServerModuleFileData} object holding the new module bytes and signature properties.
     */
    private void createData(@Nullable final ServerModuleFileData data) {
        if (this.data == null) {
            this.data = new ServerModuleFileData(this);
        }

        if (data == null) {
            return;
        }

        this.data.copyFrom(data);
    }

    /**
     * Gets the {@link ServerModuleFileState state} for the specified {@code nodeId}
     *
     * @param nodeId    the cluster node identifier.  Required and cannot be {@code null}
     * @return The module {@link ServerModuleFileState state} for the specified {@code nodeId} or {@code null} if there
     * is no state registered for the node.
     */
    @Nullable
    public ServerModuleFileState getStateForNode(@NotNull final String nodeId) {
        if (states != null) {
            for (final ServerModuleFileState state : states) {
                if (nodeId.equals(state.getNodeId())) {
                    return state;
                }
            }
        }
        return null;
    }

    /**
     * Sets the module {@link ServerModuleFileState state} for the specified {@code nodeId}.<br/>
     * This method will set the module state and reset any error message, indicating that the module has successfully
     * transited into the specified state.
     *
     * @param nodeId         The OID of the owning node to update the state.  Required.
     * @param moduleState    module {@link ModuleState state}.  Required.
     * @see #updateState(String, ModuleState, String)
     */
    public void setStateForNode(@NotNull final String nodeId, @NotNull final ModuleState moduleState) {
        updateState(nodeId, moduleState, null);
    }

    /**
     * Sets the module {@link ServerModuleFileState state} error message for the specified {@code nodeId}.<br/>
     * This method will set module error message and its state to {@link ModuleState#ERROR}, indicating that the module
     * failed to transit from current state to the next state.
     *
     * @param nodeId          The OID of the owning node to update the state.  Required.
     * @param errorMessage    A {@code String} describing the error.  Required.
     * @see #updateState(String, ModuleState, String)
     */
    public void setStateErrorMessageForNode(@NotNull final String nodeId, @NotNull final String errorMessage) {
        updateState(nodeId, ModuleState.ERROR, errorMessage);
    }

    /**
     * Convenient method for updating module {@link ServerModuleFileState state} for the specified {@code nodeId}.<br/>
     * If the module doesn't have a state for the specified {@code nodeId} a new {@link ServerModuleFileState state}
     * will be created and added to the {@link #states} collection.<br/>
     * If error message is set for {@code nodeId} which doesn't have any previous state, then a new
     * {@link ServerModuleFileState state} object is created with {@link ModuleState#UPLOADED} state
     * and specified {@code errorMessage}.
     *
     * @param nodeId            The OID of the owning node to update the state.  Required.
     * @param moduleState       updated {@link ModuleState state}.  Optional.
     * @param errorMessage      updated error-message.  Optional.
     */
    private void updateState(@NotNull final String nodeId, @Nullable final ModuleState moduleState, @Nullable final String errorMessage) {
        if (states == null) {
            states = new ArrayList<>();
        }

        boolean foundStateForNode = false;
        for (final ServerModuleFileState state : states) {
            if (nodeId.equals(state.getNodeId())) {
                if (moduleState != null) {
                    state.setState(moduleState);
                }
                state.setErrorMessage(errorMessage);
                foundStateForNode = true;
                break;
            }
        }
        if (!foundStateForNode) {
            final ServerModuleFileState state = new ServerModuleFileState(this);
            state.setNodeId(nodeId);
            // shouldn't really happen but in case there is no state for the specified node use UPLOADED
            state.setState(moduleState != null ? moduleState : ModuleState.UPLOADED);
            state.setErrorMessage(errorMessage);
            states.add(state);
        }
    }


    /**
     * Copy data from specified {@code otherModule}.
     *
     * @param otherModule            the module from which to copy properties.  Required.
     * @param includeData            flag indicating whether to copy data bytes and signature properties from the specified module.
     * @param includeFileMetadata    flag indicating whether to copy module metadata (i.e. sha-256 and properties) from the specified module.
     * @param includeStates          flag indicating whether to copy all module states from the specified module.
     */
    public void copyFrom(
            @NotNull final ServerModuleFile otherModule,
            final boolean includeData,
            final boolean includeFileMetadata,
            final boolean includeStates
    ) {
        setGoid(otherModule.getGoid());
        setVersion(otherModule.getVersion());
        setName(otherModule.getName());
        setModuleType(otherModule.getModuleType());

        if (includeFileMetadata) {
            setModuleSha256(otherModule.getModuleSha256());
            for (final String key : ALL_PROPERTY_KEYS) {
                setProperty(key, otherModule.getProperty(key));
            }
        }

        createData(includeData ? otherModule.getData() : null);

        if (includeStates) {
            List<ServerModuleFileState> newStates = null;
            final List<ServerModuleFileState> otherStates = otherModule.getStates();
            if (otherStates != null) {
                newStates = new ArrayList<>();
                for (final ServerModuleFileState state : otherStates) {
                    final ServerModuleFileState newState = new ServerModuleFileState(this);
                    newState.copyFrom(state);
                    newStates.add(newState);
                }
            }
            setStates(newStates);
        }
    }

    /**
     * Convenient method for representing a {@link #PROP_SIZE file size} property into human readable format.
     *
     * @return a {@code String} containing a human readable format of the bytes. Never {@code null}.
     */
    @Transient
    public String getHumanReadableFileSize() {
        try {
            return humanReadableBytes(Long.parseLong(getProperty(PROP_SIZE)));
        } catch (final NumberFormatException e) {
            return StringUtils.EMPTY;
        }

    }

    /**
     * Utility function for representing a file size into human readable format.
     *
     * @param bytes    file size in bytes.
     * @return a {@code String} containing a human readable format of the bytes. Never {@code null}.
     */
    @NotNull
    public static String humanReadableBytes(final long bytes) {
        final int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        //noinspection SpellCheckingInspection
        return String.format("%.1f %cB", bytes / Math.pow(unit, exp), "KMGTPE".charAt(exp - 1));
    }

    /**
     * Utility function for gathering all known property keys used by the {@code ServerModuleFile}.
     *
     * @return a {@code String} array containing all known property keys used by the {@code ServerModuleFile}, never {@code null}.
     * @see #ALL_PROPERTY_KEYS
     */
    @NotNull
    public static String[] getPropertyKeys() {
        return ALL_PROPERTY_KEYS;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final ServerModuleFile that = (ServerModuleFile) o;

        if (moduleType != null ? !moduleType.equals(that.moduleType) : that.moduleType != null) return false;
        if (moduleSha256 != null ? !moduleSha256.equals(that.moduleSha256) : that.moduleSha256 != null) return false;
        // states are ignored
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (moduleType != null ? moduleType.hashCode() : 0);
        result = 31 * result + (moduleSha256 != null ? moduleSha256.hashCode() : 0);
        return result;
    }
}
