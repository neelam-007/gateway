package com.l7tech.console.panels;

import com.l7tech.console.policy.EnumPropertyEditor;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.variable.InvalidContextVariableException;
import com.l7tech.policy.wsp.TypeMappingUtils;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic Assertion bean property editor that provides poor-quality dialog for any assertion bean that has
 * at least one WSP-visible property.
 */
public class DefaultAssertionPropertiesEditor<AT extends Assertion> extends AssertionPropertiesOkCancelSupport<AT> {
    protected static final Logger logger = Logger.getLogger(DefaultAssertionPropertiesEditor.class.getName());

    protected static class BadViewValueException extends ValidationException {
        private final EditRow row;

        public BadViewValueException(String message, Throwable cause, EditRow row) {
            super(message, cause);
            this.row = row;
        }
    }

    /**
     * Represents a property and the UI component that is used to edit it.  Owns any external wiring
     * that may be needed between the UI component and the PropertyEditor state.
     */
    protected static class EditRow {
        private final PropertyDescriptor prop;
        private final PropertyEditor editor;
        private final Component editorComponent;

        /**
         * Create an EditRow for the specified property using the specified UI component.
         * <p/>
         * This constructor assumes that the editor component is already wired up to the property editor
         * and does no additional wiring.
         *
         * @param prop the property descriptor for the property to edit. Required.
         * @param editor the property editor for this property.  Required.
         * @param editorComponent  the UI component to use for editing the property.  Required.
         */
        public EditRow(PropertyDescriptor prop, PropertyEditor editor, Component editorComponent) {
            if (prop == null || editor == null || editorComponent == null)
                throw new NullPointerException();
            this.prop = prop;
            this.editor = editor;
            this.editorComponent = editorComponent;
        }

        /**
         * @return the property descriptor.  Never null.
         */
        protected PropertyDescriptor getPropertyDescriptor() {
            return prop;
        }

        /**
         * @return the property editor.  Never null.
         */
        protected PropertyEditor getPropertyEditor() {
            return editor;
        }

        /**
         * @return the UI component.  Never null.
         */
        public Component getEditorComponent() {
            return editorComponent;
        }

        /**
         * Get current value from the view, translated into an object that can be passed to the property writer.
         * Throws if the view is not formatted correctly to be converted into the property type.
         * <p/>
         * This method just invokes getValue on the property editor.
         *
         * @return the current value from the view, ready to be written back into the corresponding property.  May be null.
         * @throws BadViewValueException if the current value in the view cannot be converted into a property value.
         */
        public Object getViewValue() throws BadViewValueException {
            return editor.getValue();
        }
    }

    private final Collection<EditRow> editRows = new ArrayList<EditRow>();

