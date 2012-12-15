package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

/**
 * Properties dialog for an encapsulated assertion instance in a policy.
 * <p/>
 * Not to be confused with the properties dialog for the entity representing an encapsulated assertion config -- for that,
 * see {@link EncapsulatedAssertionConfigPropertiesDialog}.
 */
public class EncapsulatedAssertionPropertiesDialog extends EditRowBasedAssertionPropertiesEditor<EncapsulatedAssertion> {
    private JPanel contentPane;
    private JPanel propertiesPanel;

    public EncapsulatedAssertionPropertiesDialog(Frame owner, final EncapsulatedAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
    }

    @Override
    protected Set<PropertyInfo> getPropertyInfos() {
        if (initialAssertion == null)
            throw new IllegalStateException("initialAssertion not set");

        EncapsulatedAssertionConfig config = initialAssertion.config();
        if (config == null)
            throw new IllegalStateException("EncapsulatedAssertionConfig not provided");

        Set<PropertyInfo> ret = new LinkedHashSet<PropertyInfo>();

        Collection<EncapsulatedAssertionArgumentDescriptor> args = Functions.sort(config.getArgumentDescriptors(), new Comparator<EncapsulatedAssertionArgumentDescriptor>() {
            @Override
            public int compare(EncapsulatedAssertionArgumentDescriptor a, EncapsulatedAssertionArgumentDescriptor b) {
                return a.getArgumentName().compareTo(b.getArgumentName());
            }
        });
        for (EncapsulatedAssertionArgumentDescriptor arg : args) {
            if (!arg.isGuiPrompt())
                continue;

            ret.add(new EncapsulatedAssertionArgumentDescriptorPropertyInfo(arg, getPropertyValueClass(arg)));
        }

        return ret;
    }

    private Class getPropertyValueClass(EncapsulatedAssertionArgumentDescriptor arg) {
        DataType type = DataType.forName(arg.getArgumentType());
        if (type == null || DataType.UNKNOWN.equals(type))
            return Object.class;
        if (DataType.MESSAGE == type)
            return Message.class;
        return type.getValueClasses()[0];
    }

    private class EncapsulatedAssertionArgumentDescriptorPropertyInfo implements PropertyInfo {
        private final EncapsulatedAssertionArgumentDescriptor arg;
        private final Class propertyValueClass;

        public EncapsulatedAssertionArgumentDescriptorPropertyInfo(EncapsulatedAssertionArgumentDescriptor arg, Class propertyValueClass) {
            this.arg = arg;
            this.propertyValueClass = propertyValueClass;
        }

        @Override
        public String getName() {
            return arg.getArgumentName();
        }

        @Override
        public PropertyEditor createPropertyEditor() {
            PropertyEditor editor = findPropertyEditorForType(propertyValueClass);

            if (editor == null && Message.class.isAssignableFrom(propertyValueClass)) {
                editor = new GenericStringTagPropertyEditor(ArrayUtils.unshift(getVariableNamesOfType(DataType.MESSAGE), null));
            }

            if (editor == null && Element.class.isAssignableFrom(propertyValueClass)) {
                editor = new GenericStringTagPropertyEditor(ArrayUtils.unshift(getVariableNamesOfType(DataType.ELEMENT), null));
            }

            if (editor == null) {
                logger.log(Level.WARNING, "No property editor for type: " + propertyValueClass + " (property " + arg.getArgumentName() + ")");
            }

            return editor;
        }

        @Override
        public Object readValueFromBean(Object assertion) {
            return decodeFromString(arg, propertyValueClass, ((EncapsulatedAssertion) assertion).getParameter(arg.getArgumentName()));
        }

        @Override
        public void writeValueToBean(Object assertion, Object value) throws IllegalArgumentException {
            ((EncapsulatedAssertion)assertion).putParameter(arg.getArgumentName(), encodeToString(arg, propertyValueClass, value));
        }
    }

