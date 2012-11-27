package com.l7tech.objectmodel.encass;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;

/**
 * Represents a declared input value for an encapsulated assertion.
 * <p/>
 * When an encapsulated assertion is dragged from the palette into a policy, a properties dialog will be displayed
 * that requires values to be entered for any argument descriptor with {@link #isGuiPrompt()}=true.  Values configured
 * in the GUI will be stored as properties in the encapsulated assertion bean (and hence stored in the policy XML). If
 * these fields use variables those will be advertised in the assertion bean's UsesVariables.  Any argument descriptor
 * with {@link #isGuiPrompt()}=false will be advertised directly in UsesVariables.
 * <p/>
 * At runtime, if a value is not configured for a property because it is marked as guiPrompt=true but the property
 * isn't set in the assertion bean's policy XML, or if guiPrompt=false but no variable with that property is present
 * in the parent PolicyEnforcementContext, then the default value will be used (if any).
 * <p/>
 * The convention for converting defaultValue to the actual runtime value will vary depending on the configured data type.
 */
@Entity
@Proxy(lazy=false)
@Table(name="encapsulated_assertion_argument")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class EncapsulatedAssertionArgumentDescriptor extends PersistentEntityImp {
    private EncapsulatedAssertionConfig encapsulatedAssertionConfig;
    private String argumentName;
    private String argumentType;
    private String defaultValue;
    private boolean guiPrompt;

    @ManyToOne(optional=false)
    @JoinColumn(name="encapsulated_assertion_oid", nullable=false)
    public EncapsulatedAssertionConfig getEncapsulatedAssertionConfig() {
        return encapsulatedAssertionConfig;
    }

    public void setEncapsulatedAssertionConfig(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
        this.encapsulatedAssertionConfig = encapsulatedAssertionConfig;
    }

    // Ensure version gets persisted
    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    /**
     * @return the name of a an input parameter for the encapsulated assertion (passed in either as a context
     * variable or as an assertion bean property, depending on the setting of {@link #isGuiPrompt()}).
     */
    @Column(name="argument_name")
    public String getArgumentName() {
        return argumentName;
    }

    public void setArgumentName(String name) {
        this.argumentName = name;
    }

    /**
     * @return the data type of this argument, as a name of an enum value of {@link EncapsulatedAssertionDataType}.
     */
    @Column(name="argument_type")
    public String getArgumentType() {
        return argumentType;
    }

    public void setArgumentType(String type) {
        this.argumentType = type;
    }

    /**
     * A default value to use if a value cannot be found at runtime.  This can be useful for versioning when
     * new parameters are added to a previously-used encapsulated assertion.
     *
     * @return default value for this argument if a value cannot be found at runtime.  May be null.
     */
    @Nullable
    @Column(name="default_value")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(@Nullable String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Whether the parameter should be delivered via a previously-set context variable (isGuiPrompt=false) or by
     * expanding an expression configured via the assertion bean properties GUI and stored in the assertion bean's policy XML.
     *
     * @return true if the GUI should prompt for a value for this property when the assertion is dragged from the palette into a policy.
     *         false if the value is expected to be delivered as a context variable already set in the parent policy enforcement context.
     */
    @Column(name="gui_prompt")
    public boolean isGuiPrompt() {
        return guiPrompt;
    }

    public void setGuiPrompt(boolean guiPrompt) {
        this.guiPrompt = guiPrompt;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncapsulatedAssertionArgumentDescriptor)) return false;
        if (!super.equals(o)) return false;

        EncapsulatedAssertionArgumentDescriptor that = (EncapsulatedAssertionArgumentDescriptor) o;

        if (guiPrompt != that.guiPrompt) return false;
        if (defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null) return false;
        if (argumentName != null ? !argumentName.equals(that.argumentName) : that.argumentName != null) return false;
        if (argumentType != null ? !argumentType.equals(that.argumentType) : that.argumentType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (argumentName != null ? argumentName.hashCode() : 0);
        result = 31 * result + (argumentType != null ? argumentType.hashCode() : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        result = 31 * result + (guiPrompt ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EncapsulatedAssertionArgumentDescriptor{" +
            "eacOid=" + (encapsulatedAssertionConfig == null ? null : encapsulatedAssertionConfig.getOid()) +
            ", argumentName='" + argumentName + '\'' +
            ", argumentType='" + argumentType + '\'' +
            ", defaultValue='" + defaultValue + '\'' +
            ", guiPrompt=" + guiPrompt +
            '}';
    }
}
