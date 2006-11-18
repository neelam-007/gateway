package com.l7tech.server.config.ui.gui;

import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.beans.PartitionConfigBean;
import com.l7tech.server.config.commands.PartitionConfigCommand;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.Set;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 11:37:25 AM
 */
public class ConfigWizardPartitioningPanel extends ConfigWizardStepPanel{

    PartitionConfigBean partitionBean;
    private JPanel mainPanel;
    private JTextField partitionName;
    private JList partitionList;
    private DefaultListModel listData;

    public ConfigWizardPartitioningPanel(WizardStepPanel next) {
        super(next);
        init();
    }

    private void init() {
        configBean = new PartitionConfigBean();
        configCommand = new PartitionConfigCommand(configBean);
        stepLabel = "Select Partition";
        listData = new DefaultListModel();
        partitionList.setModel(listData);
        setShowDescriptionPanel(false);

        initListeners();

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.NORTH);
    }

    private void enableNextButton() {
        getParentWizard().getNextButton().setEnabled(hasPartitionName());
    }


    public boolean canAdvance() {
        return hasPartitionName();
    }

    private boolean hasPartitionName() {
        return StringUtils.isNotEmpty(partitionName.getText());
    }

    private void initListeners() {
        partitionList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                partitionName.setText((String) partitionList.getSelectedValue());
            }
        });

        partitionName.getDocument().addDocumentListener(
                new RunOnChangeListener(new Runnable() {
                    public void run() {
                        doPartitionNameChange();
                    }
                }
            )
        );
    }

    private void doPartitionNameChange() {
        enableNextButton();
    }

    protected void updateModel() {
        String whichPartition = getPartitionName();
        PartitionConfigBean partBean = (PartitionConfigBean) configBean;
        partBean.setNewPartition(!listData.contains(whichPartition));
        partBean.setPartitionName(whichPartition);
    }

    private String getPartitionName() {
        return partitionName.getText();
    }

    protected void updateView() {
        Set<String> partNames = PartitionManager.getInstance().getPartitionNames();
        listData.clear();
        for (String partName : partNames) {
            listData.addElement(partName);
        }
        enableNextButton();
    }
}
