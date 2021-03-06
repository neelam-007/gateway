package com.l7tech.objectmodel.encass;

import com.l7tech.objectmodel.GuidEntity;
import com.l7tech.objectmodel.JaxbMapType;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.policy.Policy;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.Functions;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A persistent entity that holds information about a user-defined assertion with behavior backed by an editable policy fragment.
 * <p/>
 * This is an assertion whose server-side runtime implementation consists of invoking a policy fragment.
 * Unlike a regular Include assertion, a UserDefinedAssertion behaves like a regular assertion on the palette,
 * with implementation hiding, configurable assertion icon, tree node labels, configuration GUI with per-assertion-instance properties,
 * and runtime that uses a nested (child) policy enforcement context.
 */
@SuppressWarnings("Convert2Diamond")
@XmlRootElement(name = "EncapsulatedAssertion")
@XmlType(propOrder = {"policy", "argumentDescriptors", "resultDescriptors", "properties"})
@Entity
@Proxy(lazy=false)
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@Table(name="encapsulated_assertion")
public class EncapsulatedAssertionConfig extends ZoneableNamedEntityImp implements GuidEntity {
    /** ID of palette folder in which to offer the encapsulated assertion. */
    public static final String PROP_PALETTE_FOLDER = "paletteFolder";

    /** Description of the encapsulated assertion, if not the default. */
    public static final String PROP_DESCRIPTION = "description";

    /** Base64-encoded image in a supported format (gif, png, or jpg).  Should be 16x16 pixels, for the time being. */
    public static final String PROP_ICON_BASE64 = "paletteIconBase64";

    /** Hash representing the configuration of the encapsulated assertion and its backing policy (used for import and export). */
    public static final String PROP_ARTIFACT_VERSION = "artifactVersion";

    /** The backing Policy guid (can be used to retrieve the backing Policy if it is detached) **/
    public static final String PROP_POLICY_GUID = "policyGuid";

    /** Base resource path in which icon file resource names are searched for. */
    public static final String ICON_RESOURCE_DIRECTORY = "com/l7tech/console/resources/";

    /** Filename (including extension, but not including full path) of icon resource under {@link #ICON_RESOURCE_DIRECTORY}; or null if not specified. */
    public static final String PROP_ICON_RESOURCE_FILENAME = "paletteIconResourceName";

    /** The default icon to use for an encapsulated assertion that doesn't specify a different one. */
    public static final String DEFAULT_ICON_RESOURCE_FILENAME = "star16.gif";

    /** Flag to indicate if debug tracing is allowed into the encapsulated assertion backing policy. */
    public static final String PROP_ALLOW_TRACING = "allowTracing";

    /** Flag to indicate whether this assertion should pass any routing metrics to its parent. */
    public static final String PROP_PASS_METRICS_TO_PARENT = "passMetricsToParent";

    /**
     * Optional template for displaying a custom node label in the policy tree.  Template may include
     * variables:
     * <pre>
     *   ${meta.name} - base name of assertion
     *   ${in.FOO.value} - value of input variable named FOO, if known at design time, otherwise empty string
     *   ${in.FOO.label} - GUI label for input variable named FOO, or just the name ("FOO" in this case) if no GUI label set
     *   ${in.FOO.type} - Data type for input variable named FOO
     *   ${in.0.value} - value of first argument, if known at design time, otherwise empty string
     *   ${in.1.name} - name of second argument, if known at design time, otherwise empty string
     *   ${in.2.label} - GUI label of third argument, if known at design time, otherwise empty string
     *   ${in.3.type} - Data type of third argument
     * </pre>
     */
    public static final String PROP_POLICY_NODE_NAME_TEMPLATE = "policyNodeNameTemplate";

    /** Name of a registered policy-backed service interface, or null. */
    public static final String PROP_SERVICE_INTERFACE = "serviceInterface";

    /** Name of a method in a registered policy-backed service interface, or null. */
    public static final String PROP_SERVICE_METHOD = "serviceMethod";

    /** The string "true" if this encapsulated assertion config was created on the fly as a service method descriptor, and should not be persisted. */
    public static final String PROP_EPHEMERAL = "ephemeral";

    private String guid;
    private Policy policy;
    private Set<EncapsulatedAssertionArgumentDescriptor> argumentDescriptors = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
    private Set<EncapsulatedAssertionResultDescriptor> resultDescriptors = new HashSet<EncapsulatedAssertionResultDescriptor>();
    private Map<String,String> properties = new HashMap<String,String>();

    /**
     * Sets the name of the encapsulated assertion. This is here to enforce the name length
     * @return The encapsulated assertion name.
     */
    @RbacAttribute
    @Size(min=1,max=128)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * @return the GUID for this encapsulated assertion configuration, or null if not yet assigned.
     */
    @RbacAttribute
    @Nullable
    @Column(name="guid", nullable=false, length=255)
    @XmlAttribute(name = "guid")
    public String getGuid() {
        return guid;
    }

