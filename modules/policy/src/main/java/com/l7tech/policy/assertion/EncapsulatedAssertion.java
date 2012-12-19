package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
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
    static final String DEFAULT_OID_STR = Long.toString(EncapsulatedAssertionConfig.DEFAULT_OID);

    private DefaultAssertionMetadata meta = null;
    private String encapsulatedAssertionConfigId = DEFAULT_OID_STR;

    private Map<String,String> parameters = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);

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
        copy.encapsulatedAssertionConfig = encapsulatedAssertionConfig == null ? null : encapsulatedAssertionConfig.getCopy();
        copy.meta = null;

        return copy;
    }

    /**
     * @return the EncapsulatedAssertionConfig that provides metadata and behavior for this assertion, or null if not currently available.
     */
    public EncapsulatedAssertionConfig config() {
        return encapsulatedAssertionConfig;
    }

    /**
     * Set the EncapsulatedAssertionConfig instance that will provide metadata and behavior for this assertion.
     * <p/>
     * Calling this method will cause assertion metadata to be regenerated next time meta() is called.
     *
     * @param config new config, or null to clear it.
     */
    public void config(EncapsulatedAssertionConfig config) {
        this.encapsulatedAssertionConfig = config;
        if (config != null)
            this.encapsulatedAssertionConfigId = config.getId();
        meta = null; // Force metadata to be rebuilt
    }

    /**
     * @return the OID string of the encapsulated assertion config that will provide metadata and behavior for this assertion.
     */
    public String getEncapsulatedAssertionConfigId() {
        return encapsulatedAssertionConfigId;
    }

    /**
     * @param encapsulatedAssertionConfigId the OID string of the encapsulated assertion config to provide metadata and behavior for this assertion.
     */
    public void setEncapsulatedAssertionConfigId(String encapsulatedAssertionConfigId) {
        this.encapsulatedAssertionConfigId = encapsulatedAssertionConfigId;
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
    public void setParameters(Map<String, String> parameters) {
        this.parameters.clear();
        if (parameters != null)
            this.parameters.putAll(parameters);
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        return encapsulatedAssertionConfigId == null || DEFAULT_OID_STR.equals(encapsulatedAssertionConfigId)
            ? new EntityHeader[0]
            : new EntityHeader[] { new EntityHeader(encapsulatedAssertionConfigId, EntityType.ENCAPSULATED_ASSERTION, null, null) };
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (newEntityHeader != null && EntityType.ENCAPSULATED_ASSERTION.equals(newEntityHeader.getType())) {
            encapsulatedAssertionConfigId = newEntityHeader.getStrId();
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
            : new EncapsulatedAssertionConfig();

        meta.put(SHORT_NAME, config.getName());
        meta.put(BASE_64_NODE_IMAGE, config.getProperty(EncapsulatedAssertionConfig.PROP_ICON_BASE64));
        meta.put(PALETTE_NODE_ICON, findIconResourcePath(config));

        if (config.hasAtLeastOneGuiParameter()) {
            // Use dialog
            meta.put(PROPERTIES_EDITOR_FACTORY, null);
            meta.put(PROPERTIES_ACTION_CLASSNAME, null);
            meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.EncapsulatedAssertionPropertiesDialog");
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
                return new EncapsulatedAssertion(config.getReadOnlyCopy());
            }
        });

        // Copy over properties that require some adaptation
        final String folder = config.getProperty(EncapsulatedAssertionConfig.PROP_PALETTE_FOLDER);
        if (folder != null) {
            meta.put(PALETTE_FOLDERS, new String[] { folder });
        }

        final String description = config.getProperty(EncapsulatedAssertionConfig.PROP_DESCRIPTION);
        if (description != null) {
            meta.put(DESCRIPTION, description);
        }

        this.meta = meta;
        return meta;
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
            ret.add(new VariableMetadata(output.getResultName(), false, false, output.getResultName(), true, DataType.forName(output.getResultType())));
        }
        return ret.toArray(new VariableMetadata[ret.size()]);
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        EncapsulatedAssertionConfig config = config();
        if (config == null)
            return new String[0];

        Set<String> ret = new LinkedHashSet<String>();
        for (EncapsulatedAssertionArgumentDescriptor descriptor : config.getArgumentDescriptors()) {
            final String argumentName = descriptor.getArgumentName();
            if (descriptor.isGuiPrompt()) {
                // Parameter is specified in per-instance parameter map, so shouldn't itself be advertised in getVariablesUsed, but its value might use context variables.
                ret.addAll(Arrays.asList(descriptor.getVariablesUsed(getParameter(argumentName))));
            } else {
                // Parameter is specified as context variable.  Advertise in getVariablesUsed.
                ret.add(argumentName);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    @Override
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
            EncapsulatedAssertion encapsulatedAssertion = (EncapsulatedAssertion) assertion;
            this.config(encapsulatedAssertion.config());
        }
    }
}