    private Map<String, VariableMetadata> getVariablesSetByPredecessors() {
        Assertion previousAssertion = getPreviousAssertion();
        return (initialAssertion != null && initialAssertion.getParent() != null) ? SsmPolicyVariableUtils.getVariablesSetByPredecessors(initialAssertion) :
                (previousAssertion != null)? SsmPolicyVariableUtils.getVariablesSetByPredecessorsAndSelf( previousAssertion ) :
                    new TreeMap<String, VariableMetadata>();
    }

    private String[] getVariableNamesOfType(final DataType desiredType) {
        final Collection<VariableMetadata> allMetas = getVariablesSetByPredecessors().values();
        final List<VariableMetadata> messageMetas = Functions.grep(allMetas, new Functions.Unary<Boolean, VariableMetadata>() {
            @Override
            public Boolean call(VariableMetadata variableMetadata) {
                return desiredType.equals(variableMetadata.getType());
            }
        });
        List<String> ret = Functions.map(messageMetas, Functions.<String, VariableMetadata>propertyTransform(VariableMetadata.class, "name"));
        return ret.toArray(new String[ret.size()]);
    }

    private String encodeToString(EncapsulatedAssertionArgumentDescriptor arg, Class propertyValueClass, Object value) {
        if (Enum.class.isAssignableFrom(propertyValueClass)) {
            return value == null ? null : ((Enum)value).name();
        } else if (Date.class.isAssignableFrom(propertyValueClass)) {
            return value instanceof Date ? ISO8601Date.format((Date)value) : null;
        } else if (X509Certificate.class.isAssignableFrom(propertyValueClass)) {
            try {
                return value instanceof X509Certificate ? CertUtils.encodeAsPEM((X509Certificate) value) : null;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to encode certificate property: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        } else if (byte[].class.isAssignableFrom(propertyValueClass)) {
            // The binary value is stored in the string as base64 even though it is edited in the GUI as a hex string.
            return value instanceof byte[] ? HexUtils.encodeBase64((byte[])value) : null;
        }
        return value == null ? null : value.toString();
    }

    private Object decodeFromString(EncapsulatedAssertionArgumentDescriptor arg, Class propertyValueClass, String valueString) {
        if (Enum.class.isAssignableFrom(propertyValueClass)) {
            //noinspection unchecked
            return valueString == null ? null : Enum.valueOf((Class<? extends Enum>)propertyValueClass, valueString);
        } else if (Message.class == propertyValueClass) {
            // Value is the name of an in-scope Message variable
            return valueString;
        } else if (Boolean.class == propertyValueClass) {
            return Boolean.valueOf(valueString);
        } else if (Date.class.isAssignableFrom(propertyValueClass)) {
            try {
                return valueString == null || valueString.trim().length() < 1 ? null : ISO8601Date.parse(valueString);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Date property not ISO 8601 string: " + valueString + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                return null;
            }
        } else if (X509Certificate.class.isAssignableFrom(propertyValueClass)) {
            try {
                return valueString == null || valueString.trim().length() < 1 ? null : CertUtils.decodeFromPEM(valueString, false);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Certificate property not a valid PEM X.509 certificate: " + valueString + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                return null;
            }
        } else if (byte[].class.isAssignableFrom(propertyValueClass)) {
            // The binary value is stored in the string as base64 even though it is edited in the GUI as a hex string.
            return valueString == null || valueString.trim().length() < 1 ? null : HexUtils.decodeBase64(valueString, true);
        } else {
            return valueString;
        }

        // TODO move this functionality somewhere more central, maybe the server side can reuse it
    }

    /**
     * A simple property editor that just shows one or more String tags.
     */
    public static class GenericStringTagPropertyEditor extends PropertyEditorSupport {
        private final String[] tags;

        public GenericStringTagPropertyEditor(@NotNull String[] tags) {
            this.tags = tags;
        }

        @Override
        public String[] getTags() {
            return tags;
        }

        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            setValue(text);
        }
    }
}