    /**
     * Create a DefaultAssertionPropertiesEditor dialog owned by the specified Frame and prepared to edit the
     * specified assertion bean.
     *
     * @param parent owner frame from which the dialog is displayed.
     * @param assertion the assertion bean to edit. Required.
     */
    public DefaultAssertionPropertiesEditor(Frame parent, AT assertion) {
        //noinspection unchecked
        super((Class<? extends AT>) assertion.getClass(), parent, (String)assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
        setData(assertion);
    }

    /**
     * Create a DefaultAssertionPropertiesEditor dialog prepared to edit the specified assertion bean
     * class.
     *
     * @param assertionClass class of assertion bean to edit.  Required.
     */
    protected DefaultAssertionPropertiesEditor(Class<? extends AT> assertionClass) {
        super(assertionClass);
    }

    protected void showValidationErrorMessage(ValidationException ve) {
        if (ve instanceof BadViewValueException) {
            BadViewValueException bvve = (BadViewValueException) ve;
            DialogDisplayer.showMessageDialog(this, "Invalid Field", "Field " + bvve.row.getPropertyDescriptor().getName() + " is invalid.", null);
        } else {
            super.showValidationErrorMessage(ve);
        }
    }

    /**
     * Check if the specified assertion class has at least one property that is readable, writable,
     * and not WSP ignorable.
     *
     * @param c the assertion class to examine
     * @return true if the specified class has at least one public property that is readable and writable
     *         and is not ignorable.
     */
    public static boolean hasEditableProperties(Class<? extends Assertion> c) {
        try {
            return !getWspProperties(c).isEmpty();
        } catch (IntrospectionException e) {
            logger.log(Level.WARNING, "Unable to introspect assertion class: " + ExceptionUtils.getMessage(e), e);
            return false;
        }
    }

    /**
     * Create a panel to edit the properties of the assertion bean.  This panel does not include any
     * Ok or Cancel buttons.
     * <p/>
     * This method creates one EditRow for each assertion WSP property, and arranges them from top
     * to bottom.  Each row has a label on the left and a property editor component on the right.
     * Each row is backed by an EditRow instance that takes care of wiring up the view to the property editor
     * and knows how to retrieve the the property value currently represented by the view.
     * @return a panel that can be used to edit the assertion properties.  Never null.
     */
    protected JPanel createPropertyPanel() {
        editRows.clear();
        JPanel propPanel = new JPanel(new GridBagLayout());
        int y = 0;
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(6, 6, 6, 6);
        try {
            Set<PropertyDescriptor> props = getWspProperties(getBeanClass());

            for (PropertyDescriptor prop : props) {
                PropertyEditor editor = getPropertyEditor(prop);

                if (editor == null) {
                    // No way to edit this property
                    continue;
                }

                final EditRow row = createEditRow(prop, editor);

                gc.gridy = y;
                gc.gridx = 1;
                gc.weightx = 0.0;
                propPanel.add(createEditRowLabel(row), gc);
                gc.gridx = 2;
                gc.weightx = 1.0;
                propPanel.add(row.getEditorComponent(), gc);

                editRows.add(row);
                y++;
            }

            gc.gridy = y;
            gc.gridx = 1;
            gc.weightx = 0.0;
            gc.weighty = 99999.0;
            propPanel.add(Box.createGlue(), gc);

        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        return propPanel;
    }

    /**
     * Create an EditRow for the specified property.
     * <p/>
     * This method tries the following:
     * <ol>
     * <li>If the specified property editor provides a custom component, this method invokes {@link #createEditRowUsingComponent(java.beans.PropertyDescriptor, java.beans.PropertyEditor, java.awt.Component)}.
     * <li>Otherwise, if the specified property editor provides tags, this method invokes {@link #createEditRowUsingTags(java.beans.PropertyDescriptor, java.beans.PropertyEditor, String[])}.
     * <li>Otherwise, this method invokes {@link #createEditRowUsingString(java.beans.PropertyDescriptor, java.beans.PropertyEditor)}.
     * </ol>
     *
     * @param prop  the property.  Required.
     * @param editor  the property editor for this property.  Required.
     * @return an EditRow instance for editing this property.  Never null.
     */
    protected EditRow createEditRow(PropertyDescriptor prop, PropertyEditor editor) {
        final EditRow row;
        Component component = editor.supportsCustomEditor() ? editor.getCustomEditor() : null;
        if (component != null) {
            row = createEditRowUsingComponent(prop, editor, component);
        } else {
            // See if it uses tags
            String[] tags = editor.getTags();
            if (tags != null && tags.length > 0) {
                row = createEditRowUsingTags(prop, editor, tags);
            } else {
                row = createEditRowUsingString(prop, editor);
            }
        }
        return row;
    }

    /**
     * Create a label for the specified EditRow.
     * <p/>
     * This method just creates a new JLabel with the text set to the value returned by {@link #getDisplayName(java.beans.PropertyDescriptor)}
     * called with the row's property descriptor.
     *
     * @param row the EditRow to label. Required.
     * @return a component to use as a label for this row.  Never null.
     */
    protected Component createEditRowLabel(EditRow row) {
        return new JLabel(getDisplayName(row.getPropertyDescriptor()));
    }

    /**
     * Get a friendly name to use for the specified property.
     * <p/>
     * This method just invokes {@link java.beans.PropertyDescriptor#getDisplayName()}.
     * <p/>
     * TODO read from annotation and/or assertion metadata
     *
     * @param prop the property to examine. Required.
     * @return the display name for this property.
     */
    protected String getDisplayName(PropertyDescriptor prop) {
        return prop.getDisplayName();
    }

    /**
     * Create an EditRow instance that will edit the specified property using the specified property editor,
     * which provides the specified custom editor component.
     * <p/>
     * This method just instantiates a base EditRow to hold the descriptor, editor, and component.
     *
     * @param prop  the property.  Required.
     * @param editor  the property editor for this property.  Required.
     * @param component  a custom component provided by this property editor.  Required: caller is responsible for ensuring
     *                   that the property editor provided one and that this parameter is non-null.
     * @return an EditRow instance for this property.  Never null.
     */
    protected EditRow createEditRowUsingComponent(PropertyDescriptor prop, PropertyEditor editor, Component component) {
        return new EditRow(prop, editor, component);
    }

    /**
     * Create an EditRow instance that will edit the specified property using its tags.
     * <p/>
     * This method just instantiates {@link ComboBoxEditRow}.
     *
     * @param prop  the property.  Required.
     * @param editor  the property editor for this property.  Required.
     * @param tags  the property editor tags.  Required: caller is responsible for ensuring that
     *              the property editor provided some and that this parameteris non-null and non-empty.
     * @return an EditRow instance for this property.  Never null.
     */
    protected EditRow createEditRowUsingTags(PropertyDescriptor prop, PropertyEditor editor, String[] tags) {
        return new ComboBoxEditRow(prop, editor, new JComboBox(tags));
    }

    /**
     * Create an EditRow instance that will edit the specified property as a String.
     * <p/>
     * This method just instantiates {@link TextComponentEditRow}.
     *
     * @param prop  the property.  Required.
     * @param editor  the property editor for this property.  Required.
     * @return an EditRow instance for this property.  Never null.
     */
    protected EditRow createEditRowUsingString(PropertyDescriptor prop, PropertyEditor editor) {
        return new TextComponentEditRow(prop, editor, new JTextField(50));
    }

    /**
     * Find a PropertyEditor instance for the specified property.
     * <p/>
     * This method tries the following, in the following order:
     * <ol>
     * <li>Calls {@link PropertyDescriptor#getPropertyEditorClass} on the property.
     * <li>Calls {@link PropertyEditorManager#findEditor(Class)} to check for a globally-registered editor.
     * <li>If the field value is an Enum, creates an {@link EnumPropertyEditor}.
     * <li>Otherwise, this method gives up and returns null.
     * </ol>
     * <p/>
     * TODO support getting this information from an annotation and/or assertion metadata
     * 
     * @param prop property to examine. Require.
     * @return a PropertyEditor that can be used to edit this property value, or null.
     * @throws InstantiationException if the property editor cannot be instantiated
     * @throws IllegalAccessException if the property editor cannot be instantiated
     */
    protected PropertyEditor getPropertyEditor(PropertyDescriptor prop) throws InstantiationException, IllegalAccessException {
        PropertyEditor editor = null;

        Class<?> propEditClass = prop.getPropertyEditorClass();
        if (propEditClass != null)
            editor = (PropertyEditor)propEditClass.newInstance();

        if (editor == null)
            editor = PropertyEditorManager.findEditor(prop.getPropertyType());

        if (editor == null && Enum.class.isAssignableFrom(prop.getPropertyType())) {
            //noinspection unchecked
            Class<? extends Enum> clazz = (Class<? extends Enum>)prop.getPropertyType();
            editor = new EnumPropertyEditor(clazz);
        }
        return editor;
    }

    /**
     * Find all Wsp-visible properties for the specified assertion class.
     * <p/>
     * This method uses Introspector to examine the bean class, and returns all property descriptors that
     * are readable, writable, and not marked as ignorable by {@link TypeMappingUtils#isIgnorableProperty(String)}.
     *
     * @param assertionClass the assertion bean class. Required.
     * @return a set of property descriptors.  May be empty, but never null.
     * @throws IntrospectionException if the assertion bean class cannot be introspected.
     */
    public static Set<PropertyDescriptor> getWspProperties(Class<? extends Assertion> assertionClass) throws IntrospectionException {
        Set<PropertyDescriptor> ret = new HashSet<PropertyDescriptor>();
        BeanInfo info = Introspector.getBeanInfo(assertionClass);
        PropertyDescriptor[] props = info.getPropertyDescriptors();
        for (PropertyDescriptor prop : props) {
            String name = prop.getName();
            if (TypeMappingUtils.isIgnorableProperty(name))
                continue;
            Method reader = prop.getReadMethod();
            Method writer = prop.getWriteMethod();
            if (reader != null && writer != null && reader.getDeclaringClass() == assertionClass && writer.getDeclaringClass() == assertionClass)
                ret.add(prop);
        }
        return ret;
    }

    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    public void setData(AT assertion) {
        for (EditRow row : editRows) {
            try {
                row.editor.setValue(row.prop.getReadMethod().invoke(assertion));
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
            } catch (InvocationTargetException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
            }
        }
    }

    /**
     * Copy the data out of the view into an assertion bean instance.
     * The provided bean should be filled and returned, if possible, but implementors may create and return
     * a new bean instead, if they must.
     * <p/>
     * This method iterates all {@link #editRows}, gets each ViewValue, and writes back the value into the
     * corresponding property.
     *
     * @param assertion a bean to which the data from the view can be copied, if possible.  Must not be null.
     * @return the assertion that was passed in, with all property values updated.  Never null.
     * @throws com.l7tech.console.panels.DefaultAssertionPropertiesEditor.BadViewValueException if at least one EditRow's view is currently in a state that cannot be converted
     *                               to a valid value for its corresponding property.
     */
    public AT getData(AT assertion) throws BadViewValueException {
        for (EditRow row : editRows) {
            try {
                row.getPropertyDescriptor().getWriteMethod().invoke(assertion, row.getViewValue());
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
            } catch (InvocationTargetException e) {
                if (ExceptionUtils.causedBy(e, InvalidContextVariableException.class)) {
                    // This block is to handle the case, where InvalidContextVariableException  
                    // occurs during validating context variable.
                    setConfirmed(false);
                    logger.log(Level.WARNING, ExceptionUtils.getMessage(e));
                    throw new BadViewValueException(ExceptionUtils.getMessage(e), e, row);
                } else {
                    logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
                }
            }
        }

        return assertion;
    }

    /**
     * An {@link EditRow} instance designed to work with a JTextComponent as the editor component.
     */
    protected static class TextComponentEditRow extends EditRow {
        private final JTextComponent textComponent;

        /**
         * Create an EditRow for the specified property using the specified JTextComponent.
         * <p/>
         * This constructor will set a property change listener on the property editor
         * that updates the text in the text component.
         *
         * @param prop the property descriptor for the property to edit. Required.
         * @param editor the property editor for this property.  Required.
         * @param textComponent the JTextComponent to use to edit this property. Required.
         */
        public TextComponentEditRow(PropertyDescriptor prop, final PropertyEditor editor, final JTextComponent textComponent) {
            super(prop, editor, textComponent);
            this.textComponent = textComponent;
            editor.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    textComponent.setText(editor.getAsText());
                }
            });
        }