    /**
     * Assign a GUID for this encapsulated assertion configuration.  A GUID should only be assigned to a persisted
     * EncapsulatedAssertion, and then only when it is first saved.
     *
     * @param guid GUID to use for this encapsulated assertion configuration.
     */
    public void setGuid(String guid) {
        checkLocked();
        this.guid = guid;
    }

    /**
     * Get the associated Policy entity that provides the runtime implementation for this encapsulated assertion.
     *
     * Migration of backing policy not supported. EncapsulatedAssertionConfigs and backing policies must be manually imported and exported via SSM.
     *
     * @return the policy, or null if not set.
     */
    @Valid
    @Migration(dependency = true)
    @ManyToOne
    @JoinColumn(name = "policy_goid")
    @XmlElement (name = "Policy")
    @XmlJavaTypeAdapter(PolicyAdapter.class)
    @Nullable
    public Policy getPolicy() {
        return policy;
    }

    /**
     * Sets the policy and the policyOid property.
     */
    public void setPolicy(@Nullable Policy policy) {
        checkLocked();
        this.policy = policy;
        if (policy != null && policy.getGuid() != null) {
            putProperty(PROP_POLICY_GUID, policy.getGuid());
        } else {
            removeProperty(PROP_POLICY_GUID);
        }
    }

    /**
     * Nulls the policy after first updating the policy OID property.
     */
    public void detachPolicy() {
        if (policy != null && policy.getGuid() != null) {
            putProperty(PROP_POLICY_GUID, policy.getGuid());
        }
        this.policy = null;
    }

