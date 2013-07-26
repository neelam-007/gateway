package com.l7tech.console.panels;

import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 *  GUI for creating or editing properties of a SiteMinder configuration entity.
 * User: nilic
 * Date: 7/22/13
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class SiteMinderConfigPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SiteMinderConfigPropertiesDialog.class.getName());
    private static final ResourceBundle resources = Resources.getBundle("com.l7tech.console.panels.resources.SiteMinderConfigPropertiesDialog");
    private static final int MAX_TABLE_COLUMN_NUM = 2;
    private static final int CLUSTER_THRESHOLD_MIN = 1;
    private static final int CLUSTER_THRESHOLD_MAX = 1000;
    private static final int CLUSTER_SERVER_CONN_MIN = 1;
    private static final int CLUSTER_SERVER_CONN_MAX = 3;
    private static final int CLUSTER_SERVER_CONN_STEP = 1;

    public static final String UNSET_FIPS_MODE = "UNSET";
    public static final String COMPAT_FIPS_MODE = "COMPAT";
    public static final String MIGRATE_FIPS_MODE = "MIGRATE";
    public static final String ONLY_FIPS_MODE = "ONLY";

    public static final int FIPS140_UNSET = 0;
    public static final int FIPS140_COMPAT = 1;
    public static final int FIPS140_MIGRATE = 2;
    public static final int FIPS140_ONLY = 3;

    private SiteMinderConfiguration configuration;
    private final Map<String, SiteMinderHost> siteMinderHostMap = new TreeMap<String, SiteMinderHost>();
    private PermissionFlags flags;

    private SquigglyTextField configurationNameTextField;
    private JTextField agentNameTextField;
    private JTextArea secretTextArea;
    private JTextField addressTextField;
    private JCheckBox IPCheckCheckBox;
    private JTextField hostNameTextField;
    private JComboBox fipsModeComboBox;
    private JCheckBox nonclusterFailoverCheckBox;
    private JTable clusterSettingsTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JSpinner clusterTresholdSpinner;
    private AbstractTableModel clusterSettingTableModel;
    private final Map<String, String> clusterSettingsMap = new TreeMap<String, String>();
    private SecurityZoneWidget zoneControl;
    private JButton registerButton;

    private static class FipsModeType{
        private static final Map<Integer, FipsModeType> fipsTypes = new ConcurrentHashMap<Integer, FipsModeType>();
        final int code;
        final String label;
        public FipsModeType(int code, String label){
            this.code = code;
            this.label = label;
            fipsTypes.put(code, this);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final Object UNSET_MODE =  new FipsModeType(FIPS140_UNSET, UNSET_FIPS_MODE);
    public static final Object COMPAT_MODE = new FipsModeType(FIPS140_COMPAT, COMPAT_FIPS_MODE);
    public static final Object MIGRATE_MODE = new FipsModeType(FIPS140_MIGRATE, MIGRATE_FIPS_MODE);
    public static final Object ONLY_MODE =  new FipsModeType(FIPS140_ONLY, ONLY_FIPS_MODE);

    private boolean confirmed;

    public SiteMinderConfigPropertiesDialog(Frame owner, SiteMinderConfiguration configuration){
        super(owner, resources.getString("dialog.title.siteminder.configuration.properties"));
        initialize(configuration);
    }

    public SiteMinderConfigPropertiesDialog(Dialog owner, SiteMinderConfiguration configuration) {
        super(owner, resources.getString("dialog.title.siteminder.configuration.properties"));
        initialize(configuration);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private  void initialize(SiteMinderConfiguration configuration){
        flags = PermissionFlags.get(EntityType.SITEMINDER_CONFIGURATION);

        this.configuration = configuration;

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);

        configurationNameTextField.setDocument(new MaxLengthDocument(128));
        agentNameTextField.setDocument(new MaxLengthDocument(256));
        secretTextArea.setDocument(new MaxLengthDocument(4096));
        addressTextField.setDocument(new MaxLengthDocument(128));
        hostNameTextField.setDocument(new MaxLengthDocument(255));
        ((JTextField)fipsModeComboBox.getEditor().getEditorComponent()).setDocument(new MaxLengthDocument(255));

        final RunOnChangeListener docListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableButtons();
            }
        });

        ((JTextField)fipsModeComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(docListener);

        fipsModeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableButtons();
            }
        });
        configurationNameTextField.getDocument().addDocumentListener(docListener);
        agentNameTextField.getDocument().addDocumentListener(docListener);
        secretTextArea.getDocument().addDocumentListener(docListener);
        addressTextField.getDocument().addDocumentListener(docListener);
        hostNameTextField.getDocument().addDocumentListener(docListener);

        fipsModeComboBox.setModel(new DefaultComboBoxModel(new Object[] {
                UNSET_MODE,
                COMPAT_MODE,
                MIGRATE_MODE,
                ONLY_MODE
        }));

        InputValidator validator = new InputValidator(this, resources.getString("dialog.title.siteminder.configuration.properties"));
        clusterTresholdSpinner.setModel(new SpinnerNumberModel(50, CLUSTER_THRESHOLD_MIN, CLUSTER_THRESHOLD_MAX, 1));
        validator.addRule(new InputValidator.NumberSpinnerValidationRule(clusterTresholdSpinner, resources.getString("property.clusterThreshold")));

        validator.constrainTextFieldToBeNonEmpty(resources.getString("label.configuration.name"), configurationNameTextField, null);
        validator.constrainTextFieldToBeNonEmpty(resources.getString("label.agent.name"), agentNameTextField, null);
        validator.constrainTextFieldToBeNonEmpty(resources.getString("label.agent.address"), addressTextField, null);
        validator.constrainTextFieldToBeNonEmpty(resources.getString("label.agent.hostname"), hostNameTextField, null);
        validator.constrainTextFieldToBeNonEmpty(resources.getString("label.agent.secret"), secretTextArea, null);

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

        initClusterSettingsTable();

        validator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOK();
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

        zoneControl.configure(SiteMinderConfiguration.DEFAULT_GOID.equals(configuration.getGoid()) ? OperationType.CREATE : OperationType.UPDATE, configuration);

        modelToView();
        enableOrDisableButtons();
        Utilities.setMinimumSize( this );
    }

    private void modelToView(){
        if (siteMinderHostMap != null && siteMinderHostMap.size() != 0 ){
            SiteMinderHost siteMinderHost = siteMinderHostMap.get("SiteMinder Host Configuration");

            configuration.setHostname(siteMinderHost.getHostname());
            configuration.setSecret(siteMinderHost.getSharedSecret());
            configuration.setAddress("127.0.0.1");
            configuration.setFipsmode(siteMinderHost.getFipsMode());

            Map<String, String> properties = new HashMap<String, String>();

            String [] clusterProperties = siteMinderHost.getPolicyServer().split(",");

            if (clusterProperties.length == 4){

                properties.put(resources.getString("property.cluster.server.address"), clusterProperties[0]);
                properties.put(resources.getString("property.cluster.server.accounting.port"), clusterProperties[1]);
                properties.put(resources.getString("property.cluster.server.authentication.port"), clusterProperties[2]);
                properties.put(resources.getString("property.cluster.server.authorization.port"), clusterProperties[3]);
                properties.put(resources.getString("property.cluster.server.connection.min"), String.valueOf(CLUSTER_SERVER_CONN_MIN));
                properties.put(resources.getString("property.cluster.server.connection.max"), String.valueOf(CLUSTER_SERVER_CONN_MAX));
                properties.put(resources.getString("property.cluster.server.connection.step"), String.valueOf(CLUSTER_SERVER_CONN_STEP));
                properties.put(resources.getString("property.cluster.server.timeout"), String.valueOf(siteMinderHost.getRequestTimeout()));

                configuration.setProperties(properties);
            }
        }

        configurationNameTextField.setText(configuration.getName());
        agentNameTextField.setText(configuration.getAgent_name());
        secretTextArea.setText(configuration.getSecret());
        addressTextField.setText(configuration.getAddress());
        IPCheckCheckBox.setSelected(configuration.isIpcheck());
        hostNameTextField.setText(configuration.getHostname());
        switch (configuration.getFipsmode()){
            case 0:
                fipsModeComboBox.setSelectedIndex(FIPS140_UNSET);
                break;
            case 1:
                fipsModeComboBox.setSelectedIndex(FIPS140_COMPAT);
                break;
            case 2:
                fipsModeComboBox.setSelectedIndex(FIPS140_MIGRATE);
                break;
            case 3:
                fipsModeComboBox.setSelectedIndex(FIPS140_ONLY);
                break;
            default:
                fipsModeComboBox.setSelectedIndex(FIPS140_UNSET);
                break;
        }
        nonclusterFailoverCheckBox.setSelected(configuration.isNoncluster_failover());
        clusterSettingsMap.clear();
        if (configuration.getProperties() != null) {
            clusterSettingsMap.putAll(configuration.getProperties());
        }
        clusterSettingTableModel.fireTableDataChanged();
    }

    private void enableOrDisableButtons(){

        boolean fipsModeOK = fipsModeComboBox.getSelectedIndex() <= 0  ?  false : true;

        //get the properties from clusterSettingsMap
        boolean enabled = clusterSettingsMap.containsKey(resources.getString("property.cluster.server.address")) &&
                          clusterSettingsMap.containsKey(resources.getString("property.cluster.server.authentication.port")) &&
                          clusterSettingsMap.containsKey(resources.getString("property.cluster.server.authorization.port")) &&
                          clusterSettingsMap.containsKey(resources.getString("property.cluster.server.accounting.port")) &&
                          clusterSettingsMap.containsKey(resources.getString("property.cluster.server.connection.min")) &&
                          clusterSettingsMap.containsKey(resources.getString("property.cluster.server.connection.max")) &&
                          clusterSettingsMap.containsKey(resources.getString("property.cluster.server.connection.step")) &&
                          clusterSettingsMap.containsKey(resources.getString("property.cluster.server.timeout")) &&
                          fipsModeOK;

        okButton.setEnabled(enabled);
    }

    private void doAdd(){
        editAndSave(new MutablePair<String, String>("", ""));
    }

    private void doEdit(){

        int selectedRow = clusterSettingsTable.getSelectedRow();

        if (selectedRow < 0) return;

        String propName = (String) clusterSettingsMap.keySet().toArray()[selectedRow];
        String propValue = (String)clusterSettingsMap.get(propName);

        editAndSave(new MutablePair<String, String>(propName, propValue));
    }

    private void doRemove(){
        int currentRow = clusterSettingsTable.getSelectedRow();
        if (currentRow < 0) return;

        String propName = (String) clusterSettingsMap.keySet().toArray()[currentRow];
        Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
        int result = JOptionPane.showOptionDialog(
                this, MessageFormat.format(resources.getString("confirmation.remove.cluster.settings.property"), propName),
                resources.getString("dialog.title.remove.cluster.settings.property"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0) {
            // Refresh the list
            clusterSettingsMap.remove(propName);

            enableOrDisableButtons();
            // Refresh the table
            clusterSettingTableModel.fireTableDataChanged();
            // Refresh the selection highlight
            if (currentRow == clusterSettingsMap.size()) currentRow--; // If the previous deleted row was the last row
            if (currentRow >= 0) clusterSettingsTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
        }
    }

    private void doOK(){
        String warningMessage =  checkDuplicateSiteMinderConfiguration();

        if (warningMessage != null) {
            DialogDisplayer.showMessageDialog( SiteMinderConfigPropertiesDialog.this, warningMessage,
                    resources.getString( "dialog.title.error.saving.config" ), JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        // Assign new attributes to the connect
        viewToModel();

        confirmed = true;
        dispose();
    }

    private void viewToModel(){

        configuration.setName(configurationNameTextField.getText().trim());
        configuration.setAgent_name(agentNameTextField.getText().trim());
        configuration.setSecret(secretTextArea.getText().trim());
        configuration.setAddress(addressTextField.getText().trim());
        configuration.setIpcheck(IPCheckCheckBox.isSelected());
        configuration.setHostname(hostNameTextField.getText().trim());
        switch (fipsModeComboBox.getSelectedItem().toString()) {
            case UNSET_FIPS_MODE:
                DialogDisplayer.showMessageDialog(SiteMinderConfigPropertiesDialog.this,
                        MessageFormat.format(resources.getString("warning.basic.config.fips.configured"), resources.getString("property.agent.fipsmode")),
                        resources.getString("dialog.title.wrong.property.fipsmode"), JOptionPane.WARNING_MESSAGE, null);
                return;
            case COMPAT_FIPS_MODE:
                configuration.setFipsmode(FIPS140_COMPAT);
                break;
            case MIGRATE_FIPS_MODE:
                configuration.setFipsmode(FIPS140_MIGRATE);
                break;
            case ONLY_FIPS_MODE:
                configuration.setFipsmode(FIPS140_ONLY);
                break;
            default:
                MessageFormat.format(resources.getString("warning.basic.config.fips.configured"), resources.getString("property.agent.fipsmode"));
                break;
        }
        configuration.setNoncluster_failover(nonclusterFailoverCheckBox.isSelected());
        configuration.setCluster_threshold((Integer)clusterTresholdSpinner.getValue());
        configuration.setProperties(clusterSettingsMap);
        configuration.setSecurityZone(zoneControl.getSelectedZone());
    }

    private void doCancel(){
        dispose();
    }

    private void doRegister(){
        register(new MutablePair<String, SiteMinderHost>("", new SiteMinderHost()));
    }

    private void register(final MutablePair<String, SiteMinderHost> property) {

        final SiteMinderRegisterConfigDialog dlg = new SiteMinderRegisterConfigDialog(this, property);
        dlg.pack();
        Utilities.centerOnScreen(dlg);

        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()){
                    siteMinderHostMap.put(property.left, property.right);
                    modelToView();
                }
            };
        });
    }

    private void initClusterSettingsTable(){

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
        public int getRowCount(){
            return clusterSettingsMap.size();
        }

        @Override
        public Object getValueAt(int row, int col){
            String name = (String)clusterSettingsMap.keySet().toArray()[row];
            switch(col){
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
                    return resources.getString("column.label.property.name");
                case 1:
                    return resources.getString("column.label.property.value");
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    private void enableOrDisableTableButtons(){

        int selectedRow = clusterSettingsTable.getSelectedRow();

        boolean addEnabled = true;
        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;

        addButton.setEnabled(flags.canCreateSome() && addEnabled);
        editButton.setEnabled(editEnabled);
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
    }

    public void selectName(){
        configurationNameTextField.requestFocus();
        configurationNameTextField.selectAll();
    }

    private void editAndSave(final MutablePair<String, String> property){
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
                                resources.getString("dialog.title.duplicate.property"), JOptionPane.WARNING_MESSAGE, null);
                        return;
                    }

                    // Save the property into the map
                    if (!originalPropName.isEmpty()) { // This is for doEdit
                        clusterSettingsMap.remove(originalPropName);
                    }
                    clusterSettingsMap.put(property.left, property.right);

                    enableOrDisableButtons();

                    // Refresh the table
                    clusterSettingTableModel.fireTableDataChanged();

                    // Refresh the selection highlight
                    ArrayList<String> keyset = new ArrayList<String>();
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
            return MessageFormat.format(resources.getString("warning.basic.config.prop.configured"), resources.getString("property.agent.name"));
        } else if ("address".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.config.prop.configured"), resources.getString("property.agent.address"));
        } else if ("hostname".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.config.prop.configured"), resources.getString("property.agent.hostname"));
        } else if ("secret".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.config.prop.configured"), resources.getString("property.agent.secret"));
        }else if ("ipcheck".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.config.prop.configured"), resources.getString("property.agent.ipcheck"));
        }else if ("fipsmode".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.config.prop.configured"), resources.getString("property.agent.fipsmode"));
        }else if ("noncluster_failover".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.config.prop.configured"), resources.getString("property.agent.noncluster_failover"));
        }else if ("cluster_threshold".compareToIgnoreCase(newPropName) == 0) {
            return MessageFormat.format(resources.getString("warning.basic.config.prop.configured"), resources.getString("property.agent.cluster_threshold"));
        }

        // Check if there exists a duplicate with other properties.
        for (String key: clusterSettingsMap.keySet()) {
            if (originalPropName.compareToIgnoreCase(key) != 0 // make sure not to compare itself
                    && newPropName.compareToIgnoreCase(key) == 0) {
                return MessageFormat.format(resources.getString("warning.message.duplicated.property"), newPropName);
            }
        }

        return null;
    }

    private String checkDuplicateSiteMinderConfiguration(){

        SiteMinderAdmin admin = getSiteMinderAdmin();

        if (admin == null) return "Can't get SiteMinder admin. Check log and try again";
        String originalName = configuration.getName();
        String newName = configurationNameTextField.getText();

        if (originalName.compareToIgnoreCase(newName) == 0) return null;
        try{
            for (String name: admin.getAllSiteMinderConfigurationNames()){
                if (name.equals(configurationNameTextField.getText())){
                    return "The connection name \"" + name + "\" already exists. Try a new name.";
                }
            }
        } catch (FindException ex){
            return "Can't find SiteMinder Configuration. Check log and try again.";
        }

        return null;
    }

    private SiteMinderAdmin getSiteMinderAdmin(){
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getSiteMinderConfigurationAdmin();
    }
}