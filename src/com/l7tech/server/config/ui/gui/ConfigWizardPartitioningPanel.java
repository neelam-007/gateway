package com.l7tech.server.config.ui.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.text.MaxLengthDocument;
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
    private JTable httpEndpointsTable;
    private JTable otherEndpointsTable;
    private PartitionListModel partitionListModel;

    PartitionConfigBean partitionBean;

    private HttpEndpointTableModel httpEndpointTableModel;
    private OtherEndpointTableModel otherEndpointTableModel;

    ActionListener managePartitionActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            doManagePartition(e);
        }
    };

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

        httpEndpointTableModel = new HttpEndpointTableModel();
        httpEndpointsTable.setModel(httpEndpointTableModel);

        otherEndpointTableModel = new OtherEndpointTableModel();
        otherEndpointsTable.setModel(otherEndpointTableModel);

        TableColumn col = httpEndpointsTable.getColumnModel().getColumn(1);
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
                httpEndpointsTable.getModel().getRowCount() > 0;
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

        partitionName.setDocument(new MaxLengthDocument(128));

        partitionList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                updateProperties();
            }
        });
        
        httpEndpointsTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                enableButtons();
            }
        });

        otherEndpointsTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                enableButtons();
            }
        });

        addPartition.addActionListener(managePartitionActionListener);
        removePartition.addActionListener(managePartitionActionListener);
    }

    private void enableButtons() {
        enablePartitionButtons();
        enableNextButton();
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
            httpEndpointTableModel.addEndpoints(pi.getHttpEndpoints());
            otherEndpointTableModel.addEndpoints(pi.getOtherEndpoints());
        }
        enableButtons();
    }

    private void enableProperties(boolean enabled) {
        Utilities.setEnabled(propertiesPanel, enabled);
    }

    protected void updateModel() {
        PartitionInformation pi = getSelectedPartition();

        pi.setPartitionId(partitionName.getText());
        pi.setHttpEndpointsList(httpEndpointTableModel.getEndpoints());
        pi.setOtherEndpointsList(otherEndpointTableModel.getEndpoints());

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
        if (size == 8) {
            addPartition.setEnabled(false);
            addPartition.setToolTipText("A maximum of 8 partitions is supported");
        } else {
            addPartition.setEnabled(true);
            addPartition.setToolTipText("Click to add a new partition");
        }


        int index = partitionList.getSelectedIndex();
        
        //if nothing is selected yet, select the first element
        if (index < 0) {
            index = 0;
            partitionList.setSelectedIndex(index);
        }

        boolean validRowSelected = (size != 0) && (0 <= index) && (index < size);
        removePartition.setEnabled(validRowSelected);
    }

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
                           List<PartitionInformation.HttpEndpointHolder> endpoints) {
            if (itemIndex > 0 && itemIndex < partitions.size()) {
                PartitionInformation oldInformation = (PartitionInformation) partitionList.getSelectedValue();

                if (partitionName != null) oldInformation.setPartitionId(partitionName);
                if (endpoints != null) oldInformation.setHttpEndpointsList(endpoints);

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

    private class HttpEndpointTableModel extends AbstractTableModel {

        private List<PartitionInformation.HttpEndpointHolder> httpEndpoints;

        public HttpEndpointTableModel() {
            httpEndpoints = new ArrayList<PartitionInformation.HttpEndpointHolder>();
        }

        public int getRowCount() {
            return (httpEndpoints == null?0: httpEndpoints.size());
        }

        public int getColumnCount() {
            return PartitionInformation.HttpEndpointHolder.getHeadings().length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PartitionInformation.HttpEndpointHolder holder = getEndpointAt(rowIndex);
            return holder.getValue(columnIndex);
        }

        public Class<?> getColumnClass(int columnIndex) {
             return PartitionInformation.HttpEndpointHolder.getClassAt(columnIndex);
        }

        public String getColumnName(int column) {
            return PartitionInformation.HttpEndpointHolder.getHeadings()[column];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            PartitionInformation.HttpEndpointHolder holder = getEndpointAt(rowIndex);
            holder.setValueAt(columnIndex, aValue);
        }

        public int getSize() {
            return httpEndpoints.size();
        }

        public PartitionInformation.HttpEndpointHolder getEndpointAt(int selectedRow) {
            if (selectedRow >=0 && selectedRow < httpEndpoints.size())
                    return httpEndpoints.get(selectedRow);
            return null;
        }

        public List<PartitionInformation.HttpEndpointHolder> getEndpointsAt(int[] selectedRows) {
            List<PartitionInformation.HttpEndpointHolder> selectedOnes = new ArrayList<PartitionInformation.HttpEndpointHolder>();
            for (int selectedRow : selectedRows) {
                selectedOnes.add(getEndpointAt(selectedRow));
            }
            return selectedOnes;
        }

        List<PartitionInformation.HttpEndpointHolder> getEndpoints() {
            return httpEndpoints;
        }

        public void clear() {
            httpEndpoints.clear();
            fireTableDataChanged();
        }

        public void addEndpoints(List<PartitionInformation.HttpEndpointHolder> ehList) {
            httpEndpoints = ehList;
            fireTableDataChanged();
        }
    }

    private class OtherEndpointTableModel extends AbstractTableModel {
        private List<PartitionInformation.OtherEndpointHolder> otherEndpoints;
        String[] headings = new String[] {
                "Description",
                "Port"
        };

         public OtherEndpointTableModel() {
            otherEndpoints = new ArrayList<PartitionInformation.OtherEndpointHolder>();
        }

        public int getRowCount() {
            return otherEndpoints == null?0:otherEndpoints.size();
        }

        public int getColumnCount() {
            return headings.length;
        }


        public Class<?> getColumnClass(int columnIndex) {
            return PartitionInformation.OtherEndpointHolder.getClassAt(columnIndex);
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PartitionInformation.OtherEndpointHolder holder = otherEndpoints.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return holder.endpointType.getName();
                case 1:
                    return holder.port;
            }
            return null;
        }

        public String getColumnName(int column) {
            return headings[column];
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            PartitionInformation.OtherEndpointHolder holder = getEndpointAt(rowIndex);
            holder.setValueAt(columnIndex, aValue);
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        public int getSize() {
            return otherEndpoints.size();
        }

        public PartitionInformation.OtherEndpointHolder getEndpointAt(int selectedRow) {
            if (selectedRow >=0 && selectedRow < otherEndpoints.size())
                    return otherEndpoints.get(selectedRow);
            return null;
        }

        List<PartitionInformation.OtherEndpointHolder> getEndpoints() {
            return otherEndpoints;
        }

        public void clear() {
            otherEndpoints.clear();
            fireTableDataChanged();
        }

        public void addEndpoints(List<PartitionInformation.OtherEndpointHolder> ehList) {
            otherEndpoints = ehList;
            fireTableDataChanged();
        }
    }
}
