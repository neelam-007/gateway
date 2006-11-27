package com.l7tech.server.config.ui.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PartitionActions;
import com.l7tech.server.config.beans.PartitionConfigBean;
import com.l7tech.server.config.commands.PartitionConfigCommand;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 11:37:25 AM
 */
public class ConfigWizardPartitioningPanel extends ConfigWizardStepPanel{
    private static final Logger logger = Logger.getLogger(ConfigWizardPartitioningPanel.class.getName());
        
    private JPanel mainPanel;
    private JList partitionList;
    private JPanel partitionSelectionPanel;
    private JButton addPartition;
    private JButton removePartition;

    private JPanel propertiesPanel;
    private JTextField partitionName;
//    private JButton removeHttpEndpoint;
    private JTable endpointsTable;
    private JTable otherEndpointsTable;
    private JButton removeOtherEndpoint;
    private PartitionListModel partitionListModel;

    PartitionConfigBean partitionBean;

    private EndpointTableModel endpointTableModel;

    public ConfigWizardPartitioningPanel(WizardStepPanel next) {
        super(next);
        stepLabel = "Configure Partitions";
        setShowDescriptionPanel(false);
    }

    private void init() {
        osFunctions = getParentWizard().getOsFunctions();
        if (osFunctions == null) osFunctions = OSDetector.getOSSpecificFunctions(PartitionInformation.DEFAULT_PARTITION_NAME);

        configBean = new PartitionConfigBean();
        configCommand = new PartitionConfigCommand(configBean);

        partitionListModel = new PartitionListModel();
        partitionList.setModel(partitionListModel);

        endpointTableModel = new EndpointTableModel();
        endpointsTable.setModel(endpointTableModel);
        TableColumn col = endpointsTable.getColumnModel().getColumn(1);
        col.setCellEditor(new DefaultCellEditor(new JComboBox(getAvailableIpAddresses())));

        initListeners();

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.NORTH);

        setupPartitions();
        enableProperties(false);
    }

    private void setupPartitions() {
        Set<String> partNames = PartitionManager.getInstance().getPartitionNames();
        partitionListModel.clear();
        for (String partName : partNames) {
            PartitionInformation pi = PartitionManager.getInstance().getPartition(partName);
            partitionListModel.add(pi);
        }
    }

    private void doManagePartition(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof JButton) {
            JButton button = (JButton) source;
            if (button == addPartition) {
                doAddPartition();
            } else if (button == removePartition) {
                doRemovePartition();
            } else {
                //do nothing
            }
        }
    }

    private void doAddPartition() {
        String newName = "New Partition";
        PartitionInformation pi = new PartitionInformation(newName);
        partitionListModel.add(pi);
        partitionList.setSelectedValue(pi, true);
        partitionName.requestFocus();
        partitionName.setSelectionStart(0);
        partitionName.setSelectionEnd(newName.length());
    }

    private void doRemovePartition() {
        Utilities.doWithConfirmation(
                ConfigWizardPartitioningPanel.this,
                "Remove Partition", "Are you sure you want to remove the selected partition? This cannot be undone.", new Runnable() {
                public void run() {
                    Object o = partitionList.getSelectedValue();
                    PartitionInformation partition = (PartitionInformation) o;
                    partitionListModel.remove(partition);
                }
        }
        );
    }

    private void enableNextButton() {
        getParentWizard().getNextButton().setEnabled(canAdvance());
    }

    public boolean canAdvance() {
        return  hasPartitionSelected() &&
                StringUtils.isNotEmpty(partitionName.getText()) &&
                endpointsTable.getModel().getRowCount() > 0;
    }

    private boolean hasPartitionSelected() {
        return  partitionList.getSelectedValue() != null;
    }

    private void initListeners() {
        partitionName.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                int index = partitionList.getSelectedIndex();
                if (index > 0 && index < partitionListModel.getSize()) {
                    partitionListModel.update(index, partitionName.getText(), null, null);
                }
            }
        });

        partitionList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                updateProperties();
            }
        });

       endpointsTable.getModel().addTableModelListener(new TableModelListener() {
           public void tableChanged(TableModelEvent e) {
               enableButtons();
           }
       });

        addPartition.addActionListener(managePartitionActionListener);
        removePartition.addActionListener(managePartitionActionListener);
