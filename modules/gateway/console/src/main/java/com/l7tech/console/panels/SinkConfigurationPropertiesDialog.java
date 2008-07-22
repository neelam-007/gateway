package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import static com.l7tech.gateway.common.log.SinkConfiguration.SeverityThreshold;
import static com.l7tech.gateway.common.log.SinkConfiguration.SinkType;
import com.l7tech.util.ValidationUtils;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.text.MessageFormat;

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

    private final SinkConfiguration sinkConfiguration;
    private InputValidator inputValidator;
    private int testCount = 0;
    private boolean confirmed = false;

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
        this.sinkConfiguration = sinkConfiguration;
        initialize();
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
        this.sinkConfiguration = sinkConfiguration;
        initialize();
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.SinkConfigurationPropertiesDialog", locale);
    }

    /**
     * Initializes this dialog window and sets all of the fields based on the provided SinkConfiguration
     * object.
     */
    private void initialize() {
        initializeBaseFields();
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
     */
    private void initializeBaseFields() {
        initResources();

        setTitle(resources.getString("sinkConfigurationProperties.window.title"));
        inputValidator = new InputValidator(this, resources.getString("sinkConfigurationProperties.window.title"));

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
        typeField.setModel(new DefaultComboBoxModel(SinkType.values()));
        typeField.setRenderer(new KeyedResourceRenderer(resources, "baseSettings.type.{0}.text"));
        typeField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableDisableTabs();
            }
        });

        severityField.setModel(new DefaultComboBoxModel(SeverityThreshold.values()));
        severityField.setRenderer(new KeyedResourceRenderer(resources, "baseSettings.severity.{0}.text"));

        categoriesList.setListData(SinkConfiguration.CATEGORIES_SET.toArray());
        categoriesList.setCellRenderer(new KeyedResourceRenderer(resources, "baseSettings.categories.{0}.text"));
        inputValidator.addRule(new InputValidator.ValidationRule(){
            public String getValidationError() {
                String error = null;

                if ( categoriesList.getSelectedValues().length==0 ) {
                    return resources.getString("baseSettings.categories.errors.empty");
                }

                return error;
            }
        });

        // Name field must not be empty and must not be longer than 128 characters
        ((AbstractDocument)nameField.getDocument()).setDocumentFilter(new DocumentSizeFilter(128));
        inputValidator.constrainTextFieldToBeNonEmpty("Name", nameField, new InputValidator.ComponentValidationRule(nameField) {
            public String getValidationError() {
                if( nameField.getText().trim().length()==0 ) {
                    return resources.getString("baseSettings.name.errors.empty");
                } else if ( !ValidationUtils.isValidCharacters(nameField.getText().trim(), ValidationUtils.ALPHA_NUMERIC + "_-") ) {
                    return resources.getString("baseSettings.name.errors.chars");
                }

                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges(nameField);

        // Description field must not be longer than 1000 characters
        ((AbstractDocument)descriptionField.getDocument()).setDocumentFilter(new DocumentSizeFilter(1000));
    }

    /**
     * Initializes the file settings fields.
     */
    private void initializeFileFields() {
        JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(fileMaxSizeField);
        fileMaxSizeField.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(1);
        numberEditor.getModel().setMaximum(1024 * 1024);
        numberEditor.getModel().setValue(1024);

        numberEditor = new JSpinner.NumberEditor(fileLogCount);
        fileLogCount.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(1);
        numberEditor.getModel().setMaximum(100);
        numberEditor.getModel().setValue(1);

        fileFormatField.setModel(new DefaultComboBoxModel(SinkConfiguration.FILE_FORMAT_SET.toArray()));
        fileFormatField.setRenderer(new KeyedResourceRenderer(resources, "fileSettings.format.{0}.text"));
        fileFormatField.setSelectedIndex(1);

        // add validation rule for maximum file use
        inputValidator.addRule(new InputValidator.ValidationRule(){
            public String getValidationError() {
                String error = null;

                long maximum = getMaximumLogFileSize();
                long reserved = getReservedLogFileSize();

                // deduct space used for previous configuration
                reserved -= getSpaceUsed( sinkConfiguration );

                // add space used for this configuration
                SinkConfiguration tempConfig = new SinkConfiguration();
                viewToModel( tempConfig );
                reserved +=  getSpaceUsed( tempConfig );

                if ( enabledField.isSelected() &&
                     reserved > maximum ) {
                    return MessageFormat.format(
                            resources.getString("fileSettings.maxFileSize.errors.toolarge"),
                            maximum,
                            reserved);
                }

                return error;
            }
        });
    }

    /**
     * Get the space used for the given sinkConfiguration.
     */
    private long getSpaceUsed( final SinkConfiguration sinkConfiguration ) {
        long space = 0L;

        if ( sinkConfiguration.isEnabled() && SinkConfiguration.SinkType.FILE == sinkConfiguration.getType() ) {
            try {
                long limit = Long.parseLong( sinkConfiguration.getProperty( SinkConfiguration.PROP_FILE_MAX_SIZE ) );
                long count = Long.parseLong( sinkConfiguration.getProperty( SinkConfiguration.PROP_FILE_LOG_COUNT ) );

                return 1024L * limit * count;
            } catch (NumberFormatException nfe) {
            }
        }

        return space;
    }

    /**
     * Initializes the syslog settings fields.
     */
    private void initializeSyslogFields() {
        syslogProtocolField.setModel(new DefaultComboBoxModel(SinkConfiguration.SYSLOG_PROTOCOL_SET.toArray()));
        syslogProtocolField.setRenderer(new KeyedResourceRenderer(resources, "syslogSettings.protocol.{0}.text"));

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
                SinkConfiguration newSinkConfiguration = new SinkConfiguration();
                viewToModel( newSinkConfiguration );

                Registry reg = Registry.getDefault();
                if (!reg.isAdminContextPresent())
                    return;
                LogSinkAdmin logSinkAdmin = reg.getLogSinkAdmin();
                if(logSinkAdmin == null) {
                    return;
                }

                logSinkAdmin.sendTestSyslogMessage(newSinkConfiguration, "Test message " + (++testCount) + ".");
            }
        });

        // If type is set to SYSLOG, then the syslog host must not be empty
        ((AbstractDocument)syslogHostField.getDocument()).setDocumentFilter(new DocumentSizeFilter(512));
        inputValidator.constrainTextField(syslogHostField, new InputValidator.ComponentValidationRule(syslogHostField) {
            public String getValidationError() {
                if( typeField.getSelectedItem() != SinkType.SYSLOG ) {
                    return null;
                }

                if( syslogHostField.getText().length() == 0 ) {
                    return resources.getString("syslogSettings.host.errors.empty");
                }

                if( !ValidationUtils.isValidDomain(syslogHostField.getText()) ) {
                    return resources.getString("syslogSettings.host.errors.invalid");
                }

                return null;
            }
        });
        // If type is set to SYSLOG, then the syslog port must be between 0 and 65535
        ((AbstractDocument)syslogPortField.getDocument()).setDocumentFilter(new DocumentSizeFilter(5));
        inputValidator.constrainTextField(syslogPortField, new InputValidator.ComponentValidationRule(syslogPortField) {
            public String getValidationError() {
                if( typeField.getSelectedItem() != SinkType.SYSLOG) {
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
     * Get the reserved space for current log configuration (bytes)
     */
    private long getReservedLogFileSize() {
        try {
            LogSinkAdmin logSinkAdmin = Registry.getDefault().getLogSinkAdmin();
            return logSinkAdmin.getReservedFileSize();
        } catch ( IllegalStateException ise ) {
            // no longer connected to server, just return a value that will validate
            return 0L;
        }
    }

    /**
     * Get the maximum space for all log configurations (bytes) 
     */
    private long getMaximumLogFileSize() {
        try {
            LogSinkAdmin logSinkAdmin = Registry.getDefault().getLogSinkAdmin();
            return logSinkAdmin.getMaximumFileSize();
        } catch ( IllegalStateException ise ) {
            // no longer connected to server, just return a value that will validate
            return Long.MAX_VALUE;
        }
    }

    /**
     * Updates the enabled status of the File Settings and Syslog Settings tabs based on the new value
     * of the type field.
     */
    private void enableDisableTabs() {
        SinkType type = (SinkType) typeField.getSelectedItem();
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
    private void modelToView() {
        modelToView(sinkConfiguration);
    }

    /**
     * Updates the dialog fields to match the values from the given SinkConfiguration.
     */
    private void modelToView(final SinkConfiguration sinkConfiguration) {
        modelToViewBase(sinkConfiguration);
        if(sinkConfiguration.getType() == null) {
            modelToViewFile(sinkConfiguration);
        } else {
            switch(sinkConfiguration.getType()) {
            case FILE:
                modelToViewFile(sinkConfiguration);
                break;
            case SYSLOG:
                modelToViewSyslog(sinkConfiguration);
                break;
            }
        }
    }

    /**
     * Updates the base settings fields to match the values from the given SinkConfiguration.
     */
    private void modelToViewBase(final SinkConfiguration sinkConfiguration) {
        if ( sinkConfiguration.getOid() != SinkConfiguration.DEFAULT_OID ) {
            nameField.setEditable(false);
        }
        nameField.setText(sinkConfiguration.getName());
        enabledField.setSelected(sinkConfiguration.isEnabled());
        descriptionField.setText(sinkConfiguration.getDescription());
        if ( sinkConfiguration.getType()!=null )
            typeField.setSelectedItem(sinkConfiguration.getType());
        else
            typeField.setSelectedItem(SinkType.FILE);
        if ( sinkConfiguration.getSeverity()!=null )
            severityField.setSelectedItem(sinkConfiguration.getSeverity());
        else
            severityField.setSelectedItem(SeverityThreshold.INFO);

        categoriesList.clearSelection();
        boolean selected = false;
        if(sinkConfiguration.getCategories() != null) {
            HashSet<String> values = new HashSet<String>();
            String[] categories = sinkConfiguration.getCategories().split(",");
            values.addAll(Arrays.<String>asList(categories));

            for(int i = 0;i < categoriesList.getModel().getSize();i++) {
                if(values.contains(categoriesList.getModel().getElementAt(i))) {
                    selected = true;
                    categoriesList.getSelectionModel().addSelectionInterval(i, i);
                }
            }
        }
        if (!selected) {
            categoriesList.setSelectedValue(SinkConfiguration.CATEGORY_GATEWAY_LOGS, true);
        }
    }

    /**
     * Updates the file settings fields to match the values from the given SinkConfiguration.
     */
    private void modelToViewFile(final SinkConfiguration sinkConfiguration) {
        String value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_MAX_SIZE);
        fileMaxSizeField.setValue(value == null ? 1024 : Integer.parseInt(value));
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_LOG_COUNT);
        fileLogCount.setValue(value == null ? 2 : Integer.parseInt(value));
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_FORMAT);
        if(value == null) {
            fileFormatField.setSelectedItem(SinkConfiguration.FILE_FORMAT_STANDARD);
        } else {
            fileFormatField.setSelectedItem(value);
        }
    }

    /**
     * Updates the syslog settings to match the values from the given SinkConfiguration.
     */
    private void modelToViewSyslog(final SinkConfiguration sinkConfiguration) {
        String value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL);
        if( value == null ) {
            syslogProtocolField.setSelectedItem(SinkConfiguration.SYSLOG_PROTOCOL_TCP);
        } else {
            syslogProtocolField.setSelectedItem(value);
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
    private void viewToModel() {
        viewToModel(sinkConfiguration);
    }

    /**
     * Updates the backing SinkConfiguration with the values from this dialog.
     */
    private void viewToModel(final SinkConfiguration sinkConfiguration) {
        viewToModelBase(sinkConfiguration);
        switch(sinkConfiguration.getType()) {
        case FILE:
            viewToModelFile(sinkConfiguration);
            break;
        case SYSLOG:
            viewToModelSyslog(sinkConfiguration);
            break;
        }
    }

    /**
     * Updates the backing SinkConfiguration with the base settings field values.
     */
    private void viewToModelBase(final SinkConfiguration sinkConfiguration) {
        if ( nameField.isEditable())
            sinkConfiguration.setName(nameField.getText().trim());
        sinkConfiguration.setEnabled(enabledField.isSelected());
        sinkConfiguration.setDescription(descriptionField.getText());
        sinkConfiguration.setType((SinkType)typeField.getSelectedItem());
        sinkConfiguration.setSeverity((SeverityThreshold)severityField.getSelectedItem());
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
    private void viewToModelFile(final SinkConfiguration sinkConfiguration) {
        sinkConfiguration.setProperty(SinkConfiguration.PROP_FILE_MAX_SIZE, fileMaxSizeField.getValue().toString());
        sinkConfiguration.setProperty(SinkConfiguration.PROP_FILE_LOG_COUNT, fileLogCount.getValue().toString());
        sinkConfiguration.setProperty(SinkConfiguration.PROP_FILE_FORMAT, (String)fileFormatField.getSelectedItem());
    }

    /**
     * Updates the backing SinkConfiguration with the syslog settings field values.
     */
    private void viewToModelSyslog(final SinkConfiguration sinkConfiguration) {
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

        String value = (String) syslogTimezoneField.getSelectedItem();
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

    /**
     * Renderer for items keyed into given resource bundle.
     */
    private static final class KeyedResourceRenderer extends JLabel implements ListCellRenderer {
        private final ResourceBundle bundle;
        private final String keyFormat;

        public KeyedResourceRenderer( final ResourceBundle bundle,
                                      final String keyFormat ) {
            this.bundle = bundle;
            this.keyFormat = keyFormat;
        }

        public Component getListCellRendererComponent( JList list,
                                                       Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean cellHasFocus)
        {
            Object[] keyFormatArgs = new Object[]{ value };

            String label = "";
            if ( value != null ) {
                label = bundle.getString(MessageFormat.format(keyFormat, keyFormatArgs));
            }

            setText(label);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                setOpaque(true);
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                setOpaque(false);
            }

            setEnabled(list.isEnabled());
            setFont(list.getFont());

            return this;
        }
    }
}
