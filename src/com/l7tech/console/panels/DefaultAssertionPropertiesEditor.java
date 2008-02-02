package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.wsp.TypeMappingUtils;
import com.l7tech.console.policy.EnumPropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class DefaultAssertionPropertiesEditor<AT extends Assertion> extends AssertionPropertiesEditorSupport<AT>{
    protected static final Logger logger = Logger.getLogger(DefaultAssertionPropertiesEditor.class.getName());
    private JButton okButton;
    private boolean confirmed = false;

    private static class BadViewValueException extends RuntimeException {
        private final EditRow row;

        public BadViewValueException(String message, Throwable cause, EditRow row) {
            super(message, cause);
            this.row = row;
        }
    }

    private abstract static class EditRow {
        private final PropertyDescriptor prop;
        private final PropertyEditor editor;

        public EditRow(PropertyDescriptor prop, PropertyEditor editor) {
            this.prop = prop;
            this.editor = editor;
        }

        // Get current value from the view, translated into an object that can be passed to the property writer.
        // Throws if the view is not formatted correctly to be converted into the property type
        abstract Object getViewValue() throws BadViewValueException;
    }

    private final Collection<EditRow> editRows = new ArrayList<EditRow>();

    public DefaultAssertionPropertiesEditor(Frame parent, AT assertion) {
        super(parent, (String)assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initFields(assertion.getClass());
        setData(assertion);
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

    @Override
    protected void configureView() {
        okButton.setEnabled( !isReadOnly() );
    }

    private void initFields(final Class<? extends Assertion> c) {
        editRows.clear();

        okButton = new JButton("Ok");
        JButton cancelButton = new JButton("Cancel");
        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    //noinspection unchecked
                    getData((AT)c.newInstance());
                } catch (InstantiationException e1) {
                    throw new RuntimeException(e1); // can't happen
                } catch (IllegalAccessException e1) {
                    throw new RuntimeException(e1); // can't happen
                } catch (BadViewValueException bv) {
                    DialogDisplayer.showMessageDialog(DefaultAssertionPropertiesEditor.this,
                                                      "Invalid value for property " + bv.row.prop.getDisplayName() + ": " +
                                                            ExceptionUtils.getMessage(bv),
                                                      "Error",
                                                      JOptionPane.ERROR_MESSAGE, null);
                    return;
                }

                confirmed = true;
                DefaultAssertionPropertiesEditor.this.dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DefaultAssertionPropertiesEditor.this.dispose();
            }
        });

        JPanel main = new JPanel(new GridBagLayout());
        JPanel propPanel = new JPanel(new GridBagLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(8));
        buttonPanel.add(cancelButton);

        int y = 0;
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(12, 2, 12, 2);
        main.add(propPanel, gc);
        gc.gridy = 1;
        gc.insets = new Insets(2, 18, 8, 8);
        main.add(buttonPanel, gc);
        gc.insets = new Insets(6, 6, 6, 6);


        try {
            Set<PropertyDescriptor> props = getWspProperties(c);

            for (PropertyDescriptor prop : props) {
                PropertyEditor editor = null;

                Class<?> propEditClass = prop.getPropertyEditorClass();
                if (propEditClass != null)
                    editor = (PropertyEditor)propEditClass.newInstance();

                if (editor == null)
                    editor = PropertyEditorManager.findEditor(prop.getPropertyType());

                if (editor == null && Enum.class.isAssignableFrom(prop.getPropertyType())) {
                    Class<? extends Enum> clazz = (Class<? extends Enum>)prop.getPropertyType();
                    editor = new EnumPropertyEditor(clazz);
                }

                if (editor == null) {
                    // No way to edit this property
                    continue;
                }

                JLabel label = new JLabel(prop.getDisplayName());
                Component component = editor.supportsCustomEditor() ? editor.getCustomEditor() : null;
                final EditRow row;
                if (component == null) {
                    // See if it uses tags
                    String[] tags = editor.getTags();
                    final PropertyEditor editor1 = editor;
                    if (tags != null && tags.length > 0) {
                        final JComboBox comboBox = new JComboBox(tags);
                        component = comboBox;
                        editor.addPropertyChangeListener(new PropertyChangeListener() {
                            public void propertyChange(PropertyChangeEvent evt) {
                                comboBox.setSelectedItem(editor1.getAsText());
                            }
                        });
                        row = new EditRow(prop, editor) {
                            @Override
                            Object getViewValue() throws BadViewValueException {
                                try {
                                    editor1.setAsText(comboBox.getSelectedItem().toString());
                                    return editor1.getValue();
                                } catch (IllegalArgumentException e) {
                                    throw new BadViewValueException(ExceptionUtils.getMessage(e), e, this);
                                }
                            }
                        };
                    } else {
                        final JTextField textArea = new JTextField(50);
                        component = textArea;
                        editor.addPropertyChangeListener(new PropertyChangeListener() {
                            public void propertyChange(PropertyChangeEvent evt) {
                                textArea.setText(editor1.getAsText());
                            }
                        });
                        row = new EditRow(prop, editor) {
                            @Override
                            Object getViewValue() throws BadViewValueException {
                                try {
                                    editor1.setAsText(textArea.getText());
                                    return editor1.getValue();
                                } catch (IllegalArgumentException e) {
                                    throw new BadViewValueException(ExceptionUtils.getMessage(e), e, this);
                                }
                            }
                        };
                    }
                } else {
                    final PropertyEditor editor2 = editor;
                    row = new EditRow(prop, editor2) {
                        @Override
                        Object getViewValue() {
                            return editor2.getValue();
                        }
                    };
                }

                gc.gridy = y++;
                gc.gridx = 1;
                propPanel.add(label, gc);
                gc.gridx = 2;
                propPanel.add(component, gc);

                editRows.add(row);
            }

        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }

        setContentPane(main);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        pack();
    }

    private static Set<PropertyDescriptor> getWspProperties(Class<? extends Assertion> c) throws IntrospectionException {
        Set<PropertyDescriptor> ret = new HashSet<PropertyDescriptor>();
        BeanInfo info = Introspector.getBeanInfo(c);
        PropertyDescriptor[] props = info.getPropertyDescriptors();
        for (PropertyDescriptor prop : props) {
            String name = prop.getName();
            if (TypeMappingUtils.isIgnorableProperty(name))
                continue;
            Method reader = prop.getReadMethod();
            Method writer = prop.getWriteMethod();
            if (reader != null && writer != null && reader.getDeclaringClass() == c && writer.getDeclaringClass() == c)
                ret.add(prop);
        }
        return ret;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

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

    public AT getData(AT assertion) throws BadViewValueException {
        for (EditRow row : editRows) {
            try {
                row.prop.getWriteMethod().invoke(assertion, row.getViewValue());
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
            } catch (InvocationTargetException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
            }
        }

        return assertion;
    }
}
