/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.l7tech.common.gui.NumberField;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.common.gui.widgets.IpListPanel;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.proxy.datamodel.Ssg;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * The Network panel for the SSG Property Dialog.
 *
 * @author mike
 */
class SsgNetworkPanel extends JPanel {
    private static final Ssg referenceSsg = new Ssg(); // SSG bean with default values for all

    private JPanel networkPane;
    private WrappingLabel fieldLocalEndpoint;
    private WrappingLabel fieldWsdlEndpoint;
    private JRadioButton radioStandardPorts;
    private JRadioButton radioNonstandardPorts;
    private JTextField fieldSsgPort;
    private JTextField fieldSslPort;
    private JPanel wsdlUrlLabelPanel;
    private JPanel proxyUrlLabelPanel;
    private JPanel wsdlUrlPanel;
    private JPanel proxyUrlPanel;
    private JLabel defaultSsgPortLabel;
    private JLabel defaultSslPortLabel;
    private JPanel sslPortFieldPanel;
    private JPanel ssgPortFieldPanel;
    private JPanel ipListPanel;
    private IpListPanel ipList;

    public SsgNetworkPanel() {
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(networkPane);

        networkPane.setBorder(BorderFactory.createEmptyBorder());

        // Endpoint panel

        WrappingLabel splain01 = new WrappingLabel("The SecureSpan Bridge listens for incoming messages at the " +
                                                   "following local proxy URL, then routes the messages to the " +
                                                   "SecureSpan Gateway.", 2);
        //x,y,rows,col,anchor,fill,sizex,sizey,minsize,prefsize,maxsize
        GridConstraints gc = new GridConstraints(0, 0, 1, 1,
                                                 GridConstraints.ANCHOR_NORTHWEST,
                                                 GridConstraints.FILL_HORIZONTAL,
                                                 GridConstraints.SIZEPOLICY_CAN_GROW,
                                                 GridConstraints.SIZEPOLICY_FIXED,
                                                 null, null, null);
        proxyUrlLabelPanel.add(splain01, gc);

        fieldLocalEndpoint = new WrappingLabel("");
        fieldLocalEndpoint.setContextMenuEnabled(true);
        proxyUrlPanel.add(fieldLocalEndpoint,
                          new GridConstraints(0, 0, 1, 1,
                                              GridConstraints.ANCHOR_NORTHWEST,
                                              GridConstraints.FILL_HORIZONTAL,
                                              GridConstraints.SIZEPOLICY_CAN_GROW,
                                              GridConstraints.SIZEPOLICY_FIXED,
                                              null, null, null));

        WrappingLabel splain02 = new WrappingLabel("The SecureSpan Bridge offers proxied WSDL lookups at the " +
                                                   "following local WSDL URL:", 1);
        wsdlUrlLabelPanel.add(splain02, gc);

        fieldWsdlEndpoint = new WrappingLabel("");
        fieldWsdlEndpoint.setContextMenuEnabled(true);
        wsdlUrlPanel.add(fieldWsdlEndpoint, gc);

        ButtonGroup bg = new ButtonGroup();
        final ChangeListener changeListener = new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        updateCustomPortsEnableState();
                    }
                };
        radioStandardPorts.addChangeListener(changeListener);
        bg.add(radioStandardPorts);

        defaultSsgPortLabel.setText(String.valueOf(referenceSsg.getSsgPort()));
        Utilities.enableGrayOnDisabled(defaultSsgPortLabel);
        defaultSslPortLabel.setText(String.valueOf(referenceSsg.getSslPort()));

        radioNonstandardPorts.addChangeListener(changeListener);
        bg.add(radioNonstandardPorts);

        fieldSsgPort = new ContextMenuTextField("");
        Utilities.enableGrayOnDisabled(fieldSsgPort);
        Utilities.constrainTextFieldToIntegerRange(fieldSsgPort, 1, 65535);
        fieldSsgPort.setDocument(new NumberField(6));
        fieldSsgPort.setPreferredSize(new Dimension(50, 20));
        ssgPortFieldPanel.add(fieldSsgPort, gc);

        fieldSslPort = new ContextMenuTextField("");
        Utilities.enableGrayOnDisabled(fieldSslPort);
        Utilities.constrainTextFieldToIntegerRange(fieldSslPort, 1, 65535);
        fieldSslPort.setDocument(new NumberField(6));
        fieldSslPort.setPreferredSize(new Dimension(50, 20));
        sslPortFieldPanel.add(fieldSslPort, gc);

        ipList = new IpListPanel();
        ipListPanel.removeAll();
        ipListPanel.setLayout(new BorderLayout());
        ipListPanel.add(ipList, BorderLayout.CENTER);

        updateCustomPortsEnableState();
    }

    void updateCustomPortsEnableState() {
        boolean en = radioNonstandardPorts.isSelected();
        fieldSsgPort.setEnabled(en);
        fieldSslPort.setEnabled(en);
    }


    void setCustomIpAddresses(String[] ips) {
        ipList.setAddresses(ips);
    }

    String[] getCustomIpAddresses() {
        return ipList.getAddresses();
    }
    void setCustomPorts(boolean customPorts) {
        radioStandardPorts.setSelected(!customPorts);
        radioNonstandardPorts.setSelected(customPorts);
    }

    void setSslPort(int sslPort) {
        fieldSslPort.setText(Integer.toString(sslPort));
    }

    void setSsgPort(int ssgPort) {
        fieldSsgPort.setText(Integer.toString(ssgPort));
    }

    void setLocalEndpoint(String endpointUrl) {
        fieldLocalEndpoint.setText(endpointUrl);
    }

    void setWsdlEndpoint(String s) {
        fieldWsdlEndpoint.setText(s);
    }

    boolean isCustomPorts() {
         return radioNonstandardPorts.isSelected();
    }

    public int getSsgPort() {
        return Integer.parseInt(fieldSsgPort.getText());
    }

    public int getSslPort() {
        return Integer.parseInt(fieldSslPort.getText());
    }

    public boolean isUseOverrideIpAddresses() {
        return ipList.isAddressesEnabled();
    }

    public void setUseOverrideIpAddresses(boolean useOverrideIpAddresses) {
        ipList.setAddressesEnabled(useOverrideIpAddresses);
    }

    public void setFailoverStrategyName(String name) {
        ipList.setFailoverStrategyName(name);
    }

    public String getFailoverStrategyName() {
        return ipList.getFailoverStrategyName();
    }
}
