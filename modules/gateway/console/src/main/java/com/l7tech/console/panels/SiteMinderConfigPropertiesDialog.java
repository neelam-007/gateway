package com.l7tech.console.panels;

import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.siteminder.SiteMinderFipsModeOption;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.MutablePair;
import sun.security.util.Resources;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 * @author nilic
 * Date: 7/22/13
 * Time: 4:36 PM
 */
public class SiteMinderConfigPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SiteMinderConfigPropertiesDialog.class.getName());

    private static final ResourceBundle RESOURCES =
            Resources.getBundle("com.l7tech.console.panels.resources.SiteMinderConfigPropertiesDialog");

    private static final int MAX_TABLE_COLUMN_NUM = 2;
    private static final int CLUSTER_THRESHOLD_MIN = 1;
    private static final int CLUSTER_THRESHOLD_MAX = 100;
    private static final int CLUSTER_SERVER_CONN_MIN = 1;
    private static final int CLUSTER_SERVER_CONN_MAX = 3;
    private static final int CLUSTER_SERVER_CONN_STEP = 1;

    private final Map<String, String> clusterSettingsMap = new TreeMap<>();

    private boolean confirmed;

    private PermissionFlags flags;
    private SiteMinderConfiguration configuration;
    private SquigglyTextField configurationNameTextField;
    private JTextField agentNameTextField;
    private JTextField addressTextField;
    private JCheckBox checkIPCheckBox;
    private JTextField hostNameTextField;
    private JComboBox<SiteMinderFipsModeOption> fipsModeComboBox;
    private JCheckBox enableFailoverCheckBox;
    private JTable clusterSettingsTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JSpinner clusterThresholdSpinner;
    private AbstractTableModel clusterSettingTableModel;
    private SecurityZoneWidget zoneControl;
    private JButton registerButton;
    private JCheckBox updateSSOTokenCheckBox;
    private JPasswordField secretPasswordField;
    private JButton testButton;
    private JCheckBox disableCheckBox;

    public SiteMinderConfigPropertiesDialog(Frame owner, SiteMinderConfiguration configuration) {
        super(owner, RESOURCES.getString("dialog.title.siteminder.configuration.properties"));
        initialize(configuration);
    }

    public SiteMinderConfigPropertiesDialog(Dialog owner, SiteMinderConfiguration configuration) {
        super(owner, RESOURCES.getString("dialog.title.siteminder.configuration.properties"));
        initialize(configuration);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void initialize(SiteMinderConfiguration configuration) {
        flags = PermissionFlags.get(EntityType.SITEMINDER_CONFIGURATION);

        this.configuration = configuration;

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);

        configurationNameTextField.setDocument(new MaxLengthDocument(128));
        agentNameTextField.setDocument(new MaxLengthDocument(256));
        secretPasswordField.setDocument(new MaxLengthDocument(4096));
        addressTextField.setDocument(new MaxLengthDocument(128));
        hostNameTextField.setDocument(new MaxLengthDocument(255));
        ((JTextField)fipsModeComboBox.getEditor().getEditorComponent()).setDocument(new MaxLengthDocument(255));
        fipsModeComboBox.setModel(new DefaultComboBoxModel<>(SiteMinderFipsModeOption.values()));
        clusterThresholdSpinner.setModel(new SpinnerNumberModel(50, CLUSTER_THRESHOLD_MIN, CLUSTER_THRESHOLD_MAX, 1));

        initClusterSettingsTable();

        InputValidator.ValidationRule clusterThresholdRule =
                new InputValidator.NumberSpinnerValidationRule(clusterThresholdSpinner,
                        RESOURCES.getString("property.agent.clusterThreshold"));

        InputValidator.ValidationRule ipCheckBoxRule =
                new InputValidator.ComponentValidationRule(checkIPCheckBox) {
                    @Override
                    public String getValidationError() {
                        if (checkIPCheckBox.isSelected()) {
                            final String address = addressTextField.getText();

                            if (address == null || address.trim().isEmpty()) {
                                return "The " + RESOURCES.getString("property.agent.address") +
                                        " field must not be empty if Check IP is enabled.";
                            }
                        }

                        return null;
                    }
                };

        InputValidator testValidator =
                new InputValidator(this, RESOURCES.getString("dialog.title.siteminder.configuration.properties"));

        InputValidator okValidator =
                new InputValidator(this, RESOURCES.getString("dialog.title.siteminder.configuration.properties"));

        okValidator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.configurationName"), configurationNameTextField, null);
        okValidator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.agent.name"), agentNameTextField, null);
        okValidator.constrainPasswordFieldToBeNonEmpty(RESOURCES.getString("property.agent.secret"), secretPasswordField);
        okValidator.addRule(ipCheckBoxRule);
        okValidator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.agent.hostname"), hostNameTextField, null);
        okValidator.ensureComboBoxSelection(RESOURCES.getString("property.agent.fipsMode"), fipsModeComboBox);
        okValidator.addRule(clusterThresholdRule);

        testValidator.constrainPasswordFieldToBeNonEmpty(RESOURCES.getString("property.agent.secret"), secretPasswordField);
        testValidator.addRule(ipCheckBoxRule);
        testValidator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.agent.hostname"), hostNameTextField, null);
        testValidator.ensureComboBoxSelection(RESOURCES.getString("property.agent.fipsMode"), fipsModeComboBox);
        testValidator.addRule(clusterThresholdRule);

        okValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOK();
            }
        });

        testValidator.attachToButton(testButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doTest();
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAdd();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doRemove();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doRegister();
            }
        });

        zoneControl.configure(this.configuration);

        modelToView();

        Utilities.setMinimumSize(this);
    }

    private void doAdd() {
        editAndSave(new MutablePair<>("", ""));
    }

    private void doEdit() {
        int selectedRow = clusterSettingsTable.getSelectedRow();

        if (selectedRow < 0) return;

        String propName = (String) clusterSettingsMap.keySet().toArray()[selectedRow];
        String propValue = clusterSettingsMap.get(propName);

        editAndSave(new MutablePair<>(propName, propValue));
    }

    private void doRemove() {
        int currentRow = clusterSettingsTable.getSelectedRow();
        if (currentRow < 0) return;

        String propName = (String) clusterSettingsMap.keySet().toArray()[currentRow];
        Object[] options = {RESOURCES.getString("button.remove"), RESOURCES.getString("button.cancel")};
        int result = JOptionPane.showOptionDialog(
                this, MessageFormat.format(RESOURCES.getString("confirmation.remove.cluster.settings.property"), propName),
                RESOURCES.getString("dialog.title.remove.cluster.settings.property"), JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0) {
            // Refresh the list
            clusterSettingsMap.remove(propName);

            // Refresh the table
            clusterSettingTableModel.fireTableDataChanged();
            // Refresh the selection highlight
            if (currentRow == clusterSettingsMap.size()) currentRow--; // If the previous deleted row was the last row
            if (currentRow >= 0) clusterSettingsTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
        }
    }

    private void doOK() {
        if (validateSiteMinderConfigurationNameUnique()) {
            viewToModel();

            confirmed = true;

            dispose();
        }
    }

    private void doTest() {
        if (validateSiteMinderConfiguration()) {
            DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this,
                    RESOURCES.getString("message.validation.siteminder.config.passed"),
                    RESOURCES.getString("dialog.title.siteminder.configuration.validation"),
                    JOptionPane.INFORMATION_MESSAGE, null);
        }
    }

    private void doCancel() {
        dispose();
    }

    private void doRegister() {
        register(new SiteMinderHost(configuration.getHostname(),
                clusterSettingsMap.get(RESOURCES.getString("property.cluster.server.address")),
                configuration.getHostConfiguration(),
                SiteMinderFipsModeOption.getByCode(configuration.getFipsmode()),
                configuration.getUserName(),
                configuration.getPasswordGoid(),
                zoneControl.getSelectedZone()));
    }

    private void register(final SiteMinderHost siteMinderHost) {
        final SiteMinderRegisterConfigDialog dlg = new SiteMinderRegisterConfigDialog(this, siteMinderHost);
        dlg.pack();
        Utilities.centerOnScreen(dlg);

        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    SiteMinderHost newHost = dlg.getSiteMinderHost();

                    configuration.setHostConfiguration(newHost.getHostConfigObject());
                    configuration.setUserName(newHost.getUserName());
                    configuration.setPasswordGoid(newHost.getPasswordGoid());

                    hostNameTextField.setText(newHost.getHostname());
                    secretPasswordField.setText(newHost.getSharedSecret());
                    addressTextField.setText("127.0.0.1");
                    fipsModeComboBox.setSelectedItem(newHost.getFipsMode());

                    Map<String, String> properties = new HashMap<>();

                    String[] clusterProperties = newHost.getPolicyServer().split(",");

                    if (clusterProperties.length == 4) {
                        properties.put(RESOURCES.getString("property.cluster.server.address"), clusterProperties[0]);
                        properties.put(RESOURCES.getString("property.cluster.server.accounting.port"), clusterProperties[1]);
                        properties.put(RESOURCES.getString("property.cluster.server.authentication.port"), clusterProperties[2]);
                        properties.put(RESOURCES.getString("property.cluster.server.authorization.port"), clusterProperties[3]);
                        properties.put(RESOURCES.getString("property.cluster.server.connection.min"), String.valueOf(CLUSTER_SERVER_CONN_MIN));
                        properties.put(RESOURCES.getString("property.cluster.server.connection.max"), String.valueOf(CLUSTER_SERVER_CONN_MAX));
                        properties.put(RESOURCES.getString("property.cluster.server.connection.step"), String.valueOf(CLUSTER_SERVER_CONN_STEP));
                        properties.put(RESOURCES.getString("property.cluster.server.timeout"), String.valueOf(newHost.getRequestTimeout()));
                    } else {
                        logger.log(Level.WARNING, "Unexpected number of SiteMinder Cluster Properties returned: " + newHost.getPolicyServer());
                    }

                    clusterSettingsMap.clear();

                    clusterSettingsMap.putAll(properties);

                    clusterSettingTableModel.fireTableDataChanged();
                }
            }
        });
    }

    /**
     * Assign model values to view.
     *
     * N.B. Security zone configuration is already handled in initialization.
     */
    private void modelToView() {
        configurationNameTextField.setText(configuration.getName());
        agentNameTextField.setText(configuration.getAgentName());
        secretPasswordField.setText(configuration.getSecret());
        addressTextField.setText(configuration.getAddress());
        checkIPCheckBox.setSelected(configuration.isIpcheck());
        updateSSOTokenCheckBox.setSelected(configuration.isUpdateSSOToken());
        hostNameTextField.setText(configuration.getHostname());
        disableCheckBox.setSelected(!configuration.isEnabled());

        SiteMinderFipsModeOption mode = SiteMinderFipsModeOption.getByCode(configuration.getFipsmode());

        // any unrecognized fips mode setting will be replaced with COMPAT
        fipsModeComboBox.setSelectedItem(mode == null ? SiteMinderFipsModeOption.COMPAT : mode);

        enableFailoverCheckBox.setSelected(configuration.isNonClusterFailover());
        clusterThresholdSpinner.setValue(configuration.getClusterThreshold());
        clusterSettingsMap.clear();

        if (configuration.getProperties() != null) {
            clusterSettingsMap.putAll(configuration.getProperties());
        }

        clusterSettingTableModel.fireTableDataChanged();
    }

    private void viewToModel() {
        configuration.setName(configurationNameTextField.getText().trim());
        configuration.setAgentName(agentNameTextField.getText().trim());
        configuration.setSecret(new String(secretPasswordField.getPassword()));
        configuration.setAddress(addressTextField.getText().trim());
        configuration.setIpcheck(checkIPCheckBox.isSelected());
        configuration.setUpdateSSOToken(updateSSOTokenCheckBox.isSelected());
        configuration.setHostname(hostNameTextField.getText().trim());
        configuration.setEnabled(!disableCheckBox.isSelected());

        int modeIndex = fipsModeComboBox.getSelectedIndex();
        SiteMinderFipsModeOption mode = modeIndex > -1 ? fipsModeComboBox.getItemAt(modeIndex) : SiteMinderFipsModeOption.COMPAT;

        configuration.setFipsmode(mode.getCode());
        configuration.setNonClusterFailover(enableFailoverCheckBox.isSelected());
        configuration.setClusterThreshold((Integer) clusterThresholdSpinner.getValue());
        configuration.setSecurityZone(zoneControl.getSelectedZone());
        configuration.setProperties(clusterSettingsMap);
    }

    private void initClusterSettingsTable() {
        clusterSettingTableModel= new ClusterSettingsTableModel();

        clusterSettingsTable.setModel(clusterSettingTableModel);
        clusterSettingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        clusterSettingsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableTableButtons();
            }
        });

        clusterSettingsTable.getTableHeader().setReorderingAllowed(false);
        Utilities.setDoubleClickAction(clusterSettingsTable, editButton);
        enableOrDisableTableButtons();
    }

    private class ClusterSettingsTableModel extends AbstractTableModel {

        @Override
        public int getColumnCount() {
            return MAX_TABLE_COLUMN_NUM;
        }

        @Override
        public void fireTableDataChanged() {
            super.fireTableDataChanged();
            enableOrDisableTableButtons();
        }

        @Override
        public int getRowCount() {
            return clusterSettingsMap.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            String name = (String)clusterSettingsMap.keySet().toArray()[row];
            switch(col) {
                case 0:
                    return name;
                case 1:
                    return clusterSettingsMap.get(name);
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return RESOURCES.getString("column.label.property.name");
                case 1:
                    return RESOURCES.getString("column.label.property.value");
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    private void enableOrDisableTableButtons() {
        int selectedRow = clusterSettingsTable.getSelectedRow();

        boolean addEnabled = true;
        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;

        addButton.setEnabled(flags.canCreateSome() && addEnabled);
        editButton.setEnabled(editEnabled);
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
    }

    public void selectName() {
        configurationNameTextField.requestFocus();
        configurationNameTextField.selectAll();
    }

    private void editAndSave(final MutablePair<String, String> property) {
        if (property == null || property.left == null || property.right == null) return;

        final String originalPropName = property.left;

        final ClusterSettingsDialog dlg = new ClusterSettingsDialog(this, property);
        dlg.pack();
        Utilities.centerOnScreen(dlg);

        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    String warningMessage = checkDuplicateProperty(property.left, originalPropName);
                    if (warningMessage != null) {
                        DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this, warningMessage,
                                RESOURCES.getString("dialog.title.duplicate.property"), JOptionPane.WARNING_MESSAGE, null);
                        return;
                    }

                    // Save the property into the map
                    if (!originalPropName.isEmpty()) { // This is for doEdit
                        clusterSettingsMap.remove(originalPropName);
                    }
                    clusterSettingsMap.put(property.left, property.right);

                    // Refresh the table
                    clusterSettingTableModel.fireTableDataChanged();

                    // Refresh the selection highlight
                    ArrayList<String> keySet = new ArrayList<>();
                    keySet.addAll(clusterSettingsMap.keySet());
                    int currentRow = keySet.indexOf(property.left);
                    clusterSettingsTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
                }
            }
        });
    }

    private String checkDuplicateProperty(String newPropName, final String originalPropName) {
        // Check if there exists a duplicate with Basic Connection Configuration.
        if ("name".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.name"));
        } else if ("address".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.address"));
        } else if ("hostname".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.hostname"));
        } else if ("secret".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.secret"));
        } else if ("ipcheck".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.checkIP"));
        } else if ("fipsmode".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.fipsMode"));
        } else if ("noncluster_failover".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.enableFailover"));
        } else if ("cluster_threshold".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.clusterThreshold"));
        }

        // Check if there exists a duplicate with other properties.
        for (String key: clusterSettingsMap.keySet()) {
            if (originalPropName.compareToIgnoreCase(key) != 0 // make sure not to compare itself
                    && newPropName.compareToIgnoreCase(key) == 0) {
                return MessageFormat.format(RESOURCES.getString("warning.message.duplicated.property"), newPropName);
            }
        }

        return null;
    }

    private boolean validateSiteMinderConfigurationNameUnique() {
        boolean valid = false;

        String newName = configurationNameTextField.getText();

        SiteMinderAdmin admin = getSiteMinderAdmin();

        if (admin != null) {
            List<SiteMinderConfiguration> configurations;

            try {
                configurations = admin.getAllSiteMinderConfigurations();

                boolean foundDuplicate = false;

                for (SiteMinderConfiguration config : configurations) {
                    if (config.getName().equals(newName) && !config.getGoid().equals(configuration.getGoid())) {
                        foundDuplicate = true;
                        break;
                    }
                }

                if (foundDuplicate) {
                    DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this,
                            "The configuration name \"" + newName + "\" is already in use. Please use a different name.",
                            RESOURCES.getString("dialog.title.error.saving.config"),
                            JOptionPane.WARNING_MESSAGE, null);
                } else {
                    valid = true;
                }
            } catch (FindException e) {
                String errorMessage = "Error checking for duplicate SiteMinder Configuration names: " + e.getMessage();

                logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));

                DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this,
                        errorMessage,
                        RESOURCES.getString("dialog.title.error.saving.config"),
                        JOptionPane.ERROR_MESSAGE, null);
            }
        } else {
            DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this,
                    "Error checking for duplicate SiteMinder Configuration names: Disconnected from gateway.",
                    RESOURCES.getString("dialog.title.error.saving.config"),
                    JOptionPane.ERROR_MESSAGE, null);
        }

        return valid;
    }

    private boolean validateSiteMinderConfiguration() {
        boolean valid = false;

        SiteMinderAdmin admin = getSiteMinderAdmin();

        if (admin != null) {
            viewToModel();

            try {
                Either<String, String> either = doAsyncAdmin(admin,
                        SiteMinderConfigPropertiesDialog.this,
                        RESOURCES.getString("message.validation.progress"),
                        RESOURCES.getString("message.validation"),
                        admin.testSiteMinderConfiguration(configuration));

                if (!either.isRight() || either.right().length() == 0) {
                    valid = true;
                } else {
                    DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this,
                            MessageFormat.format(RESOURCES.getString("message.validation.siteminder.config.failed"),
                                    either.right()),
                            RESOURCES.getString("dialog.title.siteminder.configuration.validation"),
                            JOptionPane.WARNING_MESSAGE, null);
                }
            } catch (InvocationTargetException e) {
                DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this,
                        MessageFormat.format(RESOURCES.getString("message.validation.siteminder.config.failed"),
                                e.getMessage()),
                        RESOURCES.getString("dialog.title.siteminder.configuration.validation"),
                        JOptionPane.WARNING_MESSAGE, null);
            } catch (InterruptedException e) {
                // do nothing, user cancelled
            }
        } else {
            DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this,
                    "Error validating SiteMinder Configuration: Disconnected from gateway.",
                    RESOURCES.getString("dialog.title.error.saving.config"),
                    JOptionPane.ERROR_MESSAGE, null);
        }

        return valid;
    }

    private SiteMinderAdmin getSiteMinderAdmin() {
        Registry reg = Registry.getDefault();

        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get SiteMinder Configuration Admin due to no Admin Context present.");
            return null;
        }

        return reg.getSiteMinderConfigurationAdmin();
    }

    public SiteMinderConfiguration getConfiguration() {
        return configuration;
    }
}