package com.l7tech.console.panels;

import com.l7tech.message.Message;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.variable.DataType;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyEditor;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;

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

        Set<EncapsulatedAssertionArgumentDescriptor> args = config.getArgumentDescriptors();
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

            // TODO use special editor for Message that is a combobox to select the name of an in-scope Message context variable

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

    private String encodeToString(EncapsulatedAssertionArgumentDescriptor arg, Class propertyValueClass, Object value) {
        if (Enum.class.isAssignableFrom(propertyValueClass)) {
            return value == null ? null : ((Enum)value).name();
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
        } else {
            return valueString;
        }

        // TODO support other types
        // TODO move this functionality somewhere more central, maybe the server side can reuse it
    }
}
