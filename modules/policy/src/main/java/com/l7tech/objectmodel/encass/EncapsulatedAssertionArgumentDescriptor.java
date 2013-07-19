package com.l7tech.objectmodel.encass;

import com.l7tech.objectmodel.imp.GoidEntityImp;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
@XmlRootElement(name = "EncapsulatedAssertionArgument")
public class EncapsulatedAssertionArgumentDescriptor extends GoidEntityImp {
    private EncapsulatedAssertionConfig encapsulatedAssertionConfig;
    private int ordinal;
    private String argumentName;
    private String argumentType;
    private String guiLabel;
    private boolean guiPrompt;

    @ManyToOne(optional=false)
    @JoinColumn(name="encapsulated_assertion_goid", nullable=false)
    public EncapsulatedAssertionConfig getEncapsulatedAssertionConfig() {
        return encapsulatedAssertionConfig;
    }

    public void setEncapsulatedAssertionConfig(EncapsulatedAssertionConfig encapsulatedAssertionConfig) {
        checkLocked();
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
    @XmlElement(name = "ArgumentName")
    public String getArgumentName() {
        return argumentName;
    }

    public void setArgumentName(String name) {
        checkLocked();
        this.argumentName = name;
    }

    /**
     * @return the data type of this argument, as a name of a value of {@link com.l7tech.policy.variable.DataType}.
     */
    @Column(name="argument_type")
    @XmlElement(name = "ArgumentType")
    public String getArgumentType() {
        return argumentType;
    }

    /**
     * @param type the data type of this argument, as a name of a value of {@link com.l7tech.policy.variable.DataType}.
     */
    public void setArgumentType(String type) {
        checkLocked();
        this.argumentType = type;
    }

    /**
     * Whether the parameter should be delivered via a previously-set context variable (isGuiPrompt=false) or by
     * expanding an expression configured via the assertion bean properties GUI and stored in the assertion bean's policy XML.
     *
     * @return true if the GUI should prompt for a value for this property when the assertion is dragged from the palette into a policy.
     *         false if the value is expected to be delivered as a context variable already set in the parent policy enforcement context.
     */
    @Column(name="gui_prompt")
    @XmlElement(name = "GuiPrompt")
    public boolean isGuiPrompt() {
        return guiPrompt;
    }

    public void setGuiPrompt(boolean guiPrompt) {
        checkLocked();
        this.guiPrompt = guiPrompt;
    }

    /**
     * @return a label to display for this field in the GUI, if {@link #isGuiPrompt()} is true, or null to just show the raw name.
     */
    @Nullable
    @Column(name="gui_label")
    @XmlElement(name = "GuiLabel")
    public String getGuiLabel() {
        return guiLabel;
    }

    public void setGuiLabel(String guiLabel) {
        checkLocked();
        this.guiLabel = guiLabel;
    }

    /**
     * @return a number used for ordering the arguments within the properties dialog.
     */
    @Column(name="ordinal")
    @XmlElement(name = "Ordinal")
    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        checkLocked();
        this.ordinal = ordinal;
    }

    /**
     * Get context variables that would be used by the specified parameter value provided for this argument descriptor.
     *
     * @param parameterValue value of the parameter.  If null, this method will return an empty collection.
     * @return a list of variable names referenced by the paramter value.  Never null.  Will be empty if this descriptor is not configured
     *         for input via a per-assertion-instance GUI dialog, or if this descriptor's configuration (eg, data type) does not
     *         allow context variable interpolation in the parameter value.
     */
    @NotNull
    public String[] getVariablesUsed(String parameterValue) {
        if (isGuiPrompt() && allowVariableInterpolationForDataType(getArgumentType())) {
            try {
                return Syntax.getReferencedNames(parameterValue);
            } catch (RuntimeException e) {
                /* FALLTHROUGH and return empty */
            }
        }
        if (valueIsParentContextVariableNameForDataType(getArgumentType()) && parameterValue != null && parameterValue.trim().length() > 0) {
            // The value is the name of a context variable which we will be using from the parent context
            return new String[] { parameterValue };
        }
        return new String[0];
    }

