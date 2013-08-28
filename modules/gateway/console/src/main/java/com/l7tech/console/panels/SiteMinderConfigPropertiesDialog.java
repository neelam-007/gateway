package com.l7tech.console.panels;

import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.siteminder.SiteMinderFipsMode;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
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

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 * @author nilic
 * Date: 7/22/13
 * Time: 4:36 PM
 */
public class SiteMinderConfigPropertiesDialog extends JDialog {
    private static final ResourceBundle RESOURCES =
            Resources.getBundle("com.l7tech.console.panels.resources.SiteMinderConfigPropertiesDialog");

    private static final int MAX_TABLE_COLUMN_NUM = 2;
    private static final int CLUSTER_THRESHOLD_MIN = 1;
    private static final int CLUSTER_THRESHOLD_MAX = 1000;
    private static final int CLUSTER_SERVER_CONN_MIN = 1;
    private static final int CLUSTER_SERVER_CONN_MAX = 3;
    private static final int CLUSTER_SERVER_CONN_STEP = 1;

    private final Map<String, String> clusterSettingsMap = new TreeMap<>();
    private final Map<String, SiteMinderHost> siteMinderHostMap = new TreeMap<>();

    private boolean confirmed;

    private PermissionFlags flags;
    private SiteMinderConfiguration configuration;
    private SquigglyTextField configurationNameTextField;
    private JTextField agentNameTextField;
    private JTextField addressTextField;
    private JCheckBox IPCheckCheckBox;
    private JTextField hostNameTextField;
    private JComboBox<SiteMinderFipsMode> fipsModeComboBox;
    private JCheckBox nonclusterFailoverCheckBox;
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
        fipsModeComboBox.setModel(new DefaultComboBoxModel<>(SiteMinderFipsMode.values()));
        clusterThresholdSpinner.setModel(new SpinnerNumberModel(50, CLUSTER_THRESHOLD_MIN, CLUSTER_THRESHOLD_MAX, 1));

        initClusterSettingsTable();

        InputValidator.ValidationRule clusterThresholdRule =
                new InputValidator.NumberSpinnerValidationRule(clusterThresholdSpinner,
                        RESOURCES.getString("property.clusterThreshold"));

        InputValidator.ValidationRule ipCheckBoxRule =
                new InputValidator.ComponentValidationRule(IPCheckCheckBox) {
                    @Override
                    public String getValidationError() {
                        if (IPCheckCheckBox.isSelected()) {
                            final String address = addressTextField.getText();

                            if (address == null || address.trim().isEmpty()) {
                                return "The " + RESOURCES.getString("property.agent.address") +
                                        " field must not be empty if IP Check is enabled.";
                            }
                        }

                        return null;
                    }
                };

        InputValidator testValidator =
                new InputValidator(this, RESOURCES.getString("dialog.title.siteminder.configuration.properties"));

        InputValidator okValidator =
                new InputValidator(this, RESOURCES.getString("dialog.title.siteminder.configuration.properties"));

        okValidator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.agent.name"), agentNameTextField, null);
        okValidator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.configuration.name"), configurationNameTextField, null);
        okValidator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.agent.hostname"), hostNameTextField, null);
        okValidator.constrainPasswordFieldToBeNonEmpty(RESOURCES.getString("property.agent.secret"), secretPasswordField);
        okValidator.addRule(clusterThresholdRule);
        okValidator.addRule(ipCheckBoxRule);

        testValidator.constrainTextFieldToBeNonEmpty(RESOURCES.getString("property.agent.hostname"), hostNameTextField, null);
        testValidator.constrainPasswordFieldToBeNonEmpty(RESOURCES.getString("property.agent.secret"), secretPasswordField);
        testValidator.addRule(clusterThresholdRule);
        testValidator.addRule(ipCheckBoxRule);

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

        zoneControl.configure(configuration);

        modelToView();

