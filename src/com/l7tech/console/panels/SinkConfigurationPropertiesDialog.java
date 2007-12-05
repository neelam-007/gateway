package com.l7tech.console.panels;

import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.log.SinkConfiguration;
import com.l7tech.common.log.LogSinkAdmin;
import static com.l7tech.common.log.SinkConfiguration.SeverityThreshold;
import static com.l7tech.common.log.SinkConfiguration.SinkType;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * This is the dialog for updating the properties of a log sink configuration.
 */
public class SinkConfigurationPropertiesDialog extends JDialog {
    /** The title for the dialog window */
    private static final String DIALOG_TITLE = "Log Sink Properties";

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private JPanel contentPane;
    private JTabbedPane tabbedPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameField;
    private JCheckBox enabledField;
    private JTextField descriptionField;
    private JComboBox typeField;
    private JComboBox severityField;
    private JList categoriesList;
    private JSpinner fileMaxSizeField;
    private JSpinner fileLogCount;
    private JComboBox syslogProtocolField;
    private JTextField syslogHostField;
    private JTextField syslogPortField;
    private JSpinner syslogFacilityField;
    private JComboBox syslogCharsetField;
    private JComboBox syslogTimezoneField;
    private JCheckBox syslogLogHostnameField;
    private JComboBox fileFormatField;
    private JButton syslogTestMessageButton;

    private InputValidator inputValidator;
    private SinkConfiguration sinkConfiguration;
    private boolean confirmed = false;

    /**
     * Provides a visible label and an enum value for the type combo box.
     */
    private static class TypeComboBoxValue {
        String name;
        SinkType value;