    /**
     * @return a DataType instance for the current argument type.  Null if not set, and UNKNOWN if not recognized.
     */
    public DataType dataType() {
        return DataType.forName(getArgumentType());
    }

    /**
     * Check whether context variable interpolation (${varName} references) should be permitted for the specified
     * data type.
     * <p/>
     * Currently this method returns true only for the String type.
     *
     * @param argumentType the argument type.  If null, this method returns false.
     * @return true if the GUI should permit entry of values for the specified data type that include context variable interpolation.
     */
    public static boolean allowVariableInterpolationForDataType(String argumentType) {
        // For now we will allow only String GUI fields to by expressions that include context variables.
        // TODO maybe we should make this selectable per argument descriptor
        return DataType.STRING.getShortName().equals(argumentType);
    }

    /**
     * Check whether GUI-preconfigured values for parameters of the specified type should be treated as names
     * of context variables that are assumed to already exist in the parent context.
     * <p/>
     * Currently this method returns true only for the Message and Element types.
     *
     * @param argumentType the argument type.  If null, this method returns false.
     * @return true if a GUI-configured value for a parameter of this type should be assumed to be the name of a parent context variable instead of a raw value.
     */
    public static boolean valueIsParentContextVariableNameForDataType(String argumentType) {
        return DataType.MESSAGE.getShortName().equals(argumentType) || DataType.ELEMENT.getShortName().equals(argumentType);
    }

    /**
     * Get a copy of this argument descriptor pointing at the specified config, optionally marking it as read-only.
     * <p/>
     * The copy will have the default (non-persisted) OID and so will not compare as equals() to the original.
     *
     * @param newConfig config that should own the copied descriptor.  May be null.
     * @param readOnly true to mark the copy read-only.
     * @return a new EncapsulatedAssertionArgumentDescriptor
     */
    public EncapsulatedAssertionArgumentDescriptor getCopy(EncapsulatedAssertionConfig newConfig, boolean readOnly) {
        EncapsulatedAssertionArgumentDescriptor copy = new EncapsulatedAssertionArgumentDescriptor();
        copy.setEncapsulatedAssertionConfig(newConfig);
        copy.setOrdinal(getOrdinal());
        copy.setArgumentName(getArgumentName());
        copy.setArgumentType(getArgumentType());
        copy.setGuiPrompt(isGuiPrompt());
        copy.setGuiLabel(getGuiLabel());
        if (readOnly)
            copy.lock();
        return copy;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncapsulatedAssertionArgumentDescriptor)) return false;
        if (!super.equals(o)) return false;

        EncapsulatedAssertionArgumentDescriptor that = (EncapsulatedAssertionArgumentDescriptor) o;

        if (guiPrompt != that.guiPrompt) return false;
        if (argumentName != null ? !argumentName.equals(that.argumentName) : that.argumentName != null) return false;
        if (argumentType != null ? !argumentType.equals(that.argumentType) : that.argumentType != null) return false;
        if (guiLabel != null ? !guiLabel.equals(that.guiLabel) : that.guiLabel != null) return false;
        if (ordinal != that.ordinal) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (argumentName != null ? argumentName.hashCode() : 0);
        result = 31 * result + (argumentType != null ? argumentType.hashCode() : 0);
        result = 31 * result + (guiPrompt ? 1 : 0);
        result = 31 * result + (guiLabel != null ? guiLabel.hashCode() : 0);
        result = 31 * result + ordinal;
        return result;
    }

    @Override
    public String toString() {
        return "EncapsulatedAssertionArgumentDescriptor{" +
            "eacGoid=" + (encapsulatedAssertionConfig == null ? null : encapsulatedAssertionConfig.getGuid()) +
            ", ordinal=" + ordinal +
            ", argumentName='" + argumentName + '\'' +
            ", argumentType='" + argumentType + '\'' +
            ", guiPrompt=" + guiPrompt +
            ", guiLabel=" + guiLabel +
            '}';
    }
}
