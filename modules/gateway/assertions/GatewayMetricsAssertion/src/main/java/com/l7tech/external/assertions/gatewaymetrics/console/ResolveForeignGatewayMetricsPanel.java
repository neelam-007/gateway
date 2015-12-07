package com.l7tech.external.assertions.gatewaymetrics.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsExternalReference;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 3/20/13
 * Time: 4:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResolveForeignGatewayMetricsPanel extends WizardStepPanel {

    private static final ClusterNodeInfo ALL_NODES = new ClusterNodeInfo() {
        @Override
        public String toString() {
            return "<All Nodes>";
        }
    };

    private static final ServiceHeader ALL_SERVICES = new ServiceHeader(
        false, false, "<All Services>", null, null, null, null, null, -1L, -1, null, false, false, null, null);

    private JPanel mainPanel;
    private JLabel panelTitle;
    private JPanel detailsPanel;
    private JTextField nameTextField;
    private JRadioButton changeRadioButton;
    private JComboBox changeComboBox;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;

    private GatewayMetricsExternalReference foreignRef;

    public ResolveForeignGatewayMetricsPanel(WizardStepPanel next, GatewayMetricsExternalReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        this.initialize();
    }

    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return "Unresolved " + getGatewayReferenceTypeString() + " " + foreignRef.getName();
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (changeComboBox.getSelectedIndex() < 0) {
                return false;
            }
            switch (foreignRef.getType()) {
                case CLUSTER_NODE:
                    ClusterNodeInfo clusterNodeInfo = (ClusterNodeInfo) changeComboBox.getSelectedItem();
                    foreignRef.setLocalizeReplace(clusterNodeInfo.getId());
                    break;

                case PUBLISHED_SERVICE:
                    ServiceHeader selectedService = (ServiceHeader) changeComboBox.getSelectedItem();
                    foreignRef.setLocalizeReplace(selectedService.getGoid());
                    break;

                default:
                    throw new RuntimeException("Unexpected gateway metrics reference type.");
            }

        } else if (removeRadioButton.isSelected()) {
            foreignRef.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            foreignRef.setLocalizeIgnore();
        }
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        String typeVal = getGatewayReferenceTypeString();
        panelTitle.setText("Policy contains assertions that refer to an unknown " + typeVal);
        detailsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Missing " + typeVal + " details"));
        changeRadioButton.setText("Change assertions to use this " + typeVal + ":");
        removeRadioButton.setText("Remove assertions that refer to the missing " + typeVal);

        nameTextField.setEnabled(false);
        nameTextField.setText(foreignRef.getName());
        removeRadioButton.setSelected(true);
        changeComboBox.setEnabled(false);

        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeComboBox.setEnabled(true);
            }
        });

        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeComboBox.setEnabled(false);
            }
        });

        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeComboBox.setEnabled(false);
            }
        });

        populateChangeComboBox();
    }

    private String getGatewayReferenceTypeString () {
        String result = null;
        switch (foreignRef.getType()) {
            case CLUSTER_NODE:
                result = "cluster node";
                break;

            case PUBLISHED_SERVICE:
                result = "published service";
                break;

            default:
                throw new RuntimeException("Unexpected gateway metrics reference type.");
        }
        return result;
    }

    private void populateChangeComboBox() {
        switch (foreignRef.getType()) {
            case CLUSTER_NODE:
                populateClusterNodeComboBox();
                break;

            case PUBLISHED_SERVICE:
                populatePublishedServiceComboBox();
                break;

            default:
                throw new RuntimeException("Unexpected gateway metrics reference type.");
        }
    }

    private void populateClusterNodeComboBox() {
        ClusterNodeInfo[] clusterNodes;

        try {
            clusterNodes = Registry.getDefault().getClusterStatusAdmin().getClusterStatus();
        } catch (FindException e) {
            throw new RuntimeException(e);
        }

        ArrayList<ClusterNodeInfo> sortedClusterNodes = new ArrayList<ClusterNodeInfo>(clusterNodes.length + 1);
        sortedClusterNodes.add(ALL_NODES);

        for (ClusterNodeInfo currentCurrentClusterNode : clusterNodes) {
            sortedClusterNodes.add(currentCurrentClusterNode);
        }

        Collections.sort(sortedClusterNodes, new Comparator<ClusterNodeInfo>() {
            @Override
            public int compare(ClusterNodeInfo o1, ClusterNodeInfo o2) {
                return o1.compareTo(o2);
            }
        });

        changeComboBox.setModel(Utilities.comboBoxModel(sortedClusterNodes));
    }

    private void populatePublishedServiceComboBox() {
        ServiceHeader[] services;

        try {
            services = Registry.getDefault().getServiceManager().findAllPublishedServices();
        } catch (FindException e) {
            throw new RuntimeException(e);
        }

        ArrayList<ServiceHeader> sortedServices = new ArrayList<ServiceHeader>(services.length + 1);
        sortedServices.add(ALL_SERVICES);

        for (ServiceHeader currentService : services) {
            sortedServices.add(currentService);
        }

        Collections.sort(sortedServices, new Comparator<ServiceHeader>() {
            @Override
            public int compare(ServiceHeader o1, ServiceHeader o2) {
                return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
            }
        });

        changeComboBox.setModel(Utilities.comboBoxModel(sortedServices));
    }
}