        /**
         * Get current value from the view, translated into an object that can be passed to the property writer.
         * Throws if the view is not formatted correctly to be converted into the property type.
         * <p/>
         * This method updates the property editor with the current text value, then returns the current
         * property editor value.
         *
         * @return the current value from the view, ready to be written back into the corresponding property.  May be null.
         * @throws BadViewValueException if the current value in the view cannot be converted into a property value.
         */
        @Override
        public Object getViewValue() throws BadViewValueException {
            try {
                getPropertyEditor().setAsText(textComponent.getText());
                return getPropertyEditor().getValue();
            } catch (IllegalArgumentException e) {
                throw new BadViewValueException(ExceptionUtils.getMessage(e), e, this);
            }
        }
    }

    protected static class ComboBoxEditRow extends EditRow {
        private final JComboBox comboBox;

        /**
         * Create an EditRow for the specified property using the specified JComboBox.
         * <p/>
         * This constructor will set a property change listener on the property editor
         * that updates the selected item in the combo box.
         *
         * @param prop the property descriptor for the property to edit. Required.
         * @param editor the property editor for this property.  Required.
         * @param comboBox the combo box to use to edit this property. Required.
         */
        public ComboBoxEditRow(PropertyDescriptor prop, final PropertyEditor editor, final JComboBox comboBox) {
            super(prop, editor, comboBox);
            this.comboBox = comboBox;
            editor.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    comboBox.setSelectedItem(editor.getAsText());
                }
            });
        }

        /**
         * Get current value from the view, translated into an object that can be passed to the property writer.
         * Throws if the view is not formatted correctly to be converted into the property type.
         * <p/>
         * This method just invokes getValue on the property editor.
         *
         * @return the current value from the view, ready to be written back into the corresponding property.  May be null.
         * @throws BadViewValueException if the current value in the view cannot be converted into a property value.
         */
        @Override
        public Object getViewValue() throws BadViewValueException {
            try {
                getPropertyEditor().setAsText(comboBox.getSelectedItem().toString());
                return getPropertyEditor().getValue();
            } catch (IllegalArgumentException e) {
                throw new BadViewValueException(ExceptionUtils.getMessage(e), e, this);
            }
        }
    }
}