//        removeHttpEndpoint.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                doRemoveEndpoint();
//            }
//        });        
    }

    private void enableButtons() {
        enablePartitionButtons();
        enableEndpointButtons();
        enableNextButton();
    }

    private void enableEndpointButtons() {
        enableEditDeleteEndpointButtons();
    }

    private void enableEditDeleteEndpointButtons() {
        int size = endpointsTable.getModel().getRowCount();
        int index = partitionList.getSelectedIndex();

        boolean validRowSelected = size != 0 &&
                 index > 0 && index < size;
//        removeHttpEndpoint.setEnabled(validRowSelected);
//        editEndpoint.setEnabled(validRowSelected);
    }

    private void enablePartitionButtons() {
        enableEditDeletePartitionButtons();
    }

    private void updateProperties() {
        Object o = partitionList.getSelectedValue();
        enableProperties(o != null);

        if (o != null) {
            PartitionInformation pi = (PartitionInformation) o;
            partitionName.setText(pi.getPartitionId());
//            endpointTableModel.clear();
            endpointTableModel.addEndpoints(pi.getEndpoints());
        }
        enableButtons();
    }

    private void enableProperties(boolean enabled) {
        Utilities.setEnabled(propertiesPanel, enabled);
    }

    protected void updateModel() {
        PartitionInformation pi = getSelectedPartition();

        pi.setPartitionId(partitionName.getText());
        pi.setEndpointsList(endpointTableModel.getEndpoints());

        PartitionConfigBean partBean = (PartitionConfigBean) configBean;
        partBean.setPartition(pi);

        osFunctions = pi.getOSSpecificFunctions();
        getParentWizard().setPartitionName(pi);
    }

    protected void updateView() {
        if (osFunctions == null) init();

        enableButtons();
    }

    private void enableEditDeletePartitionButtons() {
        int size = partitionListModel.getSize();
        int index = partitionList.getSelectedIndex();
        
        //if nothing is selected yet, select the first element
        if (index < 0) {
            index = 0;
            partitionList.setSelectedIndex(index);
        }

        boolean validRowSelected = (size != 0) && (0 <= index) && (index < size);
        removePartition.setEnabled(validRowSelected);
    }

