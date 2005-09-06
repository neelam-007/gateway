package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.commands.ClusteringConfigCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

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
    private JPanel clusterTypePanel;
    private JPanel clusterCreatePanel;
    private JPanel clusterClonePanel;

    private JLabel ssgHostnameLabel;
    private JTextField newHostname;
    private JRadioButton newClusterOption;
    private JRadioButton joinClusterOption;
    private JRadioButton existingClusterOption;
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
        clusterGroup.add(existingClusterOption);
        clusterGroup.add(newClusterOption);
        clusterGroup.add(joinClusterOption);

        existingClusterOption.addActionListener(clusterChangeListener);
        newClusterOption.addActionListener(clusterChangeListener);
        joinClusterOption.addActionListener(clusterChangeListener);

        useSsgHostnameOption.setSelected(true);
        existingClusterOption.setSelected(true);

        clusterClonePanel.setVisible(false); //change this if we decide to collect master info and copy keys
        enableControls();
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateView(HashMap settings) {

        //get the local host name text for the label
        try
        {
            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            clusteringConfigBean.setLocalHostName(localMachine.getHostName());
        } catch(java.net.UnknownHostException uhe) {
            System.out.println(uhe.getMessage());
        }

        //now get the cluster host name for the text box
        String clusterHostname = osFunctions.getClusterHostName();

        if (StringUtils.isEmpty(getParentWizard().getHostname())) {
            if (clusterHostname != null) {
                clusteringConfigBean.setClusterHostname(clusterHostname);
                clusteringConfigBean.setNewHostName(true);
            }
            else {
                clusteringConfigBean.setNewHostName(false);
            }
        }
        //now enable the controls appropriately
        if (clusteringConfigBean.isNewHostName()) {
            useNewHostnameOption.setSelected(true);
        }

        ssgHostnameLabel.setText(clusteringConfigBean.getLocalHostName());
        newHostname.setText(clusteringConfigBean.getClusterHostname());
    }

    protected void updateModel(HashMap settingsMap) {
        String hostnameForWizard;
        if (useNewHostnameOption.isSelected()) {
            clusteringConfigBean.setNewHostName(true);
            clusteringConfigBean.setClusterHostname(newHostname.getText());
            hostnameForWizard = newHostname.getText();
        }
        else {
            clusteringConfigBean.setNewHostName(false);
            clusteringConfigBean.setClusterHostname("");
            hostnameForWizard = clusteringConfigBean.getLocalHostName();
        }

        if (existingClusterOption.isSelected()) {
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
        boolean showMsg = false;
        ArrayList msgs = new ArrayList();
        if (newClusterOption.isSelected() || joinClusterOption.isSelected()) {
            showMsg = true;
            msgs.add("\n- UPDATE HOSTS FILE: add or update a line which contains the IP address for this ssgm, followed by the cluster host name and then true hostname");
        }

        if (joinClusterOption.isSelected()) {
            msgs.add("\n- COPY KEYS: copy the certificates and keystores from the primary node in the cluster to \"" + osFunctions.getKeystoreDir() + "\"");
        }

        if (showMsg == true) {
            String title = "Necessary Manual Action Required";

            StringBuffer buffer = new StringBuffer();
            buffer.append("Please note, you will need to perform the following manual tasks once this wizard is finished in order to properly configure the cluster");
            Iterator iter = msgs.iterator();
            while (iter.hasNext()) {
                buffer.append((String)iter.next());
            }
            JOptionPane.showMessageDialog(this, buffer.toString(), title, JOptionPane.INFORMATION_MESSAGE);
        }
        return true;
    }
}
