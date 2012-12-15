package com.l7tech.console.panels;

import com.l7tech.console.policy.*;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility superclass for any assertion properties editor that can edit all of its parameters using (possibly-dynamic)
 * EditRow instances for each parameter.
 * <p/>
 * Subclasses must call {@link #initComponents()} after superclass construction to initialize.
 * Subclasses or users must then call {@link #setData} to set the view data.
 */
public abstract class EditRowBasedAssertionPropertiesEditor<AT extends Assertion> extends AssertionPropertiesOkCancelSupport<AT> {
    protected static final Logger logger = Logger.getLogger(DefaultAssertionPropertiesEditor.class.getName());

    /** The assertion passed to the constructor, if any. */
    @Nullable
    protected final AT initialAssertion;

    private final Collection<EditRow> editRows = new ArrayList<EditRow>();

    public EditRowBasedAssertionPropertiesEditor(Window owner, AT assertion) {
        super((Class<? extends AT>)assertion.getClass(), owner, assertion, true);
        this.initialAssertion = assertion;
    }

    public EditRowBasedAssertionPropertiesEditor(Class<? extends AT> beanClass) {
        super(beanClass);
        this.initialAssertion = null;
    }

    public EditRowBasedAssertionPropertiesEditor(Class<? extends AT> beanClass, Window owner, String title, boolean modal, AT assertion) {
        super(beanClass, owner, title, modal);
        this.initialAssertion = assertion;
    }

    protected void showValidationErrorMessage(ValidationException ve) {
        if (ve instanceof BadViewValueException) {
            BadViewValueException bvve = (BadViewValueException) ve;
            DialogDisplayer.showMessageDialog(this, "Invalid Field", "Field " + bvve.propertyName + " is invalid.", null);
        } else {
            super.showValidationErrorMessage(ve);
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
        Set<PropertyInfo> props = getPropertyInfos();

        for (PropertyInfo prop : props) {
            PropertyEditor editor = prop.createPropertyEditor();

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

        return propPanel;
    }

    /**
     * Get property descriptors for the editable properties of the assertion.
     * <p/>
     * This method just gets descriptors for all WSP-visible properties of the bean class.
     *
     * @return a set of PropertyDescriptor instances.
     */
    protected abstract Set<PropertyInfo> getPropertyInfos();

    /**
     * Create an EditRow for the specified property.
     * <p/>
     * This method tries the following:
     * <ol>
     * <li>If the specified property editor provides a custom component, this method invokes {@link #createEditRowUsingComponent}.
     * <li>Otherwise, if the specified property editor provides tags, this method invokes {@link #createEditRowUsingTags}.
     * <li>Otherwise, this method invokes {@link #createEditRowUsingString}.
     * </ol>
     *
     * @param prop  the property.  Required.
     * @param editor  the property editor for this property.  Required.
     * @return an EditRow instance for editing this property.  Never null.
     */
    protected EditRow createEditRow(PropertyInfo prop, PropertyEditor editor) {
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
     * This method just creates a new JLabel with the text set to the value returned by {@link #getDisplayName}
     * called with the row's property descriptor.
     *
     * @param row the EditRow to label. Required.
     * @return a component to use as a label for this row.  Never null.
     */
    protected Component createEditRowLabel(EditRow row) {
        return new JLabel(getDisplayName(row.getPropertyInfo()));
    }

    /**
     * Get a friendly name to use for the specified property.
     * <p/>
     * This method just invokes {@link PropertyInfo#getName()}.
     *
     * @param prop the property to examine. Required.
     * @return the display name for this property.
     */
    protected String getDisplayName(PropertyInfo prop) {
        return prop.getName();
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
    protected EditRow createEditRowUsingComponent(PropertyInfo prop, PropertyEditor editor, Component component) {
        return new EditRow(prop, editor, component);
    }

    /**
     * Create an EditRow instance that will edit the specified property using its tags.
     * <p/>
     * This method just instantiates {@link com.l7tech.console.panels.EditRowBasedAssertionPropertiesEditor.ComboBoxEditRow}.
     *
     * @param prop  the property.  Required.
     * @param editor  the property editor for this property.  Required.
     * @param tags  the property editor tags.  Required: caller is responsible for ensuring that
     *              the property editor provided some and that this parameter is non-null and non-empty.
     * @return an EditRow instance for this property.  Never null.
     */
    protected EditRow createEditRowUsingTags(PropertyInfo prop, PropertyEditor editor, String[] tags) {
        return new ComboBoxEditRow(prop, editor, new JComboBox(tags));
    }

    /**
     * Create an EditRow instance that will edit the specified property as a String.
     * <p/>
     * This method just instantiates {@link com.l7tech.console.panels.EditRowBasedAssertionPropertiesEditor.TextComponentEditRow}.
     *
     * @param prop  the property.  Required.
     * @param editor  the property editor for this property.  Required.
     * @return an EditRow instance for this property.  Never null.
     */
    protected EditRow createEditRowUsingString(PropertyInfo prop, PropertyEditor editor) {
        return new TextComponentEditRow(prop, editor, new JTextField(50));
    }

    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    public void setData(AT assertion) {
        for (EditRow row : editRows) {
            setViewDataFromBean(row, assertion);
        }
    }

    protected void setViewDataFromBean(EditRow row, AT assertion) {
        row.editor.setValue(row.prop.readValueFromBean(assertion));
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
     * @throws BadViewValueException if at least one EditRow's view is currently in a state that cannot be converted
     *                               to a valid value for its corresponding property.
     */
    public AT getData(AT assertion) throws BadViewValueException {
        for (EditRow row : editRows) {
            try {
                row.prop.writeValueToBean(assertion, row.getViewValue());
            } catch (BadViewValueException e) {
                setConfirmed(false);
            } catch (IllegalArgumentException e) {
                setConfirmed(false);
            }
        }

        return assertion;
    }

    protected static class BadViewValueException extends ValidationException {
        private final String propertyName;

        public BadViewValueException(String message, Throwable cause, String propertyName) {
            super(message, cause);
            this.propertyName = propertyName;
        }
    }

    /**
     * Utility method to find a property editor for the specified property type.
     *
     * @param propertyType type of the property, eg Enum.class.
     * @return a property editor, or null if not found.
     */
    @Nullable
    public static PropertyEditor findPropertyEditorForType(@NotNull Class<?> propertyType) {
        PropertyEditor editor = PropertyEditorManager.findEditor(propertyType);

        if (editor == null && Enum.class.isAssignableFrom(propertyType)) {
            //noinspection unchecked
            Class<? extends Enum> clazz = (Class<? extends Enum>) propertyType;
            editor = new EnumPropertyEditor(clazz);
        }

        if (editor == null && BigInteger.class.isAssignableFrom(propertyType)) {
            editor = new BigIntegerPropertyEditor();
        }

        if (editor == null && BigDecimal.class.isAssignableFrom(propertyType)) {
            editor = new BigDecimalPropertyEditor();
        }

        if (editor == null && Date.class.isAssignableFrom(propertyType)) {
            editor = new DateTimePropertyEditor();
        }

        if (editor == null && X509Certificate.class.isAssignableFrom(propertyType)) {
            editor = new ConsoleX509CertificatePropertyEditor();
        }

        if (editor == null && byte[].class.isAssignableFrom(propertyType)) {
            editor = new ByteArrayPropertyEditor();
        }

        return editor;
    }

    /**
     * Represents a property and the UI component that is used to edit it.  Owns any external wiring
     * that may be needed between the UI component and the PropertyEditor state.
     */
    protected static class EditRow {
        private final PropertyInfo prop;
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
        public EditRow(PropertyInfo prop, PropertyEditor editor, Component editorComponent) {
            if (prop == null || editor == null || editorComponent == null)
                throw new NullPointerException();
            this.prop = prop;
            this.editor = editor;
            this.editorComponent = editorComponent;
        }

        /**
         * @return the property descriptor.  Never null.
         */
        protected PropertyInfo getPropertyInfo() {
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
        public TextComponentEditRow(PropertyInfo prop, final PropertyEditor editor, final JTextComponent textComponent) {
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
                throw new BadViewValueException(ExceptionUtils.getMessage(e), e, getPropertyInfo().getName());
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
        public ComboBoxEditRow(PropertyInfo prop, final PropertyEditor editor, final JComboBox comboBox) {
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
                final Object tagItem = comboBox.getSelectedItem();
                getPropertyEditor().setAsText(tagItem == null ? null : tagItem.toString());
                return getPropertyEditor().getValue();
            } catch (IllegalArgumentException e) {
                throw new BadViewValueException(ExceptionUtils.getMessage(e), e, getPropertyInfo().getName());
            }
        }
    }

    protected static interface PropertyInfo {
        /**
         * @return the property name.
         */
        String getName();

        /**
         * @return a new property editor for this component.
         */
        PropertyEditor createPropertyEditor();

        /**
         * Read the value of this property from the specified assertion bean.
         *
         * @param assertion the assertion from which to read this property's value.
         * @return the value of the property.  May be null.
         */
        Object readValueFromBean(Object assertion);

        /**
         * Update the value of this property in the specified assertion bean.
         *
         * @param assertion the assertion to which to write this property's value.
         * @param value the value of hte property.  May be null.
         * @throws IllegalArgumentException if bean does not accept this value for this property
         */
        void writeValueToBean(Object assertion, Object value) throws IllegalArgumentException;
    }
}
