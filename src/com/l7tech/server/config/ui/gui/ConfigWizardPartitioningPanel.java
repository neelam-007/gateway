package com.l7tech.server.config.ui.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.NumberField;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.PartitionActions;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.PartitionConfigBean;
import com.l7tech.server.config.commands.PartitionConfigCommand;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 11:37:25 AM
 */
public class ConfigWizardPartitioningPanel extends ConfigWizardStepPanel{
    private static final Logger logger = Logger.getLogger(ConfigWizardPartitioningPanel.class.getName());

    private JPanel mainPanel;
    private JList partitionList;
    private JButton addPartition;
    private JButton removePartition;

    private JPanel propertiesPanel;
    private JTextField partitionName;
    private JTable httpEndpointsTable;
    private JTable otherEndpointsTable;
    private JLabel errorMessageLabel;
    private PartitionListModel partitionListModel;

    PartitionConfigBean partitionBean;

    private HttpEndpointTableModel httpEndpointTableModel;
    private OtherEndpointTableModel otherEndpointTableModel;

    private FilterDocument.Filter partitionNameFilter;
    public static String pathSeparator = File.separator;

    private List<PartitionInformation> partitionsAdded;
    private int newPartitionIndex = 0;

    ActionListener managePartitionActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            doManagePartition(e);
        }
    };

   public ConfigWizardPartitioningPanel(WizardStepPanel next) {
        super(next);
        partitionNameFilter = new FilterDocument.Filter() {
            public boolean accept(String s) {
                return s.matches("[^" + pathSeparator + "\\s]{1,128}");
            }
        };

        stepLabel = "Configure Partitions";
        setShowDescriptionPanel(false);
        partitionList.setCellRenderer(new PartitionListRenderer());
        partitionsAdded = new ArrayList<PartitionInformation>();
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

        TableColumn httpEndpointsIpColumn = httpEndpointsTable.getColumnModel().getColumn(1);
        httpEndpointsIpColumn.setCellEditor(new DefaultCellEditor(new JComboBox(new DefaultComboBoxModel(PartitionActions.getAvailableIpAddresses()))));

        TableColumn httpEndpointsPortColumn = httpEndpointsTable.getColumnModel().getColumn(2);
        httpEndpointsPortColumn.setCellEditor(new DefaultCellEditor(new JTextField(new NumberField(5), null, 0)));
        httpEndpointsPortColumn.setCellRenderer(new PortNumberRenderer(httpEndpointTableModel));

        otherEndpointTableModel = new OtherEndpointTableModel();
        otherEndpointsTable.setModel(otherEndpointTableModel);

        TableColumn otherEndpointsPortColumn = otherEndpointsTable.getColumnModel().getColumn(1);
        otherEndpointsPortColumn.setCellEditor(new DefaultCellEditor(new JTextField(new NumberField(5), null, 0)));
        otherEndpointsPortColumn.setCellRenderer(new PortNumberRenderer(otherEndpointTableModel));

        initListeners();

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        errorMessageLabel.setVisible(false);
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
        String newName;
        do {
            newName = "NewPartition" + (newPartitionIndex == 0?"":String.valueOf(newPartitionIndex));
            newPartitionIndex++;
        } while (partitionListModel.contains(newName));

        PartitionInformation pi = new PartitionInformation(newName);
        partitionListModel.add(pi);
        partitionList.setSelectedValue(pi, true);
        partitionName.requestFocus();
        partitionName.setSelectionStart(0);
        partitionName.setSelectionEnd(newName.length());
        partitionsAdded.add(pi);
    }

    private void doRemovePartition() {
        Object[] deleteThem = partitionList.getSelectedValues();
        for (Object o : deleteThem) {
            final PartitionInformation pi = (PartitionInformation) o;
            if (!pi.getPartitionId().equals(PartitionInformation.DEFAULT_PARTITION_NAME)) {
                Utilities.doWithConfirmation(
                    ConfigWizardPartitioningPanel.this,
                    "Remove Partition", "Are you sure you want to remove the \"" + pi.getPartitionId() + "\" partition? This cannot be undone.",
                    new Runnable() {
                        public void run() {
                            String message = "";
                            String title = "";
                            int type;
                            if (partitionListModel.remove(pi)) {
                                message = "All Selected Partitions have now been deleted. You may continue to use the wizard to configure other partitions or exit now.";
                                title = "Deletion Complete";
                                type = JOptionPane.INFORMATION_MESSAGE;
                            } else {
                                message = "Could not delete the selected partitions. Check that you have permission to delete directories in " + pi.getOSSpecificFunctions().getPartitionBase() + ".";
                                title = "Could Not Delete";
                                type = JOptionPane.ERROR_MESSAGE;

                            }
                            JOptionPane.showMessageDialog(ConfigWizardPartitioningPanel.this, message, title, type);
                            enableEditDeletePartitionButtons();
                        }
                    }
                );
            }
        }
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
        partitionName.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                String newname = partitionName.getText() + e.getKeyChar();
                int index = partitionList.getSelectedIndex();
                if (index > 0 && index < partitionListModel.getSize()) {
                    if (!partitionListModel.contains(newname))
                        partitionListModel.update(index, newname, null, null);
                    else
                        JOptionPane.showMessageDialog(
                                ConfigWizardPartitioningPanel.this,
                                "There is already a \"" + newname + "\" partition",
                                "Duplicate Partition Name",
                                JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        partitionName.setDocument(new FilterDocument(128, partitionNameFilter));

        partitionList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                updateProperties();
                enableNameField();
            }
        });

        partitionList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    doRemovePartition();
                }
            }
        });

        addPartition.addActionListener(managePartitionActionListener);
        removePartition.addActionListener(managePartitionActionListener);
    }

    private void enableNameField() {
        PartitionInformation pi = getSelectedPartition();
        boolean isEnabled = pi != null && !pi.getPartitionId().equals(PartitionInformation.DEFAULT_PARTITION_NAME);
        partitionName.setEditable(isEnabled);
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

        // don't NPE on back button
        if (pi != null) {
            pi.setPartitionId(partitionName.getText());
            pi.setHttpEndpointsList(httpEndpointTableModel.getEndpoints());
            pi.setOtherEndpointsList(otherEndpointTableModel.getEndpoints());

            PartitionConfigBean partBean = (PartitionConfigBean) configBean;
            partBean.setPartition(pi);
            osFunctions = pi.getOSSpecificFunctions();
        }
        getParentWizard().setActivePartition(pi);
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
        PartitionInformation pi = getSelectedPartition();

        removePartition.setEnabled(validRowSelected && pi != null && !pi.getPartitionId().equals(PartitionInformation.DEFAULT_PARTITION_NAME));
    }

    protected boolean isValidated() {
        boolean isValid = true;
        saveEditsToCurrentPartition();

        PartitionInformation pInfo = getSelectedPartition();

        if (PartitionActions.validateAllPartitionEndpoints(pInfo, false)) {
            OSSpecificFunctions partitionFunctions = pInfo.getOSSpecificFunctions();
            PartitionActions partActions = new PartitionActions(partitionFunctions);
            createNewPartitions(partActions);

            if (!pInfo.isNewPartition()) {
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
            errorMessageLabel.setVisible(false);
        } else {
            errorMessageLabel.setVisible(true);
            isValid = false;
            updateProperties();
        }
        return isValid;
    }

    private boolean createNewPartitions(PartitionActions partActions) {
        boolean hadErrors = false;
        for (PartitionInformation newPartition : partitionsAdded) {
            try {
                partActions.createNewPartition(newPartition.getPartitionId());
            } catch (IOException e) {
                logger.severe("Error while creating the new partition \"" + newPartition + "\": " + e.getMessage());
                hadErrors = true;
            }
        }
        return hadErrors;
    }

    private void saveEditsToCurrentPartition() {
        TableCellEditor editor = httpEndpointsTable.getCellEditor();
        if (editor != null)
            editor.stopCellEditing();

        editor = otherEndpointsTable.getCellEditor();
        if (editor != null)
            editor.stopCellEditing();
    }

    private PartitionInformation getSelectedPartition() {
        return (PartitionInformation) partitionList.getSelectedValue();
    }

    //Models for the lists on this form
    private class PartitionListModel extends AbstractListModel {
        java.util.List<PartitionInformation> partitions = new ArrayList<PartitionInformation>();
        public int getSize() {
            return partitions.size();
        }

        public boolean contains(String partitionName) {
            return partitions.contains(new PartitionInformation(partitionName));
        }

        public Object getElementAt(int index) {
            if (index < partitions.size() && index > 0)
                return partitions.get(index);
            else
                return partitions.get(0);
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

        public boolean remove(PartitionInformation partitionToRemove) {
            boolean removed = false;
            try {
                if (partitions.size() != 0 && partitions.contains(partitionToRemove)) {
                    int index = partitions.indexOf(partitionToRemove);

                    PartitionActions partActions = new PartitionActions(partitionToRemove.getOSSpecificFunctions());
                    File partitionDir = new File(osFunctions.getPartitionBase() + partitionToRemove.getPartitionId());
                    if (!(partitionDir.exists())) {
                        //we are removing something from the list that isn't yet on disk so remove only the entry
                        // from the model
                        partitions.remove(partitionToRemove);
                        removed = true;
                    }
                    else {
                        if (partActions.removePartition(partitionToRemove)) {
                            partitions.remove(partitionToRemove);
                            removed = true;
                        }
                    }
                    if (removed)
                        partitionList.setSelectedIndex(index == 0?0:index-1);
                }
            } finally {
                fireContentsChanged(partitionList, 0, partitions.size());
            }
            return removed;
        }

        public void clear() {
            partitions.clear();
        }
    }

    private abstract class EndpointTableModel extends AbstractTableModel {
        public PartitionInformation.EndpointHolder getEndpointAtRow(int row) {
            return getEndpointAt(row);
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            PartitionInformation.EndpointHolder holder = getEndpointAt(rowIndex);
            holder.setValueAt(columnIndex, aValue);
            doAfterSetValue(aValue, rowIndex, columnIndex);
        }

        protected abstract void doAfterSetValue(Object aValue, int rowIndex, int columnIndex);

        public abstract PartitionInformation.EndpointHolder getEndpointAt(int row);
    }

    private class HttpEndpointTableModel extends EndpointTableModel {

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

        protected void doAfterSetValue(Object aValue, int rowIndex, int columnIndex) {
            PartitionActions.validatePartitionEndpoints(getSelectedPartition(), false);
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

        public void addEndpoints(List<PartitionInformation.HttpEndpointHolder> ehList) {
            httpEndpoints = ehList;
            fireTableDataChanged();
        }
    }

    private class OtherEndpointTableModel extends EndpointTableModel {
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
            PartitionInformation.OtherEndpointHolder holder = getEndpointAt(rowIndex);
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

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        public int getSize() {
            return otherEndpoints.size();
        }

        protected void doAfterSetValue(Object aValue, int rowIndex, int columnIndex) {
            PartitionActions.validatePartitionEndpoints(getSelectedPartition(), false);
        }

        public PartitionInformation.OtherEndpointHolder getEndpointAt(int selectedRow) {
            if (selectedRow >=0 && selectedRow < otherEndpoints.size())
                    return otherEndpoints.get(selectedRow);
            return null;
        }

        List<PartitionInformation.OtherEndpointHolder> getEndpoints() {
            return otherEndpoints;
        }

        public void addEndpoints(List<PartitionInformation.OtherEndpointHolder> ehList) {
            otherEndpoints = ehList;
            fireTableDataChanged();
        }
    }

    class PortNumberRenderer extends DefaultTableCellRenderer {

        EndpointTableModel endpointModel;
        public PortNumberRenderer(EndpointTableModel model) {
            super();
            this.endpointModel = model;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            TableCellRenderer renderer = table.getDefaultRenderer(table.getModel().getColumnClass(column));
            Component x = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (table != null) {
                if (endpointModel != null) {
                    PartitionInformation.EndpointHolder holder = endpointModel.getEndpointAtRow(row);
                    if (StringUtils.isNotEmpty(holder.validationMessaqe)) {
                        ((JLabel)x).setForeground(Color.RED);
                        ((JLabel)x).setToolTipText(holder.validationMessaqe);
                    } else {
                        ((JLabel)x).setForeground(Color.BLACK);
                        ((JLabel)x).setToolTipText(null);
                    }
                }
            }
            return x;
        }
    }

    class PartitionListRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PartitionInformation) {
                PartitionInformation partitionInformation = (PartitionInformation) value;
                if (!partitionInformation.isEnabled()) {
                    label.setForeground(Color.GRAY);
                }
            }
            return label;
        }
    }
}