//    private void doRemoveEndpoint() {
//        Utilities.doWithConfirmation(
//            getParentWizard(),
//            "Remove Endpoint", "Are you sure you want to remove the selected endpoint?", new Runnable() {
//            public void run() {
//                EndpointTableModel model = (EndpointTableModel) endpointsTable.getModel();
//                List<PartitionInformation.EndpointHolder> selectedEndpoints = model.getEndpointsAt(endpointsTable.getSelectedRows());
//                for (PartitionInformation.EndpointHolder holder : selectedEndpoints) {
//                    PartitionInformation.EndpointHolder ep = (PartitionInformation.EndpointHolder) holder;
//                    endpointTableModel.remove(ep);
//                }
//                enableButtons();
//            }
//        });
//    }

    protected boolean isValidated() {
        boolean isValid = true;
        PartitionInformation pInfo = getSelectedPartition();
        OSSpecificFunctions partitionFunctions = pInfo.getOSSpecificFunctions();
        String partitionDirectory = partitionFunctions.getPartitionBase() + pInfo.getPartitionId();

        PartitionActions partActions = new PartitionActions(partitionFunctions);

        if (pInfo.isNewPartition()) {
            logger.info("Creating \"" + partitionDirectory + "\" Directory");
            try {
                File newPartDir = partActions.createNewPartition(partitionDirectory);
                partActions.copyTemplateFiles(newPartDir);
            } catch (IOException e) {
                logger.severe("Error while creating the new partition \"" + pInfo.getPartitionId() + "\": " + e.getMessage());
                isValid = false;
            }
        } else {
            //check if the name has changed
            String oldPartitionId = pInfo.getOldPartitionId();
            if (oldPartitionId == null || oldPartitionId.equals(pInfo.getPartitionId())) {
                isValid = true;
            } else {
                try {
                    partActions.changeDirName(pInfo.getOldPartitionId(), pInfo.getPartitionId());
                } catch (IOException e) {
                    logger.severe("Error while updating the \"" + pInfo.getPartitionId() + "\" partition: " + e.getMessage());
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    private PartitionInformation getSelectedPartition() {
        return (PartitionInformation) partitionList.getSelectedValue();
    }

    private ComboBoxModel getAvailableIpAddresses() {
        String localHostName;
        java.util.List<String> allIpAddresses = new ArrayList<String>();
        allIpAddresses.add("*");
        try {
            localHostName = InetAddress.getLocalHost().getCanonicalHostName();
            InetAddress[] localAddresses = InetAddress.getAllByName(localHostName);
            for (InetAddress localAddress : localAddresses) {
                allIpAddresses.add(localAddress.getHostAddress());
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not determine the network interfaces for this gateway. Please run the system configuration wizard");
        }
        return new DefaultComboBoxModel(allIpAddresses.toArray(new String[0]));
    }

    //Models for the lists on this form
    private class PartitionListModel extends AbstractListModel {
        java.util.List<PartitionInformation> partitions = new ArrayList<PartitionInformation>();
        public int getSize() {
            return partitions.size();
        }

        public Object getElementAt(int index) {
            return partitions.get(index);
        }
        
        public void add(PartitionInformation newpartition) {
            if (!partitions.contains(newpartition)) {
                try {
                    partitions.add(newpartition);
                } finally {
                    fireContentsChanged(partitionList, 0, partitions.size());
                }
            }
        }

        public void update(int itemIndex,
                           String partitionName,
                           String description,
                           Map<PartitionInformation.EndpointType, PartitionInformation.EndpointHolder> endpoints) {
            if (itemIndex > 0 && itemIndex < partitions.size()) {
                PartitionInformation oldInformation = (PartitionInformation) partitionList.getSelectedValue();

                if (partitionName != null) oldInformation.setPartitionId(partitionName);
                if (endpoints != null) oldInformation.setEndpointsList(endpoints);

                fireContentsChanged(partitionList, 0, partitions.size());
            }
        }

        public void clear() {
            partitions.clear();
        }

        public void remove(PartitionInformation partitionToRemove) {
            try {
                if (partitions.size() != 0 && partitions.contains(partitionToRemove)) {
                    PartitionActions partActions = new PartitionActions(partitionToRemove.getOSSpecificFunctions());
                    if (partActions.removePartitionDirectory(partitionToRemove))
                        partitions.remove(partitionToRemove);
                }
            } finally {
                fireContentsChanged(partitionList, 0, partitions.size());
            }
        }
    }

    private class EndpointTableModel extends AbstractTableModel {

        Map<PartitionInformation.EndpointType, PartitionInformation.EndpointHolder> endpoints;

        public EndpointTableModel() {
            endpoints = new TreeMap<PartitionInformation.EndpointType, PartitionInformation.EndpointHolder>();
//            PartitionInformation.EndpointHolder.populateDefaultEndpoints(endpoints);
        }

        public int getRowCount() {
            return (endpoints == null?0:endpoints.size());
        }

        public int getColumnCount() {
            return PartitionInformation.EndpointHolder.getHeadings().length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PartitionInformation.EndpointHolder holder = getEndpointAt(rowIndex);
            return holder.getValue(columnIndex);
        }

        public Class<?> getColumnClass(int columnIndex) {
             return PartitionInformation.EndpointHolder.getClassAt(columnIndex);
        }

        public String getColumnName(int column) {
            return PartitionInformation.EndpointHolder.getHeadings()[column];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            PartitionInformation.EndpointHolder holder = getEndpointAt(rowIndex);
            holder.setValueAt(columnIndex, aValue);
        }

        public int getSize() {
            return endpoints.size();
        }

        public PartitionInformation.EndpointHolder getEndpointAt(int selectedRow) {
            Set<Map.Entry<PartitionInformation.EndpointType,PartitionInformation.EndpointHolder>> entries =
                    endpoints.entrySet();

            int index = 0;
            Map.Entry<PartitionInformation.EndpointType, PartitionInformation.EndpointHolder> rightEntry = null;
            for (Map.Entry<PartitionInformation.EndpointType, PartitionInformation.EndpointHolder> entry : entries) {
                rightEntry = entry;
                if (index++ == selectedRow) break;
            }

            assert rightEntry != null;
            return rightEntry.getValue();
        }

        public List<PartitionInformation.EndpointHolder> getEndpointsAt(int[] selectedRows) {
            List<PartitionInformation.EndpointHolder> selectedOnes = new ArrayList<PartitionInformation.EndpointHolder>();
            for (int selectedRow : selectedRows) {
                selectedOnes.add(getEndpointAt(selectedRow));
            }
            return selectedOnes;
        }

//        public void remove(PartitionInformation.EndpointHolder holder) {
//            endpoints.remove(holder);
//        }

//        public void add(PartitionInformation.EndpointHolder holder) {
//            if (!endpoints.contains(holder.get))
//                endpoints.add(holder);
//            fireTableDataChanged();
//        }

        Map<PartitionInformation.EndpointType, PartitionInformation.EndpointHolder> getEndpoints() {
            return endpoints;
        }

        public void clear() {
            endpoints.clear();
            fireTableDataChanged();
        }

        public void addEndpoints(Map<PartitionInformation.EndpointType, PartitionInformation.EndpointHolder> ehList) {
            endpoints = ehList;
            fireTableDataChanged();
        }
    }

    ActionListener managePartitionActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            doManagePartition(e);
        }
    };
}
