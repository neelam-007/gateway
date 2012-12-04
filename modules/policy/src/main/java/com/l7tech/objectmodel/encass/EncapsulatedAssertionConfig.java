package com.l7tech.objectmodel.encass;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.BeanUtils;
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
    public static final String META_PROP_PREFIX = "assertionMetadata.";
    public static final String PROP_META_BASE_NAME = META_PROP_PREFIX + AssertionMetadata.BASE_NAME;
    public static final String PROP_META_PALETTE_NODE_NAME = META_PROP_PREFIX + AssertionMetadata.PALETTE_NODE_NAME;
    public static final String PROP_META_PALETTE_NODE_ICON = META_PROP_PREFIX + AssertionMetadata.PALETTE_NODE_ICON;
    public static final String PROP_PALETTE_FOLDER = "paletteFolder";
    public static final String PROP_ICON_BASE64 = "paletteIconBase64";

    private Policy policy;
    private Set<EncapsulatedAssertionArgumentDescriptor> argumentDescriptors = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
    private Set<EncapsulatedAssertionResultDescriptor> resultDescriptors = new HashSet<EncapsulatedAssertionResultDescriptor>();
    private Map<String,String> properties = new HashMap<String,String>();


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

    @Transient
    public EncapsulatedAssertionConfig getCopy() {
        //noinspection TryWithIdenticalCatches
        try {
            EncapsulatedAssertionConfig copy = new EncapsulatedAssertionConfig();
            BeanUtils.copyProperties(this, copy,
                BeanUtils.omitProperties(BeanUtils.getProperties(getClass()), "properties", "argumentDescriptors", "resultDescriptors"));
            copy.setProperties(new HashMap<String, String>(getProperties()));
            copy.setArgumentDescriptors(new HashSet<EncapsulatedAssertionArgumentDescriptor>(getArgumentDescriptors()));
            copy.setResultDescriptors(new HashSet<EncapsulatedAssertionResultDescriptor>(getResultDescriptors()));
            return copy;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Transient
    public EncapsulatedAssertionConfig getReadOnlyCopy() {
        EncapsulatedAssertionConfig copy = getCopy();
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
    public String toString() {
        return "EncapsulatedAssertionConfig{" +
            "oid=" + getOid() +
            ", name='" + getName() + "'" +
            ", policy=" + policy + "\n" +
            ", argumentDescriptors=" + argumentDescriptors + "\n" +
            ", resultDescriptors=" + resultDescriptors + "\n" +
            ", properties=" + properties +
            "}\n";
    }
}
