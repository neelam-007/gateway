package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Assertion bean representing an invocation of server behavior for an EncapsulatedAssertionConfig.
 */
public class EncapsulatedAssertion extends Assertion implements UsesEntitiesAtDesignTime, UsesVariables, SetsVariables {

    private ZoneableGuidEntityHeader configHeader = new ZoneableGuidEntityHeader();
    private Map<String,String> parameters = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);

    private transient DefaultAssertionMetadata meta = null;
    private transient EncapsulatedAssertionConfig encapsulatedAssertionConfig;

    /**
     * Create an EncapsulatedAssertion bean with no backing config.
     */
    public EncapsulatedAssertion() {
    }

    /**
     * Create an EncapsulatedAssertion bean backed by the specifiec config.
     *
     * @param encapsulatedAssertionConfig config to provide metadata and behavior for this assertion.
     */
    public EncapsulatedAssertion(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
        config(encapsulatedAssertionConfig);
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override
    public Object clone() {
        EncapsulatedAssertion copy = (EncapsulatedAssertion) super.clone();

        //noinspection unchecked
        copy.parameters = (Map<String, String>) ((TreeMap<String, String>)parameters).clone();
        copy.configHeader = new ZoneableGuidEntityHeader(configHeader.getStrId(), EntityType.ENCAPSULATED_ASSERTION, configHeader.getName(), configHeader.getDescription(), configHeader.getVersion());
        copy.configHeader.setGuid(configHeader.getGuid());
        copy.encapsulatedAssertionConfig = encapsulatedAssertionConfig == null ? null : encapsulatedAssertionConfig.getCopy();
        copy.meta = null;

        return copy;
    }

    /**
     * @return the EncapsulatedAssertionConfig that provides metadata and behavior for this assertion, or null if not currently available.
     */
    @Nullable
    @Migration(dependency = false)
    public EncapsulatedAssertionConfig config() {
        return encapsulatedAssertionConfig;
    }

    /**
     * Set the EncapsulatedAssertionConfig instance that will provide metadata and behavior for this assertion.
     * <p/>
     * Calling this method will cause assertion metadata to be regenerated next time meta() is called.
     * <p/>
     * Clearing the config will also null out any cached config name and GUID.
     *
     * @param config new config, or null to clear it.
     */
    public void config(@Nullable EncapsulatedAssertionConfig config) {
        this.encapsulatedAssertionConfig = config;
        if (config == null) {
            configHeader.setGoid(EncapsulatedAssertionConfig.DEFAULT_GOID);
            configHeader.setGuid(null);
            configHeader.setName(null);
            configHeader.setVersion(null);
        } else {
            configHeader.setGoid(config.getGoid());
            configHeader.setGuid(config.getGuid());
            configHeader.setName(config.getName());
            configHeader.setVersion(config.getVersion());
        }
        meta = null; // Force metadata to be rebuilt
    }

    /**
     * @return the GUID of the encapsulated assertion config that will provide metadata and behavior for this assertion.
     */
    @Nullable
    public String getEncapsulatedAssertionConfigGuid() {
        return configHeader.getGuid();
    }

    /**
     * Set the encapsulated assertion config GUID.  This method should not be used except for serialization purposes.
     * The config GUID is updated when the {@link #config(com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig)} method
     * is called to set a config.
     *
     * @param encapsulatedAssertionConfigGuid the GUID string of the encapsulated assertion config to provide metadata and behavior for this assertion.
     */
    public void setEncapsulatedAssertionConfigGuid(String encapsulatedAssertionConfigGuid) {
        configHeader.setGuid(encapsulatedAssertionConfigGuid);
    }

    /**
     * @return the name of the encapsulated assertion config intended to provide metadata and behavior for this assertion, or null.
     */
    @Nullable
    public String getEncapsulatedAssertionConfigName() {
        return configHeader.getName();
    }

    /**
     * Set the encapsulated assertion config name.  This method should not be used except for serialization purposes.
     * The config name is updated when the {@link #config(com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig)} method
     * is called to set a config.
     *
     * @param encapsulatedAssertionConfigName the name of the encapsulated assertion config intended to provide metadata and behavior for this assertion
     */
    public void setEncapsulatedAssertionConfigName(String encapsulatedAssertionConfigName) {
        configHeader.setName(encapsulatedAssertionConfigName);
    }

    /**
     * @return the names of all assertion parameters stored within this assertion bean, for use by the corresponding server assertion instance.
     */
    @NotNull
    public Set<String> getParameterNames() {
        return parameters.keySet();
    }

    /**
     * Get the value of a parameter.
     *
     * @param key the parameter name.  Required.
     * @return the value of the parameter or null if not found.
     */
    @Nullable
    public String getParameter(@NotNull String key) {
        return parameters.get(key);
    }

    /**
     * Set the value of a parameter.
     *
     * @param key the parameter name.  Required.
     * @param value the value of the parameter.  Required.
     */
    public void putParameter(@NotNull String key, @NotNull String value) {
        parameters.put(key, value);
    }

    /**
     * Remove a parameter from the parameter map.
     *
     * @param key the parameter name.  Required.
     * @return the previous value of the removed parameter, or null if there way no previous value.
     */
    public String removeParameter(@NotNull String key) {
        return parameters.remove(key);
    }

    /**
     * Get raw access to the current parameters map.
     * <p/>
     * The parameters map is used to configure values that are per-assertion-instance, typically configured in the SSM GUI,
     * rather than those that arrive at runtime via context variables.
     * <p/>
     * This method should not be used directly.
     *
     * @return the current parameters map.
     * @deprecated this is intended for WSP serialization purposes only.  Use {@link #getParameterNames()}, {@link #getParameter(String)}, {@link #putParameter(String, String)}, and {@link #removeParameter} instead.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Replace the current parameters map.
     * <p/>
     * The parameters map is used to configure values that are per-assertion-instance, typically configured in the SSM GUI,
     * rather than those that arrive at runtime via context variables.
     * <p/>
     * This method should not be used directly.
     *
     * @param parameters the new parameters map.  if null, all parameters will be cleared.
     * @deprecated this is intended for WSP serialization purposes only.  Use {@link #getParameterNames()}, {@link #getParameter(String)}, {@link #putParameter(String, String)}, and {@link #removeParameter} instead.
     */
    public void setParameters(@Nullable Map<String, String> parameters) {
        this.parameters.clear();
        if (parameters != null)
            this.parameters.putAll(parameters);
    }

    /**
     * Migration mapping not supported for EncapsulatedAssertionConfig (it should already exist on the target SSG).
     */
    @Override
    @Migration(export = false, mapName = MigrationMappingSelection.NONE)
    public EntityHeader[] getEntitiesUsed() {
        return configHeader.getGuid() == null
            ? new EntityHeader[0]
            : new EntityHeader[] { makeConfigHeader() };
    }

    private GuidEntityHeader makeConfigHeader() {
        ZoneableGuidEntityHeader ret = new ZoneableGuidEntityHeader(configHeader.getGoid().toString(), EntityType.ENCAPSULATED_ASSERTION, configHeader.getName(), null, configHeader.getVersion());
        ret.setGuid(configHeader.getGuid());
        return ret;
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (newEntityHeader != null && EntityType.ENCAPSULATED_ASSERTION.equals(newEntityHeader.getType()) && newEntityHeader instanceof GuidEntityHeader) {
            final String newGuid = ((GuidEntityHeader) newEntityHeader).getGuid();
            final EncapsulatedAssertionConfig thisConfig = config();
            if (thisConfig != null) {
                // If we have a cached config entity, clear it if the new GUID is different
                if (!newGuid.equals(thisConfig.getGuid())) {
                    config(null);
                }
            }
            configHeader.setGoid(newEntityHeader.getGoid());
            configHeader.setGuid(newGuid);
            configHeader.setName(newEntityHeader.getName());
            configHeader.setVersion(newEntityHeader.getVersion());
        }
    }

    @Override
    public AssertionMetadata meta() {
        if (this.meta != null)
            return this.meta;

        // Avoid using super.defaultMeta() because this assertion uses different metadata for each instance
        DefaultAssertionMetadata meta = new DefaultAssertionMetadata(this);

        EncapsulatedAssertionConfig config = this.encapsulatedAssertionConfig != null
            ? this.encapsulatedAssertionConfig
            : newConfigFromHeader(configHeader);

        meta.put(SHORT_NAME, config.getName());
        meta.put(BASE_64_NODE_IMAGE, config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_BASE64));
        meta.put(PALETTE_NODE_ICON, findIconResourcePath(config));

        if (config.hasAtLeastOneGuiParameter()) {
            // Use dialog
            meta.put(PROPERTIES_EDITOR_FACTORY, null);
            meta.put(PROPERTIES_ACTION_CLASSNAME, null);
            meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.encass.EncapsulatedAssertionPropertiesDialog");
        } else {
            // Disable properties editor
            meta.putNull(PROPERTIES_ACTION_CLASSNAME);
            meta.putNull(PROPERTIES_EDITOR_FACTORY);
            meta.putNull(PROPERTIES_EDITOR_CLASSNAME);
        }

        meta.put(ASSERTION_FACTORY, new Functions.Unary< EncapsulatedAssertion, EncapsulatedAssertion >() {
            @Override
            public EncapsulatedAssertion call(EncapsulatedAssertion encapsulatedAssertion) {
                EncapsulatedAssertionConfig config = encapsulatedAssertion.config();
                if (config == null)
                    return new EncapsulatedAssertion();
                return new EncapsulatedAssertion(config);
            }
        });

        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.EncapsulatedAssertionValidator");
        meta.put(PALETTE_NODE_CLASSNAME, "com.l7tech.console.tree.EncapsulatedAssertionNode");

        // Copy over properties that require some adaptation
        final String folder = config.getProperty(EncapsulatedAssertionConfig.PROP_PALETTE_FOLDER);
        if (folder != null) {
            meta.put(PALETTE_FOLDERS, new String[] { folder });
        }

        final String description = config.getProperty(EncapsulatedAssertionConfig.PROP_DESCRIPTION);
        if (description != null) {
            meta.put(DESCRIPTION, description);
        }

        meta.put( POLICY_ADVICE_CLASSNAME, "auto" );

        this.meta = meta;
        return meta;
    }

    private static EncapsulatedAssertionConfig newConfigFromHeader(GuidEntityHeader header) {
        EncapsulatedAssertionConfig ret = new EncapsulatedAssertionConfig();
        ret.setGoid(header.getGoid());
        ret.setGuid(header.getGuid());
        ret.setName(header.getName());
        return ret;
    }

    private String findIconResourcePath(EncapsulatedAssertionConfig config) {
        String filename = config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_RESOURCE_FILENAME);
        if (filename != null)
            return EncapsulatedAssertionConfig.ICON_RESOURCE_DIRECTORY + filename;

        return EncapsulatedAssertionConfig.ICON_RESOURCE_DIRECTORY + EncapsulatedAssertionConfig.DEFAULT_ICON_RESOURCE_FILENAME;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        EncapsulatedAssertionConfig config = config();
        if (config == null)
            return new VariableMetadata[0];

        List<VariableMetadata> ret = new ArrayList<VariableMetadata>();
        Set<EncapsulatedAssertionResultDescriptor> outputs = config.getResultDescriptors();
        for (EncapsulatedAssertionResultDescriptor output : outputs) {
            ret.add(new VariableMetadata(output.getResultName(), false, true, output.getResultName(), true, DataType.forName(output.getResultType())));
        }
        return ret.toArray(new VariableMetadata[ret.size()]);
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return getVariablesUsed(this, config());
    }

    /**
     * Get the variables that would be declared used by the specified assertion, with its current parameters, if
     * it were backed by the specified config entity.
     *
     * @param assertion the assertion to provide parameter values.  Required.
     * @param config the config entity.  May be null.
     * @return variables used.  May be empty but never null.
     */
    @NotNull
    public static String[] getVariablesUsed(@NotNull EncapsulatedAssertion assertion, @Nullable EncapsulatedAssertionConfig config) {
        if (config == null)
            return new String[0];

        Set<String> ret = new LinkedHashSet<String>();
        for (EncapsulatedAssertionArgumentDescriptor descriptor : config.getArgumentDescriptors()) {
            final String argumentName = descriptor.getArgumentName();
            if (descriptor.isGuiPrompt()) {
                // Parameter is specified in per-instance parameter map, so shouldn't itself be advertised in getVariablesUsed, but its value might use context variables.
                ret.addAll(Arrays.asList(descriptor.getVariablesUsed(assertion.getParameter(argumentName))));
            } else {
                // Parameter is specified as context variable.  Advertise in getVariablesUsed.
                ret.add(argumentName);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }


    @Override
    @Migration(dependency = false)
    public EntityHeader[] getEntitiesUsedAtDesignTime() {
        return getEntitiesUsed();
    }

    @Override
    public boolean needsProvideEntity(@NotNull EntityHeader header) {
        if (EntityType.ENCAPSULATED_ASSERTION.equals(header.getType())) {
            return config() == null;
        }
        return false;
    }

    @Override
    public void provideEntity(@NotNull EntityHeader header, @NotNull Entity entity) {
        if (entity instanceof EncapsulatedAssertionConfig) {
            config((EncapsulatedAssertionConfig) entity);
        }
    }

    @Override
    public void updateTemporaryData(Assertion assertion) {
        if (assertion instanceof EncapsulatedAssertion) {
            EncapsulatedAssertion other = (EncapsulatedAssertion) assertion;
            final EncapsulatedAssertionConfig config = other.config();
            this.config(config);
            if (config == null) {
                this.configHeader.setGoid(other.configHeader.getGoid());
                this.configHeader.setGuid(other.configHeader.getGuid());
                this.configHeader.setName(other.configHeader.getName());
            }
        }
    }
}