    /**
     * Get the declared parameter values for this encapsulated assertion.
     *
     * @return the parameters.  May be empty but never null.
     */
    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="encapsulatedAssertionConfig", orphanRemoval=true)
    @XmlElementWrapper(name = "EncapsulatedAssertionArguments")
    @XmlElement(name = "EncapsulatedAssertionArgument")
    @Migration(dependency = false)
    public Set<EncapsulatedAssertionArgumentDescriptor> getArgumentDescriptors() {
        return argumentDescriptors;
    }

    public void setArgumentDescriptors(@Nullable Set<EncapsulatedAssertionArgumentDescriptor> argumentDescriptors) {
        checkLocked();
        this.argumentDescriptors = argumentDescriptors;
    }

    /**
     * Get the declared return values for this encapsulated assertion.
     *
     * @return the results.  May be empty but never null.
     */
    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="encapsulatedAssertionConfig", orphanRemoval=true)
    @XmlElementWrapper(name = "EncapsulatedAssertionResults")
    @XmlElement(name = "EncapsulatedAssertionResult")
    @Migration(dependency = false)
    public Set<EncapsulatedAssertionResultDescriptor> getResultDescriptors() {
        return resultDescriptors;
    }

    public void setResultDescriptors(Set<EncapsulatedAssertionResultDescriptor> resultDescriptors) {
        checkLocked();
        this.resultDescriptors = resultDescriptors;
    }

    /**
     * Get an arbitrary property for this encapsulated assertion.
     *
     * @param key name of property.  Required.
     * @return property value, or null if not set.
     */
    @Nullable
    @Transient
    public String getProperty(@NotNull String key) {
        return properties.get(key);
    }

    /**
     * Convenience method to get a property as boolean.
     *
     * @param propertyName the name of the property to get
     * @return boolean represented by the requested property value
     */
    @Transient
    public boolean getBooleanProperty(@NotNull final String propertyName) {
        return Boolean.parseBoolean(getProperty(propertyName));
    }

    /**
     * Set an arbitrary property for this encapsulated assertion.
     *
     * @param key name of property.  Required.
     * @param value value of property.  May not be null.
     */
    public void putProperty(@NotNull String key, @NotNull String value) {
        checkLocked();
        properties.put(key, value);
    }

    /**
     * Convenience method to set a boolean property.
     *
     * @param propertyName name of property.  Required.
     * @param propertyValue value of property
     */
    @Transient
    public void putBooleanProperty(@NotNull String propertyName, boolean propertyValue) {
        putProperty(propertyName, Boolean.toString(propertyValue));
    }

    /**
     * Get names of arbitrary properties for this encapsulated assertion.
     *
     * @return set of property names.  May be empty but never null.
     */
    @NotNull
    @Transient
    public Set<String> getPropertyNames() {
        return new HashSet<String>(properties.keySet());
    }

    /**
     * Delete an arbitrary property for this encapsulated assertion.
     *
     * @param key name of property.  Required.
     */
    public void removeProperty(@NotNull String key) {
        checkLocked();
        properties.remove(key);
    }

    /**
     * Get the extra properties of this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @return a Set containing the extra connector properties.  May be empty but never null.
     */
    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection(fetch=FetchType.EAGER)
    @JoinTable(name="encapsulated_assertion_property",
        joinColumns=@JoinColumn(name="encapsulated_assertion_goid", referencedColumnName="goid"))
    @MapKeyColumn(name="name",length=128)
    @Column(name="value", nullable=false, length=32672)
    @XmlElement(name = "Properties")
    @XmlJavaTypeAdapter(JaxbMapType.JaxbMapTypeAdapter.class)
    public Map<String,String> getProperties() {
        return properties;
    }

    /**
     * Set the extra properties for this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @param properties the properties set to use
     */
    public void setProperties(Map<String,String> properties) {
        checkLocked();
        this.properties = properties;
    }

    /**
     * @return true if this config has at least one argument descriptor marked as showInGui=true.
     */
    public boolean hasAtLeastOneGuiParameter() {
        Set<EncapsulatedAssertionArgumentDescriptor> args = getArgumentDescriptors();
        if (args == null)
            return false;
        for (EncapsulatedAssertionArgumentDescriptor arg : argumentDescriptors) {
            if (arg.isGuiPrompt())
                return true;
        }
        return false;
    }

    /**
     * Convenience method that returns the argument descriptors sorted by ordinal.
     *
     * @return a List of argument descriptors, sorted by ordinal, with the variable name used as a tie-breaker.
     */
    @Migration(dependency = false)
    public List<EncapsulatedAssertionArgumentDescriptor> sortedArguments() {
        return Functions.sort(getArgumentDescriptors(), new Comparator<EncapsulatedAssertionArgumentDescriptor>() {
            @Override
            public int compare(EncapsulatedAssertionArgumentDescriptor a, EncapsulatedAssertionArgumentDescriptor b) {
                int result = Integer.valueOf(a.getOrdinal()).compareTo(b.getOrdinal());
                if (result != 0)
                    return result;
                return a.getArgumentName().compareTo(b.getArgumentName());
            }
        });
    }

    @Transient
    @Migration(dependency = false)
    public EncapsulatedAssertionConfig getCopy() {
        return getCopy(false);
    }

    @Transient
    @Migration(dependency = false)
    private EncapsulatedAssertionConfig getCopy(boolean readOnly) {
        //noinspection TryWithIdenticalCatches
        try {
            EncapsulatedAssertionConfig copy = new EncapsulatedAssertionConfig();
            BeanUtils.copyProperties(this, copy,
                BeanUtils.omitProperties(BeanUtils.getProperties(getClass()), "properties", "argumentDescriptors", "resultDescriptors", "policy"));
            copy.setProperties(new HashMap<String, String>(getProperties()));

            final HashSet<EncapsulatedAssertionArgumentDescriptor> args = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
            Set<EncapsulatedAssertionArgumentDescriptor> ourArgs = getArgumentDescriptors();
            if (ourArgs != null) {
                for (EncapsulatedAssertionArgumentDescriptor ourArg : ourArgs) {
                    args.add(ourArg.getCopy(copy, readOnly));
                }
            }
            copy.setArgumentDescriptors(args);

            final HashSet<EncapsulatedAssertionResultDescriptor> results = new HashSet<EncapsulatedAssertionResultDescriptor>();
            Set<EncapsulatedAssertionResultDescriptor> ourResults = getResultDescriptors();
            if (ourResults != null) {
                for (EncapsulatedAssertionResultDescriptor ourResult : ourResults) {
                    results.add(ourResult.getCopy(copy, readOnly));
                }
            }
            copy.setResultDescriptors(results);

            Policy policy = getPolicy();
            copy.setPolicy(policy == null ? null : new Policy(policy, null, readOnly, null));
            if (readOnly)
                copy.lock();
            return copy;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Transient
    @Migration(dependency = false)
    public EncapsulatedAssertionConfig getReadOnlyCopy() {
        EncapsulatedAssertionConfig copy = getCopy(true);
        copy.setReadOnly();
        return copy;
    }

    /**
     * Initialize any lazily-computed fields and mark this instance as read-only.
     */
    private void setReadOnly() {
        this.getArgumentDescriptors();
        this.getResultDescriptors();
        this.getPropertyNames();
        this.lock();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncapsulatedAssertionConfig)) return false;
        if (!super.equals(o)) return false;

        EncapsulatedAssertionConfig that = (EncapsulatedAssertionConfig) o;

        return !(guid != null ? !guid.equals(that.guid) : that.guid != null);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EncapsulatedAssertionConfig{" +
            "goid=" + getGoid() +
            ", guid='" + getGuid() + "'" +
            ", name='" + getName() + "'" +
            ", policy=" + policy + "\n" +
            ", argumentDescriptors=" + argumentDescriptors + "\n" +
            ", resultDescriptors=" + resultDescriptors + "\n" +
            ", properties=" + properties +
            "}\n";
    }
}