        public TypeComboBoxValue(String name, SinkType value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Provides a visible label and an enum value for the severity combo box.
     */
    private static class SeverityComboBoxValue {
        String name;
        SeverityThreshold value;

        public SeverityComboBoxValue(String name, SeverityThreshold value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Provides a visible label and a String value for the format combo box.
     */
    private static class FormatComboBoxValue {
        String name;
        String value;

        public FormatComboBoxValue(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Creates a new instance of SinkConfigurationPropertiesDialog. The fields in the dialog
     * will be set from the values in the provided SinkConfiguration and if the dialog is
     * dismissed with the OK button, then the provided SinkConfiguration will be updated with
     * the values from the fields in this dialog.
     *
     * @param owner The owner of this dialog window
     * @param sinkConfiguration The SinkConfiguration to read values from and possibly update
     */
    public SinkConfigurationPropertiesDialog(Frame owner, SinkConfiguration sinkConfiguration) {
        super(owner, DIALOG_TITLE);
        initialize(sinkConfiguration);
    }

    /**
     * Creates a new instance of SinkConfigurationPropertiesDialog. The fields in the dialog
     * will be set from the values in the provided SinkConfiguration and if the dialog is
     * dismissed with the OK button, then the provided SinkConfiguration will be updated with
     * the values from the fields in this dialog.
     *
     * @param owner The owner of this dialog window
     * @param sinkConfiguration The SinkConfiguration to read values from and possibly update
     */
    public SinkConfigurationPropertiesDialog(Dialog owner, SinkConfiguration sinkConfiguration) {
        super(owner, DIALOG_TITLE);
        initialize(sinkConfiguration);
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.SinkConfigurationPropertiesDialog", locale);
    }

    /**
     * Creates the values for the type combo box. The visible labels are read from the resource bundle
     * and the values are items from the SinkType enum.
     *
     * @return An array of values for the type combo box
     */
    private TypeComboBoxValue[] getTypeComboBoxValues() {
        TypeComboBoxValue[] values = new TypeComboBoxValue[SinkType.values().length];
        int i = 0;
        for(SinkType value : SinkType.values()) {
            String resourceString = resources.getString("baseSettings.type." + value.name() + ".text");
            if(resourceString == null) {
                values[i++] = new TypeComboBoxValue(value.name(), value);
            } else {
                values[i++] = new TypeComboBoxValue(resourceString, value);
            }
        }

        return values;
    }

    /**
     * Creates the values for the severity combo box. The visible labels are read from the resource bundle
     * and the values are from the SeverityThreshold enum.
     *
     * @return An array of values for the severity combo box
     */
    private SeverityComboBoxValue[] getSeverityComboBoxValues() {
        SeverityComboBoxValue[] values = new SeverityComboBoxValue[SeverityThreshold.values().length];
        int i = 0;
        for(SeverityThreshold value : SeverityThreshold.values()) {
            String resourceString = resources.getString("baseSettings.severity." + value.name() + ".text");
            if(resourceString == null) {
                values[i++] = new SeverityComboBoxValue(value.name(), value);
            } else {
                values[i++] = new SeverityComboBoxValue(resourceString, value);
            }
        }

        return values;
    }

    /**
     * Initializes this dialog window and sets all of the fields based on the provided SinkConfiguration
     * object.
     *
     * @param sinkConfiguration The SinkConfiguration to back this dialog
     */
    private void initialize(SinkConfiguration sinkConfiguration) {
        initializeBaseFields(sinkConfiguration);
        initializeFileFields();
        initializeSyslogFields();
        
        Utilities.setEscKeyStrokeDisposes(this);

        // Update all of the fields using the values from SinkConfiguration
        modelToView();
        enableDisableTabs();
        if (nameField.getText().length() < 1)
            nameField.requestFocusInWindow();
    }

    /**
     * Initializes the base settings fields.
     *
     * @param sinkConfiguration The SinkConfiguration to back this dialog
     */
    private void initializeBaseFields(SinkConfiguration sinkConfiguration) {
        initResources();

        setTitle(resources.getString("sinkConfigurationProperties.window.title"));
        inputValidator = new InputValidator(this, resources.getString("sinkConfigurationProperties.window.title"));

        this.sinkConfiguration = sinkConfiguration;
        setContentPane(contentPane);
        pack();
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        // Attach the validator to the OK button
        inputValidator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // When the type field is changed, enable or disable the tabs that aren't associated
        // with the new type value.
        typeField.setModel(new DefaultComboBoxModel(getTypeComboBoxValues()));
        typeField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableDisableTabs();
            }
        });

        severityField.setModel(new DefaultComboBoxModel(getSeverityComboBoxValues()));

        categoriesList.setListData(SinkConfiguration.CATEGORIES_SET.toArray());

        // Name field must not be empty and must not be longer than 128 characters
        inputValidator.constrainTextFieldToBeNonEmpty("Name", nameField, new InputValidator.ComponentValidationRule(nameField) {
            public String getValidationError() {
                if(nameField.getText().length() > 128) {
                    return resources.getString("baseSettings.name.errors.tooLong");
                }

                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges(nameField);

        // Description field must not be longer than 1000 characters
        inputValidator.constrainTextField(descriptionField, new InputValidator.ComponentValidationRule(descriptionField) {
            public String getValidationError() {
                if(descriptionField.getText().length() > 1000) {
                    return resources.getString("baseSettings.description.errors.tooLong");
                }

                return null;
            }
        });
    }

    /**
     * Initializes the file settings fields.
     */
    private void initializeFileFields() {
        JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(fileMaxSizeField);
        fileMaxSizeField.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(1);
        numberEditor.getModel().setValue(1024);

        numberEditor = new JSpinner.NumberEditor(fileLogCount);
        fileLogCount.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(1);
        numberEditor.getModel().setValue(1);

        fileFormatField.setModel(new DefaultComboBoxModel(new Object[] {
                new FormatComboBoxValue(resources.getString("fileSettings.format.RAW.text"), "RAW"),
                new FormatComboBoxValue(resources.getString("fileSettings.format.STANDARD.text"), "STANDARD"),
                new FormatComboBoxValue(resources.getString("fileSettings.format.VERBOSE.text"), "VERBOSE")
        }));
        fileFormatField.setSelectedIndex(1);
    }

    /**
     * Initializes the syslog settings fields.
     */
    private void initializeSyslogFields() {
        syslogProtocolField.setModel(new DefaultComboBoxModel(new Object[] {
                resources.getString("syslogSettings.protocol.TCP.text"),
                resources.getString("syslogSettings.protocol.UDP.text")
        }));

        JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(syslogFacilityField);
        syslogFacilityField.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(0);
        numberEditor.getModel().setMaximum(23);
        numberEditor.getModel().setValue(1);

        syslogLogHostnameField.setSelected(true);

        // Currently only give options for ASCII, LATIN-1 and UTF-8. Are more encodings needed?
        syslogCharsetField.setModel(new DefaultComboBoxModel(new Object[] {
                "ASCII",
                "LATIN-1",
                "UTF-8"
        }));
        syslogCharsetField.setSelectedItem("UTF-8");

        // Put all possible timezones into the timezone field.
        String[] tzIds = TimeZone.getAvailableIDs();
        Arrays.sort(tzIds);
        String[] tzValues = new String[tzIds.length + 1];
        tzValues[0] = resources.getString("syslogSettings.timezone.values.useExisting");
        System.arraycopy(tzIds, 0, tzValues, 1, tzIds.length);
        syslogTimezoneField.setModel(new DefaultComboBoxModel(tzValues));
        syslogTimezoneField.setSelectedItem(resources.getString("syslogSettings.timezone.values.useExisting"));

        // Setup the test button
        inputValidator.attachToButton(syslogTestMessageButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SinkConfiguration newSinkConfiguration = null;
                synchronized(SinkConfigurationPropertiesDialog.this) {
                    SinkConfiguration oldSinkConfiguration = sinkConfiguration;

                    try {
                        sinkConfiguration = new SinkConfiguration();
                        viewToModel();
                        newSinkConfiguration = sinkConfiguration;
                    } finally {
                        sinkConfiguration = oldSinkConfiguration;
                    }
                }

                if(newSinkConfiguration == null) {
                    return;
                }

                Registry reg = Registry.getDefault();
                if (!reg.isAdminContextPresent())
                    return;
                LogSinkAdmin logSinkAdmin = reg.getLogSinkAdmin();
                if(logSinkAdmin == null) {
                    return;
                }

                logSinkAdmin.sendTestSyslogMessage(newSinkConfiguration);
            }
        });

        // If type is set to SYSLOG, then the syslog host must not be empty
        inputValidator.constrainTextField(syslogHostField, new InputValidator.ComponentValidationRule(syslogHostField) {
            public String getValidationError() {
                if(((TypeComboBoxValue)typeField.getSelectedItem()).value != SinkType.SYSLOG) {
                    return null;
                }

                if(syslogHostField.getText().length() == 0) {
                    return resources.getString("syslogSettings.host.errors.empty");
                }

                return null;
            }
        });
        // If type is set to SYSLOG, then the syslog port must be between 0 and 65535
        inputValidator.constrainTextField(syslogPortField, new InputValidator.ComponentValidationRule(syslogPortField) {
            public String getValidationError() {
                if(((TypeComboBoxValue)typeField.getSelectedItem()).value != SinkType.SYSLOG) {
                    return null;
                }

                if(syslogPortField.getText().length() == 0) {
                    return resources.getString("syslogSettings.port.errors.invalid");
                }

                try {
                    int value = Integer.parseInt(syslogPortField.getText());
                    if(value < 0 || value > 65535) {
                        return resources.getString("syslogSettings.port.errors.invalid");
                    }
                } catch(NumberFormatException e) {
                    return resources.getString("syslogSettings.port.errors.invalid");
                }

                return null;
            }
        });
    }

    /**
     * Updates the enabled status of the File Settings and Syslog Settings tabs based on the new value
     * of the type field.
     */
    private void enableDisableTabs() {
        SinkType type = ((TypeComboBoxValue)typeField.getSelectedItem()).value;
        switch(type) {
        case FILE:
            tabbedPane.setEnabledAt(1, true);
            tabbedPane.setEnabledAt(2, false);
            break;
        case SYSLOG:
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setEnabledAt(2, true);
            break;
        default:
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setEnabledAt(2, false);
            break;
        }
    }

    /**
     * Updates the dialog fields to match the values from the backing SinkConfiguration.
     */
    private synchronized void modelToView() {
        modelToViewBase();
        if(sinkConfiguration.getType() == null) {
            modelToViewFile();
        } else {
            switch(sinkConfiguration.getType()) {
            case FILE:
                modelToViewFile();
                break;
            case SYSLOG:
                modelToViewSyslog();
                break;
            }
        }
    }

    /**
     * Updates the base settings fields to match the values from the backing SinkConfiguration.
     */
    private void modelToViewBase() {
        nameField.setText(sinkConfiguration.getName());
        enabledField.setSelected(sinkConfiguration.isEnabled());
        descriptionField.setText(sinkConfiguration.getDescription());

        for(int i = 0;i < typeField.getModel().getSize();i++) {
            TypeComboBoxValue value = (TypeComboBoxValue)typeField.getItemAt(i);
            if(sinkConfiguration.getType() == null && value.value == SinkType.FILE ||
                    sinkConfiguration.getType() == value.value)
            {
                typeField.setSelectedIndex(i);
                break;
            }
        }

        for(int i = 0;i < severityField.getModel().getSize();i++) {
            SeverityComboBoxValue value = (SeverityComboBoxValue)severityField.getItemAt(i);
            if(sinkConfiguration.getSeverity() == null && value.value == SeverityThreshold.INFO ||
                    sinkConfiguration.getSeverity() == value.value)
            {
                severityField.setSelectedIndex(i);
                break;
            }
        }
        
        categoriesList.clearSelection();
        if(sinkConfiguration.getCategories() != null) {
            HashSet<String> values = new HashSet<String>();
            String[] categories = sinkConfiguration.getCategories().split(",");
            values.addAll(Arrays.<String>asList(categories));

            for(int i = 0;i < categoriesList.getModel().getSize();i++) {
                if(values.contains(categoriesList.getModel().getElementAt(i))) {
                    categoriesList.getSelectionModel().addSelectionInterval(i, i);
                }
            }
        }
    }

    /**
     * Updates the file settings fields to match the values from the backing SinkConfiguration.
     */
    private void modelToViewFile() {
        String value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_MAX_SIZE);
        fileMaxSizeField.setValue(value == null ? 1024 : Integer.parseInt(value));
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_LOG_COUNT);
        fileLogCount.setValue(value == null ? 1 : Integer.parseInt(value));
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_FORMAT);
        if(value == null) {
            fileFormatField.setSelectedIndex(1);
        } else {
            for(int i = 0;i < fileFormatField.getItemCount();i++) {
                FormatComboBoxValue formatValue = (FormatComboBoxValue)fileFormatField.getItemAt(i);
                if(formatValue.value.equals(value)) {
                    fileFormatField.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Updates the syslog settings to match the values from the backing SinkConfiguration.
     */
    private void modelToViewSyslog() {
        String value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL);
        if(value == null || value.equals(SinkConfiguration.PROP_SYSLOG_PROTOCOL_TCP)) {
            syslogProtocolField.setSelectedItem(resources.getString("syslogSettings.protocol.TCP.text"));
        } else if(value.equals(SinkConfiguration.PROP_SYSLOG_PROTOCOL_UDP)) {
            syslogProtocolField.setSelectedItem(resources.getString("syslogSettings.protocol.UDP.text"));
        }
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_HOST);
        syslogHostField.setText(value);
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_PORT);
        syslogPortField.setText(value);
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_FACILITY);
        syslogFacilityField.setValue(value == null ? 1 : new Integer(value));
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME);
        syslogLogHostnameField.setSelected(value == null || Boolean.parseBoolean(value));
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_CHAR_SET);
        syslogCharsetField.setSelectedItem(value == null ? "UTF-8" : value);
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE);
        syslogTimezoneField.setSelectedItem(value == null ? resources.getString("syslogSettings.timezone.values.useExisting") : value);
    }

    /**
     * Updates the backing SinkConfiguration with the values from this dialog.
     */
    private synchronized void viewToModel() {
        viewToModelBase();
        switch(sinkConfiguration.getType()) {
        case FILE:
            viewToModelFile();
            break;
        case SYSLOG:
            viewToModelSyslog();
            break;
        }
    }

    /**
     * Updates the backing SinkConfiguration with the base settings field values.
     */
    private void viewToModelBase() {
        sinkConfiguration.setName(nameField.getText());
        sinkConfiguration.setEnabled(enabledField.isSelected());
        sinkConfiguration.setDescription(descriptionField.getText());
        sinkConfiguration.setType(((TypeComboBoxValue)typeField.getSelectedItem()).value);
        sinkConfiguration.setSeverity(((SeverityComboBoxValue)severityField.getSelectedItem()).value);
        StringBuilder sb = new StringBuilder();
        for(Object selectedValue : categoriesList.getSelectedValues()) {
            if(sb.length() > 0) {
                sb.append(',');
            }
            sb.append(selectedValue);
        }
        sinkConfiguration.setCategories(sb.toString());
    }

    /**
     * Updates the backing SinkConfiguration with the file settings field values.
     */
    private void viewToModelFile() {
        sinkConfiguration.setProperty(SinkConfiguration.PROP_FILE_MAX_SIZE, fileMaxSizeField.getValue().toString());
        sinkConfiguration.setProperty(SinkConfiguration.PROP_FILE_LOG_COUNT, fileLogCount.getValue().toString());
        sinkConfiguration.setProperty(SinkConfiguration.PROP_FILE_FORMAT, ((FormatComboBoxValue)fileFormatField.getSelectedItem()).value);
    }

    /**
     * Updates the backing SinkConfiguration with the syslog settings field values.
     */
    private void viewToModelSyslog() {
        String value = (String) syslogProtocolField.getSelectedItem();
        if (value.equals(resources.getString("syslogSettings.protocol.TCP.text"))) {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL, SinkConfiguration.PROP_SYSLOG_PROTOCOL_TCP);
        } else if (value.equals(resources.getString("syslogSettings.protocol.UDP.text"))) {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL, SinkConfiguration.PROP_SYSLOG_PROTOCOL_UDP);
        }

        sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL, (String) syslogProtocolField.getSelectedItem());
        sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_HOST, syslogHostField.getText());
        sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_PORT, syslogPortField.getText());
        sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_FACILITY, syslogFacilityField.getValue().toString());

        if (syslogLogHostnameField.isSelected()) {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME, Boolean.TRUE.toString());
        } else {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME, Boolean.FALSE.toString());
        }

        sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_CHAR_SET, (String) syslogCharsetField.getSelectedItem());

        value = (String) syslogTimezoneField.getSelectedItem();
        if (value != null && !value.equals(resources.getString("syslogSettings.timezone.values.useExisting"))) {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE, (String) syslogTimezoneField.getSelectedItem());
        } else {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE, null);
        }
    }

    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    private void onOk() {
        viewToModel();
        confirmed = true;
        dispose();
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }

    public static void main(String[] args) {
        Frame f = new JFrame();
        f.setVisible(true);
        SinkConfiguration sc = new SinkConfiguration();
        SinkConfigurationPropertiesDialog s = new SinkConfigurationPropertiesDialog(f, sc);
        s.setVisible(true);
        s.dispose();
        f.dispose();
    }
}