        Utilities.setMinimumSize(this);
    }

    private void modelToView() {
        if (siteMinderHostMap != null && siteMinderHostMap.size() != 0 ) {
            SiteMinderHost siteMinderHost = siteMinderHostMap.get("SiteMinder Host Configuration");

            if (siteMinderHost != null) {
                configuration.setHostname(siteMinderHost.getHostname());
                configuration.setSecret(siteMinderHost.getSharedSecret());
                configuration.setAddress("127.0.0.1");
                configuration.setFipsmode(siteMinderHost.getFipsMode());
                configuration.setPasswordGoid(siteMinderHost.getPasswordGoid());
                configuration.setHostConfiguration(siteMinderHost.getHostConfigObject());
                configuration.setUserName(siteMinderHost.getUserName());

                Map<String, String> properties = new HashMap<>();

                String [] clusterProperties = siteMinderHost.getPolicyServer().split(",");

                if (clusterProperties.length == 4) {
                    properties.put(RESOURCES.getString("property.cluster.server.address"), clusterProperties[0]);
                    properties.put(RESOURCES.getString("property.cluster.server.accounting.port"), clusterProperties[1]);
                    properties.put(RESOURCES.getString("property.cluster.server.authentication.port"), clusterProperties[2]);
                    properties.put(RESOURCES.getString("property.cluster.server.authorization.port"), clusterProperties[3]);
                    properties.put(RESOURCES.getString("property.cluster.server.connection.min"), String.valueOf(CLUSTER_SERVER_CONN_MIN));
                    properties.put(RESOURCES.getString("property.cluster.server.connection.max"), String.valueOf(CLUSTER_SERVER_CONN_MAX));
                    properties.put(RESOURCES.getString("property.cluster.server.connection.step"), String.valueOf(CLUSTER_SERVER_CONN_STEP));
                    properties.put(RESOURCES.getString("property.cluster.server.timeout"), String.valueOf(siteMinderHost.getRequestTimeout()));

                    configuration.setProperties(properties);
                }
            }
        }

        configurationNameTextField.setText(configuration.getName());
        agentNameTextField.setText(configuration.getAgentName());
        secretPasswordField.setText(configuration.getSecret());
        addressTextField.setText(configuration.getAddress());
        IPCheckCheckBox.setSelected(configuration.isIpcheck());
        updateSSOTokenCheckBox.setSelected(configuration.isUpdateSSOToken());
        hostNameTextField.setText(configuration.getHostname());
        disableCheckBox.setSelected(!configuration.isEnabled());

        SiteMinderFipsMode mode = SiteMinderFipsMode.getByCode(configuration.getFipsmode());

        // any unrecognized fips mode setting will be replaced with UNSET
        fipsModeComboBox.setSelectedItem(mode == null ? SiteMinderFipsMode.UNSET : mode);

        nonclusterFailoverCheckBox.setSelected(configuration.isNonClusterFailover());
        clusterSettingsMap.clear();

        if (configuration.getProperties() != null) {
            clusterSettingsMap.putAll(configuration.getProperties());
        }

        clusterSettingTableModel.fireTableDataChanged();
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
        String warningMessage =  checkDuplicateSiteMinderConfiguration();

        if (warningMessage != null) {
            DialogDisplayer.showMessageDialog( SiteMinderConfigPropertiesDialog.this, warningMessage,
                    RESOURCES.getString( "dialog.title.error.saving.config" ), JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        // Assign new attributes to the connect
        viewToModel();

        confirmed = true;
        dispose();
    }

    private void doTest() {
        String warningMessage = validateSiteMinderConfiguration();
        if (warningMessage != null && warningMessage.length() != 0) {
            DialogDisplayer.showMessageDialog( SiteMinderConfigPropertiesDialog.this, warningMessage,
                    RESOURCES.getString( "dialog.title.siteminder.configuration.validation" ), JOptionPane.WARNING_MESSAGE, null);
        } else {
            DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this, RESOURCES.getString("message.validation.siteminder.config.passed"),
                    RESOURCES.getString("dialog.title.siteminder.configuration.validation"), JOptionPane.WARNING_MESSAGE, null);
        }
    }

    private void viewToModel() {
        configuration.setName(configurationNameTextField.getText().trim());
        configuration.setAgentName(agentNameTextField.getText().trim());
        configuration.setSecret(new String(secretPasswordField.getPassword()));
        configuration.setAddress(addressTextField.getText().trim());
        configuration.setIpcheck(IPCheckCheckBox.isSelected());
        configuration.setUpdateSSOToken(updateSSOTokenCheckBox.isSelected());
        configuration.setHostname(hostNameTextField.getText().trim());
        configuration.setEnabled(!disableCheckBox.isSelected());

        int modeIndex = fipsModeComboBox.getSelectedIndex();
        SiteMinderFipsMode mode = modeIndex > -1 ? fipsModeComboBox.getItemAt(modeIndex) : SiteMinderFipsMode.UNSET;

        configuration.setFipsmode(mode.getCode());
        configuration.setNonClusterFailover(nonclusterFailoverCheckBox.isSelected());
        configuration.setClusterThreshold((Integer) clusterThresholdSpinner.getValue());
        configuration.setProperties(clusterSettingsMap);
        configuration.setSecurityZone(zoneControl.getSelectedZone());
    }

    private void doCancel() {
        dispose();
    }

    private void doRegister() {
        register(new MutablePair<>("Init SiteMinder Host", new SiteMinderHost(configuration.getHostname(),
                clusterSettingsMap.get(RESOURCES.getString("property.cluster.server.address")),
                configuration.getHostConfiguration(),
                configuration.getFipsmode(),
                configuration.getUserName(),
                configuration.getPasswordGoid())));
    }

    private void register(final MutablePair<String, SiteMinderHost> property) {
        final SiteMinderRegisterConfigDialog dlg = new SiteMinderRegisterConfigDialog(this, property);
        dlg.pack();
        Utilities.centerOnScreen(dlg);

        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    siteMinderHostMap.put(property.left, property.right);
                    modelToView();
                }
            }
        });
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
                    ArrayList<String> keyset = new ArrayList<>();
                    keyset.addAll(clusterSettingsMap.keySet());
                    int currentRow = keyset.indexOf(property.left);
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
                    RESOURCES.getString("property.agent.ipcheck"));
        } else if ("fipsmode".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.fipsmode"));
        } else if ("noncluster_failover".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.noncluster_failover"));
        } else if ("cluster_threshold".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(RESOURCES.getString("warning.basic.config.prop.configured"),
                    RESOURCES.getString("property.agent.cluster_threshold"));
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

    private String checkDuplicateSiteMinderConfiguration() {
        SiteMinderAdmin admin = getSiteMinderAdmin();

        if (admin == null) return "Can't get SiteMinder admin. Check log and try again";
        String originalName = configuration.getName();
        String newName = configurationNameTextField.getText();

        if (originalName.compareToIgnoreCase(newName) == 0) return null;
        try{
            for (String name: admin.getAllSiteMinderConfigurationNames()) {
                if (name.equals(configurationNameTextField.getText())) {
                    return "The connection name \"" + name + "\" already exists. Try a new name.";
                }
            }
        } catch (FindException ex) {
            return "Can't find SiteMinder Configuration. Check log and try again.";
        }

        return null;
    }

    private String validateSiteMinderConfiguration() {
        SiteMinderAdmin admin = getSiteMinderAdmin();

        if (admin == null) return "Can't get SiteMinder admin. Check log and try again";

        viewToModel();
        String msg = null;

        try {
            msg = doAsyncAdmin(admin,
                    SiteMinderConfigPropertiesDialog.this,
                    RESOURCES.getString("message.validation.progress"),
                    RESOURCES.getString("message.validation"),
                    admin.testSiteMinderConfiguration(configuration)).right();
        } catch (InterruptedException e) {
            // do nothing, user cancelled
        } catch (InvocationTargetException e) {
            DialogDisplayer.showMessageDialog(this,
                    MessageFormat.format(RESOURCES.getString("message.validation.siteminder.config.failed"), e.getMessage()),
                    RESOURCES.getString("dialog.title.siteminder.configuration.validation"),
                    JOptionPane.WARNING_MESSAGE, null);
        }

        return msg;
    }

    private SiteMinderAdmin getSiteMinderAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getSiteMinderConfigurationAdmin();
    }

    public SiteMinderConfiguration getConfiguration() {
        return configuration;
    }
}