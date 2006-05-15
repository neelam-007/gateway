package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.commands.ClusteringConfigCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;
import java.net.InetAddress;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 9, 2005
 * Time: 3:25:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardClusteringPanel extends ConfigWizardStepPanel {
    private JPanel mainPanel;
    private JPanel clusterClonePanel;

    private JLabel ssgHostnameLabel;
    private JTextField newHostname;
    private JRadioButton newClusterOption;
    private JRadioButton joinClusterOption;
    private JRadioButton noClusterOption;
    private JRadioButton useSsgHostnameOption;
    private JRadioButton useNewHostnameOption;
    private JTextField cloneHostname;
    private JTextField cloneUsername;
    private JPasswordField clonePassword;
    private JLabel cloneHostnameLabel;
    private JLabel cloneUsernameLabel;
    private JLabel clonePasswordLabel;

    ButtonGroup hostnameGroup;
    ButtonGroup clusterGroup;

    ClusteringConfigBean clusteringConfigBean;

    private ClusterChangeListener clusterChangeListener = new ClusterChangeListener();
    private JLabel emptyHostnameLabel;


    private final class ClusterChangeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            enableControls();
        }
    }

    /**
     * Creates new form WizardPanel
     */
    public ConfigWizardClusteringPanel(OSSpecificFunctions functions) {
        super(null, functions);
        init();
    }

    public ConfigWizardClusteringPanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
        setShowDescriptionPanel(false);
        configBean = new ClusteringConfigBean(osFunctions);
        configCommand = new ClusteringConfigCommand(configBean);

        clusteringConfigBean = (ClusteringConfigBean) configBean;
        stepLabel = "Setup SSG Clustering";

        hostnameGroup = new ButtonGroup();
        hostnameGroup.add(useSsgHostnameOption);
        hostnameGroup.add(useNewHostnameOption);


        clusterGroup = new ButtonGroup();
        clusterGroup.add(noClusterOption);
        clusterGroup.add(newClusterOption);
        clusterGroup.add(joinClusterOption);

        noClusterOption.addActionListener(clusterChangeListener);
        newClusterOption.addActionListener(clusterChangeListener);
        joinClusterOption.addActionListener(clusterChangeListener);

        useSsgHostnameOption.setSelected(true);
        noClusterOption.setSelected(true);
        emptyHostnameLabel.setForeground(Color.RED);

        clusterClonePanel.setVisible(false); //change this if we decide to collect master info and copy keys
        enableControls();
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateView(Set settings) {

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

    protected void updateModel(Set settings) {
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
            clusteringConfigBean.setDoClusterType(ClusteringConfigBean.CLUSTER_NONE);
        }

        else if (newClusterOption.isSelected()) {
            clusteringConfigBean.setDoClusterType(ClusteringConfigBean.CLUSTER_NEW);
        }
        else if (joinClusterOption.isSelected()) {
            clusteringConfigBean.setDoClusterType(ClusteringConfigBean.CLUSTER_JOIN);
            clusteringConfigBean.setCloneHostname(cloneHostname.getText());
            clusteringConfigBean.setCloneUsername(cloneUsername.getText());
            clusteringConfigBean.setClonePassword(clonePassword.getPassword());
        }
        else { //somehow none of these is selected so it's best not to do anything with the cluster
            clusteringConfigBean.setDoClusterType(ClusteringConfigBean.CLUSTER_NONE);
        }

        //set the hostname in the wizard so it can be used by later panels
        getParentWizard().setHostname(hostnameForWizard);
        getParentWizard().setClusteringType(clusteringConfigBean.getClusterType());
    }

    private void enableControls() {
        boolean enableCloneControls = joinClusterOption.isSelected();

        cloneHostnameLabel.setEnabled(enableCloneControls);
        cloneUsernameLabel.setEnabled(enableCloneControls);
        clonePasswordLabel.setEnabled(enableCloneControls);

        cloneHostname.setEnabled(enableCloneControls);
        cloneUsername.setEnabled(enableCloneControls);
        clonePassword.setEnabled(enableCloneControls);
    }

    public boolean onNextButton() {
        boolean validInput = true;
        if (useNewHostnameOption.isSelected()) {
            if (StringUtils.isEmpty(newHostname.getText())) {
                validInput = false;
                emptyHostnameLabel.setVisible(true);
            }
        }
        return validInput;
    }
}
