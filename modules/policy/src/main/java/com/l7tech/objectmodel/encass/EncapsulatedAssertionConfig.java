package com.l7tech.objectmodel.encass;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
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
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

/**
 * A persistent entity that holds information about a user-defined assertion with behavior backed by an editable policy fragment.
 * <p/>
 * This is an assertion whose server-side runtime implementation consists of invoking a policy fragment.
 * Unlike a regular Include assertion, a UserDefinedAssertion behaves like a regular assertion on the palette,
 * with implementation hiding, configurable assertion icon, tree node labels, configuration GUI with per-assertion-instance properties,
 * and runtime that uses a nested (child) policy enforcement context.
 */
@SuppressWarnings("Convert2Diamond")
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@Table(name="encapsulated_assertion")
public class EncapsulatedAssertionConfig extends NamedEntityImp {
    /** ID of palette folder in which to offer the encapsulated assertion. */
    public static final String PROP_PALETTE_FOLDER = "paletteFolder";

    /** Description of the encapsulated assertion, if not the default. */
    public static final String PROP_DESCRIPTION = "description";

    /** Base64-encoded image in a supported format (gif, png, or jpg).  Should be 16x16 pixels, for the time being. */
    public static final String PROP_ICON_BASE64 = "paletteIconBase64";

    /** Base resource path in which icon file resource names are searched for. */
    public static final String ICON_RESOURCE_DIRECTORY = "com/l7tech/console/resources/";

    /** Filename (including extension, but not including full path) of icon resource under {@link #ICON_RESOURCE_DIRECTORY}; or null if not specified. */
    public static final String PROP_ICON_RESOURCE_FILENAME = "paletteIconResourceName";

    /** The default icon to use for an encapsulated assertion that doesn't specify a different one. */
    public static final String DEFAULT_ICON_RESOURCE_FILENAME = "star16.gif";

    private String guid;
    private Policy policy;
    private Set<EncapsulatedAssertionArgumentDescriptor> argumentDescriptors = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
    private Set<EncapsulatedAssertionResultDescriptor> resultDescriptors = new HashSet<EncapsulatedAssertionResultDescriptor>();
    private Map<String,String> properties = new HashMap<String,String>();

    /**
     * @return the GUID for this encapsulated assertion configuration, or null if not yet assigned.
     */
    @Nullable
    @Column(name="guid", nullable=false, length=255)
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
     * @return the policy, or null if not set.
     */
    @Valid
    @Migration(mapName = NONE, mapValue = NONE, resolver = PropertyResolver.Type.POLICY)
    @ManyToOne
    @JoinColumn(name = "policy_oid")
    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        checkLocked();
        this.policy = policy;
    }

    /**
     * Get the declared parameter values for this encapsulated assertion.
     *
     * @return the parameters.  May be empty but never null.
     */
    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="encapsulatedAssertionConfig", orphanRemoval=true)
    public Set<EncapsulatedAssertionArgumentDescriptor> getArgumentDescriptors() {
        return argumentDescriptors;
    }

    public void setArgumentDescriptors(Set<EncapsulatedAssertionArgumentDescriptor> argumentDescriptors) {
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
        joinColumns=@JoinColumn(name="encapsulated_assertion_oid", referencedColumnName="objectid"))
    @MapKeyColumn(name="name",length=128)
    @Column(name="value", nullable=false, length=32672)
    protected Map<String,String> getProperties() {
        return properties;
    }

    /**
     * Set the extra properties for this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @param properties the properties set to use
     */
    protected void setProperties(Map<String,String> properties) {
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
    public EncapsulatedAssertionConfig getCopy() {
        return getCopy(false);
    }

    @Transient
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

        if (guid != null ? !guid.equals(that.guid) : that.guid != null) return false;

        return true;
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
            "oid=" + getOid() +
            ", guid='" + getGuid() + "'" +
            ", name='" + getName() + "'" +
            ", policy=" + policy + "\n" +
            ", argumentDescriptors=" + argumentDescriptors + "\n" +
            ", resultDescriptors=" + resultDescriptors + "\n" +
            ", properties=" + properties +
            "}\n";
    }
}
