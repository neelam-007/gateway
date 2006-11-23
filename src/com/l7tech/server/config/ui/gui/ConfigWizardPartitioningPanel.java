package com.l7tech.server.config.ui.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.beans.PartitionConfigBean;
import com.l7tech.server.config.commands.PartitionConfigCommand;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 11:37:25 AM
 */
public class ConfigWizardPartitioningPanel extends ConfigWizardStepPanel{

    private JPanel mainPanel;
    private JList partitionList;
    private JPanel partitionSelectionPanel;
    private JButton addPartition;
    private JButton removePartition;
    private JButton editPartition;

    private JPanel propertiesPanel;
    private JTextField partitionName;
    private JButton addEndpoint;
    private JButton editEndpoint;
    private JButton removeEndpoint;
    private PartitionListModel partitionListModel;

    PartitionConfigBean partitionBean;

    private JList endpointList;
    private EndpointListModel endpointListModel;

    ActionListener manageParitionActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            doManagePartition(e);
        }
    };

    public ConfigWizardPartitioningPanel(WizardStepPanel next) {
        super(next);
        init();
    }

    private void init() {
        configBean = new PartitionConfigBean();
        configCommand = new PartitionConfigCommand(configBean);
        stepLabel = "Select Partition";
        partitionListModel = new PartitionListModel();
        partitionList.setModel(partitionListModel);

        endpointListModel = new EndpointListModel();
        endpointList.setModel(endpointListModel);

        setShowDescriptionPanel(false);

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
            } else if (button == editPartition) {
                doEditPartition();
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

    private  void doEditPartition() {
    }

    private void doRemovePartition() {
        Utilities.doWithConfirmation(
                ConfigWizardPartitioningPanel.this,
                "Remove Partition", "Are you sure you want to remove the selected partition?", new Runnable() {
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
                endpointList.getModel().getSize() > 0;
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
                enablePartitionButtons();
            }
        });

       endpointList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                enableEndpointButtons();
            }
        });

        partitionList.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >=2) {
                    doEditPartition();
                }
            }
        });

        addPartition.addActionListener(manageParitionActionListener);
        editPartition.addActionListener(manageParitionActionListener);
        removePartition.addActionListener(manageParitionActionListener);

        addEndpoint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doAddEndpoint();
            }
        });

        editEndpoint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doEditEndpoint();            }
        });

        removeEndpoint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doRemoveEndpoint();
            }
        });        
    }

    private void enableEndpointButtons() {
        enableEditDeleteEndpointButtons();
    }

    private void enableEditDeleteEndpointButtons() {
        int size = endpointList.getModel().getSize();
        int index = partitionList.getSelectedIndex();

        boolean validRowSelected = size != 0 &&
                 index > 0 && index < size;
        removeEndpoint.setEnabled(validRowSelected);
        editEndpoint.setEnabled(validRowSelected);
    }

    private void enablePartitionButtons() {
        enableNextButton();
        enableEditDeletePartitionButtons();
    }

    private void updateProperties() {
        Object o = partitionList.getSelectedValue();
        enableProperties(o != null);

        if (o != null) {
            PartitionInformation pi = (PartitionInformation) o;
            partitionName.setText(pi.getPartitionId());

            endpointListModel.clear();
            List<PartitionInformation.EndpointHolder> ehList = pi.getEndpointsList();
            if (ehList != null) {
                for (PartitionInformation.EndpointHolder endpoint : ehList) {
                    endpointListModel.add(endpoint);
                }
            }
        }
        enableEndpointButtons();
    }

    private void enableProperties(boolean enabled) {
        Utilities.setEnabled(propertiesPanel, enabled);
    }

    protected void updateModel() {
        PartitionInformation pi = (PartitionInformation) partitionList.getSelectedValue();

        pi.setPartitionId(partitionName.getText());
        pi.setEndpointsList(endpointListModel.getEndpoints());

        PartitionConfigBean partBean = (PartitionConfigBean) configBean;
        partBean.setPartition(pi);
    }

    protected void updateView() {
        enablePartitionButtons();
        enableEndpointButtons();
    }

    private void enableEditDeletePartitionButtons() {
        int size = partitionListModel.getSize();
        int index = partitionList.getSelectedIndex();

        boolean validRowSelected = size != 0 &&
                 index > 0 && index < size;
        removePartition.setEnabled(validRowSelected);
        editPartition.setEnabled(validRowSelected);
    }

    private void doAddEndpoint() {
        EditEndpointDialog dlg = new EditEndpointDialog(getParentWizard(), null);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        PartitionInformation.EndpointHolder endpoint = dlg.getEndpoint();
        if (endpoint != null) {
            endpointListModel.add(endpoint);

        }
    }

    private void doEditEndpoint() {
        PartitionInformation.EndpointHolder endpoint = (PartitionInformation.EndpointHolder) endpointList.getSelectedValue();
        EditEndpointDialog dlg = new EditEndpointDialog(getParentWizard(), endpoint);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
    }

    private void doRemoveEndpoint() {
        Utilities.doWithConfirmation(
            getParentWizard(),
            "Remove Endpoint", "Are you sure you want to remove the selected endpoint?", new Runnable() {
            public void run() {
                Object[] selectedEndpoints = endpointList.getSelectedValues();
                for (Object o : selectedEndpoints) {
                    PartitionInformation.EndpointHolder ep = (PartitionInformation.EndpointHolder) o;
                    endpointListModel.remove(ep);
                }
            }
        });
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

        public void update(int itemIndex, String partitionName, String description, List<PartitionInformation.EndpointHolder> endpoints) {
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
                    partitions.remove(partitionToRemove);
                }
            } finally {
                fireContentsChanged(partitionList, 0, partitions.size());
            }
        }
    }

    private class EndpointListModel extends AbstractListModel {
        java.util.List<PartitionInformation.EndpointHolder> endpoints;


        public EndpointListModel() {
            endpoints = new ArrayList<PartitionInformation.EndpointHolder>();
        }

        public int getSize() {
            return endpoints.size();
        }

        public Object getElementAt(int index) {
            return endpoints.get(index);
        }

        public void remove(PartitionInformation.EndpointHolder holder) {
            endpoints.remove(holder);
        }

        public void add(PartitionInformation.EndpointHolder holder) {
            if (!endpoints.contains(holder))
                endpoints.add(holder);
            fireContentsChanged(endpointList, 0, endpoints.size());
        }

        java.util.List<PartitionInformation.EndpointHolder> getEndpoints() {
            return endpoints;
        }

        public void clear() {
            endpoints.clear();
            fireContentsChanged(endpointList, 0, endpoints.size());
        }
    }
}
