package com.l7tech.console.panels;

import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.log.SinkConfiguration;
import static com.l7tech.common.log.SinkConfiguration.SeverityThreshold;
import static com.l7tech.common.log.SinkConfiguration.SinkType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TimeZone;

/**
 * This is the dialog for updating the properties of a log sink configuration.
 */
public class SinkConfigurationPropertiesDialog extends JDialog {
    /** The title for the dialog window */
    private static final String DIALOG_TITLE = "Log Sink Properties";

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
    private JTextField syslogFacilityField;
    private JTextField syslogLogHostnameField;
    private JComboBox syslogCharsetField;
    private JComboBox syslogTimezoneField;

    private final InputValidator inputValidator = new InputValidator(this, DIALOG_TITLE);
    private SinkConfiguration sinkConfiguration;
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

    private void initialize(SinkConfiguration sinkConfiguration) {
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
        typeField.setModel(new DefaultComboBoxModel(SinkType.values()));
        typeField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableDisableTabs();
            }
        });

        severityField.setModel(new DefaultComboBoxModel(SeverityThreshold.values()));

        categoriesList.setListData(SinkConfiguration.CATEGORIES_SET.toArray());

        JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(fileMaxSizeField);
        fileMaxSizeField.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(1);
        numberEditor.getModel().setValue(1);

        numberEditor = new JSpinner.NumberEditor(fileLogCount);
        fileLogCount.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(1);
        numberEditor.getModel().setValue(1);

        syslogProtocolField.setModel(new DefaultComboBoxModel(new Object[] {
                SinkConfiguration.PROP_SYSLOG_PROTOCOL_TCP,
                SinkConfiguration.PROP_SYSLOG_PROTOCOL_UDP
        }));

        // Currently only give options for ASCII, LATIN-1 and UTF-8. Are more encodings needed?
        syslogCharsetField.setModel(new DefaultComboBoxModel(new Object[] {
                "ASCII",
                "LATIN-1",
                "UTF-8"
        }));

        // Put all possible timezones into the timezone field.
        String[] tzIds = TimeZone.getAvailableIDs();
        Arrays.sort(tzIds);
        syslogTimezoneField.setModel(new DefaultComboBoxModel(tzIds));
        syslogTimezoneField.setSelectedItem("UTC");

        // Name field must not be empty
        inputValidator.constrainTextFieldToBeNonEmpty("Name", nameField, null);
        inputValidator.validateWhenDocumentChanges(nameField);
        // If type is set to SYSLOG, then the syslog host must not be empty
        inputValidator.constrainTextField(syslogHostField, new InputValidator.ComponentValidationRule(syslogHostField) {
            public String getValidationError() {
                if(typeField.getSelectedItem() != SinkType.SYSLOG) {
                    return null;
                }

                if(syslogHostField.getText().length() == 0) {
                    return "The Syslog Host field must not be empty.";
                }

                return null;
            }
        });
        // If type is set to SYSLOG, then the syslog port must be between 0 and 65535
        inputValidator.constrainTextField(syslogPortField, new InputValidator.ComponentValidationRule(syslogPortField) {
            public String getValidationError() {
                if(typeField.getSelectedItem() != SinkType.SYSLOG) {
                    return null;
                }

                if(syslogPortField.getText().length() == 0) {
                    return "The Syslog Port field must be a number between 0 and 65535.";
                }

                try {
                    int value = Integer.parseInt(syslogPortField.getText());
                    if(value < 0 || value > 65535) {
                        return "The Syslog Port field must be a number between 0 and 65535.";
                    }
                } catch(NumberFormatException e) {
                    return "The Syslog Port field must be a number between 0 and 65535.";
                }

                return null;
            }
        });
        // If the type is set to SYSLOG, then the syslog facility must not be empty
        inputValidator.constrainTextField(syslogFacilityField, new InputValidator.ComponentValidationRule(syslogFacilityField) {
            public String getValidationError() {
                if(typeField.getSelectedItem() != SinkType.SYSLOG) {
                    return null;
                }

                if(syslogFacilityField.getText().length() == 0) {
                    return "The Syslog Facility field must not be empty.";
                }

                return null;
            }
        });
        // If the type is set to SYSLOG, then the syslog log hostname must not be empty
        inputValidator.constrainTextField(syslogLogHostnameField, new InputValidator.ComponentValidationRule(syslogLogHostnameField) {
            public String getValidationError() {
                if(typeField.getSelectedItem() != SinkType.SYSLOG) {
                    return null;
                }

                if(syslogLogHostnameField.getText().length() == 0) {
                    return "The Syslog Log Hostname field must not be empty.";
                }

                return null;
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        // Update all of the fields using the values from SinkConfiguration
        modelToView();
        if (nameField.getText().length() < 1)
            nameField.requestFocusInWindow();
    }

    private void enableDisableTabs() {
        SinkType type = (SinkType)typeField.getSelectedItem();
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

    private void modelToView() {
        nameField.setText(sinkConfiguration.getName());
        enabledField.setSelected(sinkConfiguration.isEnabled());
        descriptionField.setText(sinkConfiguration.getDescription());
        typeField.setSelectedItem(sinkConfiguration.getType() == null ? SinkType.FILE : sinkConfiguration.getType());
        severityField.setSelectedItem(sinkConfiguration.getSeverity() == null ? SeverityThreshold.INFO : sinkConfiguration.getSeverity());
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

        if(sinkConfiguration.getType() == SinkType.FILE) {
            String value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_MAX_SIZE);
            fileMaxSizeField.setValue(value == null ? 1024 : Integer.parseInt(value));
            value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_LOG_COUNT);
            fileLogCount.setValue(value == null ? 1 : Integer.parseInt(value));
        } else if(sinkConfiguration.getType() == SinkType.SYSLOG) {
            String value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL);
            syslogProtocolField.setSelectedItem(value == null ? SinkConfiguration.PROP_SYSLOG_PROTOCOL_TCP : value);
            value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_HOST);
            syslogHostField.setText(value);
            value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_PORT);
            syslogPortField.setText(value);
            value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_FACILITY);
            syslogFacilityField.setText(value);
            value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME);
            syslogLogHostnameField.setText(value);
            value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_CHAR_SET);
            syslogCharsetField.setSelectedItem(value == null ? "ASCII" : value);
            value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE);
            syslogTimezoneField.setSelectedItem(value == null ? "UTC" : value);
        }
    }

    private void viewToModel() {
        sinkConfiguration.setName(nameField.getText());
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

        if(sinkConfiguration.getType() == SinkType.FILE) {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_FILE_MAX_SIZE, fileMaxSizeField.getValue().toString());
            sinkConfiguration.setProperty(SinkConfiguration.PROP_FILE_LOG_COUNT, fileLogCount.getValue().toString());
        } else if(sinkConfiguration.getType() == SinkType.SYSLOG) {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL, (String)syslogProtocolField.getSelectedItem());
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_HOST, syslogHostField.getText());
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_PORT, syslogPortField.getText());
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_FACILITY, syslogFacilityField.getText());
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME, syslogLogHostnameField.getText());
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_CHAR_SET, (String)syslogCharsetField.getSelectedItem());
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE, (String)syslogTimezoneField.getSelectedItem());
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
