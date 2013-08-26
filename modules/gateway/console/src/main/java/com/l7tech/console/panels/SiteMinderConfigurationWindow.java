package com.l7tech.console.panels;

import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * GUI for managing SiteMinder entities (Add, Edit, or Remove)
 * User: nilic
 * Date: 7/22/13
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SiteMinderConfigurationWindow extends JDialog {

    private static final int MAX_TABLE_COLUMN_NUM = 5;
    private static final Logger logger = Logger.getLogger(SiteMinderConfigurationWindow.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.SiteMinderConfigurationManagerWindow");


    private JPanel mainPanel;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;
    private JButton copyButton;
    private JTable configurationTable;

    private List<SiteMinderConfiguration> configurationList = new ArrayList<SiteMinderConfiguration>();
    private AbstractTableModel  configurationTableModel;
    private PermissionFlags flags;

    public SiteMinderConfigurationWindow(Frame owner){
        super(owner, resources.getString("dialog.title.manage.siteminder.configuration"));
        initialize();
    }

    public SiteMinderConfigurationWindow(Dialog owner) {
        super(owner, resources.getString("dialog.title.manage.siteminder.configuration"));
        initialize();
    }

    private void initialize(){
        flags = PermissionFlags.get(EntityType.SITEMINDER_CONFIGURATION);

        // Initialize GUI components
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);
        Utilities.setEscKeyStrokeDisposes(this);

        initSiteMinderConfigurationTable();

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

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCopy();
            }
        });

        Utilities.setDoubleClickAction(configurationTable, editButton);
        enableOrDisableButtons();
    }

    private void initSiteMinderConfigurationTable(){
        //refresh configuration list
        loadSiteMinderConfigurationList();

        //Initialise the table model
        configurationTableModel = new SiteMinderConfigurationTableModel();

        configurationTable.setModel(configurationTableModel);
        configurationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configurationTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });
    }


    private void loadSiteMinderConfigurationList(){
        SiteMinderAdmin admin = getSiteMinderAdmin();
        if (admin != null){
            try {
                configurationList = admin.getAllSiteMinderConfigurations();
            } catch (FindException ex){
                logger.warning("Cannot find SiteMinder Configurations.");
            }
            Collections.sort(configurationList);
        }
    }

    private SiteMinderAdmin getSiteMinderAdmin(){
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getSiteMinderConfigurationAdmin();
    }

    private void doAdd(){
        SiteMinderConfiguration configuration = new SiteMinderConfiguration();
        editAndSave(configuration, true);
    }

    private void doEdit(){
        int selectedRow = configurationTable.getSelectedRow();

        if (selectedRow < 0) return;
        editAndSave(configurationList.get(selectedRow), false);
    }

    private void doRemove(){

        int selectedRow = configurationTable.getSelectedRow();
        if (selectedRow < 0) return;

        SiteMinderConfiguration configuration = configurationList.get(selectedRow);
        Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};

        int result = JOptionPane.showOptionDialog(
                this, MessageFormat.format(resources.getString("confirmation.remove.configuration"), configuration.getName()),
                resources.getString("dialog.title.remove.configuration"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0){
            configurationList.remove(selectedRow);

            SiteMinderAdmin admin = getSiteMinderAdmin();
            if (admin == null) return ;
            try{
                admin.deleteSiteMinderConfiguration(configuration);
            } catch (DeleteException ex){
                logger.warning("Cannot delete the SiteMinder configuration " + configuration.getName());
                return;
            }
        }

        Collections.sort(configurationList);
        configurationTableModel.fireTableDataChanged();

        if (selectedRow == configurationList.size()) selectedRow --;
        if (selectedRow >= 0) configurationTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
    }

    private void doCopy(){

        int selectedRow = configurationTable.getSelectedRow();

        SiteMinderConfiguration newConfiguration = new SiteMinderConfiguration();
        newConfiguration.copyFrom(configurationList.get(selectedRow));
        EntityUtils.updateCopy( newConfiguration);
        editAndSave(newConfiguration, true);
    }

    private void enableOrDisableButtons(){

        int selectedRow = configurationTable.getSelectedRow();

        boolean addEnabled = true;
        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;
        boolean copyEnabled = selectedRow >= 0;

        addButton.setEnabled(flags.canCreateSome() && addEnabled);
        editButton.setEnabled(editEnabled);  // Not using flags.canUpdateSome(), since we still allow users to view the properties.
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
        copyButton.setEnabled(flags.canCreateSome() && copyEnabled);

    }

    private class SiteMinderConfigurationTableModel extends AbstractTableModel {

        @Override
        public void fireTableDataChanged() {
            super.fireTableDataChanged();
            enableOrDisableButtons();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public int getRowCount(){
            return configurationList.size();
        }

        @Override
        public int getColumnCount(){
            return MAX_TABLE_COLUMN_NUM;
        }

        @Override
        public Object getValueAt(int row, int col){
            SiteMinderConfiguration configuration = configurationList.get(row);

            switch (col) {
                case 0:
                    return configuration.isEnabled()?"Yes":"No";
                case 1:
                    return configuration.getName();
                case 2:
                    return configuration.getAgentName();
                case 3:
                    return configuration.getAddress();
                case 4:
                    return configuration.getHostname();
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return resources.getString("column.label.enabled");              // Column: Enabled
                case 1:
                    return resources.getString("column.label.configuration.name");   // Column: Configuration Name
                case 2:
                    return resources.getString("column.label.agent.name");           // Column: Agent Name
                case 3:
                    return resources.getString("column.label.agent.address");        // Column: Agent Address
                case 4:
                    return resources.getString("column.label.agent.hostname");       // Column: Agent Hostname
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }
    }

    private void editAndSave(final SiteMinderConfiguration configuration, final boolean selectName){
        final SiteMinderConfigPropertiesDialog configPropDialog = new SiteMinderConfigPropertiesDialog(SiteMinderConfigurationWindow.this, configuration);

        configPropDialog.pack();
        Utilities.centerOnScreen(configPropDialog);
        if(selectName)
            configPropDialog.selectName();
        DialogDisplayer.display(configPropDialog, new Runnable() {
            @Override
            public void run() {
                if (configPropDialog.isConfirmed()){
                    Runnable reedit = new Runnable() {
                        @Override
                        public void run() {
                            loadSiteMinderConfigurationList();
                            editAndSave(configuration, selectName);
                        }
                    };

                    //Save the connection
                    SiteMinderAdmin admin = getSiteMinderAdmin();
                    if (admin == null) return;
                    try{
                        admin.saveSiteMinderConfiguration(configuration);
                    } catch (UpdateException ex){
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(ex),
                                ex,
                                reedit);
                        return;
                    }
                }

                loadSiteMinderConfigurationList();

                configurationTableModel.fireTableDataChanged();

                int currentRow = -1;
                if (configuration.getName() != null && configuration.getName().length() > 0) {
                    for (SiteMinderConfiguration config: configurationList){
                        currentRow ++;
                        if (config.getName().equals(configuration.getName())){
                            break;
                        }
                    }

                    configurationTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
                }
            }
        }) ;
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

}
