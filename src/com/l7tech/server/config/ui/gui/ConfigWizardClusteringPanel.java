package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.commands.ClusteringConfigCommand;
import com.l7tech.server.config.ClusteringType;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.partition.PartitionInformation;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 9, 2005
 * Time: 3:25:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardClusteringPanel extends ConfigWizardStepPanel {
    private JPanel mainPanel;

    private JLabel ssgHostnameLabel;
    private JTextField newHostname;
    private JRadioButton newClusterOption;
    private JRadioButton joinClusterOption;
    private JRadioButton noClusterOption;
    private JRadioButton useSsgHostnameOption;
    private JRadioButton useNewHostnameOption;
    ButtonGroup hostnameGroup;
    ButtonGroup clusterGroup;

    ClusteringConfigBean clusteringConfigBean;


    private JLabel emptyHostnameLabel;

    public ConfigWizardClusteringPanel(WizardStepPanel next) {
        super(next);
        stepLabel = "Set Up SSG Clustering";
        setShowDescriptionPanel(false);
    }

    private void init() {
        osFunctions = getParentWizard().getOsFunctions();
        if (osFunctions == null) osFunctions = OSDetector.getOSSpecificFunctions(PartitionInformation.DEFAULT_PARTITION_NAME);

        configBean = new ClusteringConfigBean();
        configCommand = new ClusteringConfigCommand(configBean);

        clusteringConfigBean = (ClusteringConfigBean) configBean;
        
        hostnameGroup = new ButtonGroup();
        hostnameGroup.add(useSsgHostnameOption);
        hostnameGroup.add(useNewHostnameOption);

        clusterGroup = new ButtonGroup();
        clusterGroup.add(noClusterOption);
        clusterGroup.add(newClusterOption);
        clusterGroup.add(joinClusterOption);

        useSsgHostnameOption.setSelected(true);
        noClusterOption.setSelected(true);
        emptyHostnameLabel.setForeground(Color.RED);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateView() {

        if (osFunctions == null) init();

        emptyHostnameLabel.setVisible(false);

        //now get the cluster host name for the text box
        String clusterHostname = osFunctions.getClusterHostName();

        if (StringUtils.isEmpty(getParentWizard().getHostname())) {
            if (clusterHostname != null) {
                clusteringConfigBean.setClusterHostname(clusterHostname);
                if (!StringUtils.equals(clusterHostname, clusteringConfigBean.getLocalHostName())) {
                    clusteringConfigBean.setNewHostName(true);
                } else {
                    clusteringConfigBean.setNewHostName(false);
                }
            }
            else {
                clusteringConfigBean.setNewHostName(false);
            }
        }

        ssgHostnameLabel.setText(clusteringConfigBean.getLocalHostName());
        newHostname.setText(clusteringConfigBean.getClusterHostname());

        //now enable the controls appropriately
        if (clusteringConfigBean.isNewHostName()) {
            useNewHostnameOption.setSelected(true);
        }
    }

    protected void updateModel() {
        String hostnameForWizard; //this is the hostname that will be used to

        if (useNewHostnameOption.isSelected()) {
            clusteringConfigBean.setNewHostName(true);
            clusteringConfigBean.setClusterHostname(newHostname.getText());
            hostnameForWizard = newHostname.getText();
        }
        else {
            clusteringConfigBean.setNewHostName(false);
            clusteringConfigBean.setClusterHostname(clusteringConfigBean.getLocalHostName());
            hostnameForWizard = clusteringConfigBean.getLocalHostName();
        }

        if (noClusterOption.isSelected()) {
            clusteringConfigBean.setDoClusterType(ClusteringType.CLUSTER_NONE);
        }

        else if (newClusterOption.isSelected()) {
            clusteringConfigBean.setDoClusterType(ClusteringType.CLUSTER_NEW);
        }
        else if (joinClusterOption.isSelected()) {
            clusteringConfigBean.setDoClusterType(ClusteringType.CLUSTER_JOIN);
        }
        else { //somehow none of these is selected so it's best not to do anything with the cluster
            clusteringConfigBean.setDoClusterType(ClusteringType.CLUSTER_NONE);
        }

        //set the hostname in the wizard so it can be used by later panels
        getParentWizard().setHostname(hostnameForWizard);
        getParentWizard().setClusteringType(clusteringConfigBean.getClusterType());
    }

    public boolean isValidated() {
        if (useNewHostnameOption.isSelected()) {
            if (StringUtils.isEmpty(newHostname.getText())) {
                emptyHostnameLabel.setVisible(true);
                return false;
            }
        }
        return true;
    }
}
