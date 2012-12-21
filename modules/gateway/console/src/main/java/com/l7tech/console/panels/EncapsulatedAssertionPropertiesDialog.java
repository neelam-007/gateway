package com.l7tech.console.panels;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionStringEncoding;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
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
    private static final String COLON = ":";
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

        List<EncapsulatedAssertionArgumentDescriptor> args = config.sortedArguments();
        for (EncapsulatedAssertionArgumentDescriptor arg : args) {
            if (!arg.isGuiPrompt())
                continue;

            ret.add(new EncapsulatedAssertionArgumentDescriptorPropertyInfo(arg, getPropertyValueClass(arg)));
        }

        return ret;
    }

    @Override
    protected String getDisplayName(final PropertyInfo prop) {
        if (prop instanceof EncapsulatedAssertionArgumentDescriptorPropertyInfo) {
            final EncapsulatedAssertionArgumentDescriptorPropertyInfo argProp = (EncapsulatedAssertionArgumentDescriptorPropertyInfo) prop;
            final String name = argProp.getName();
            if (StringUtils.isNotBlank(name)) {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(name);
                if (StringUtils.isNotBlank(argProp.arg.getArgumentType())) {
                    final String type = " (" + argProp.arg.getArgumentType() + ")";
                    if (name.endsWith(COLON)) {
                        stringBuilder.insert(name.length() - 1, type);
                    } else {
                        stringBuilder.append(type);
                        stringBuilder.append(COLON);
                    }
                }
                return stringBuilder.toString();
            }
        }
        return super.getDisplayName(prop);
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
        private final DataType dataType;
        private final Class propertyValueClass;

        public EncapsulatedAssertionArgumentDescriptorPropertyInfo(EncapsulatedAssertionArgumentDescriptor arg, Class propertyValueClass) {
            this.arg = arg;
            this.propertyValueClass = propertyValueClass;
            this.dataType = DataType.forName(arg.getArgumentType());
        }

        @Override
        public String getName() {
            // label has priority over name
            final String label = StringUtils.isBlank(arg.getGuiLabel()) ? StringUtils.EMPTY : arg.getGuiLabel().trim();
            final String name = StringUtils.isBlank(arg.getArgumentName()) ? StringUtils.EMPTY : arg.getArgumentName().trim();
            return !label.isEmpty() ? label : name;
        }

        @Override
        public PropertyEditor createPropertyEditor() {
            PropertyEditor editor = findPropertyEditorForType(propertyValueClass);

            if (editor == null && Message.class.isAssignableFrom(propertyValueClass)) {
                final String[] msgVars = getVariableNamesOfType(DataType.MESSAGE);
                editor = new GenericStringTagPropertyEditor(msgVars.length > 0 ? msgVars : new String[]{null});
            }

            if (editor == null && Element.class.isAssignableFrom(propertyValueClass)) {
                final String[] elementVars = getVariableNamesOfType(DataType.ELEMENT);
                editor = new GenericStringTagPropertyEditor(elementVars.length > 0 ? elementVars : new String[]{null});
            }

            if (editor == null) {
                logger.log(Level.WARNING, "No property editor for type: " + propertyValueClass + " (property " + arg.getArgumentName() + ")");
            }

            return editor;
        }

        @Override
        public Object readValueFromBean(Object assertion) {
            return EncapsulatedAssertionStringEncoding.decodeFromString(dataType, ((EncapsulatedAssertion) assertion).getParameter(arg.getArgumentName()));
        }

        @Override
        public void writeValueToBean(Object assertion, Object value) throws IllegalArgumentException {
            ((EncapsulatedAssertion)assertion).putParameter(arg.getArgumentName(), EncapsulatedAssertionStringEncoding.encodeToString(dataType, value));
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
