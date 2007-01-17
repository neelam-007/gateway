/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.gui.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.common.gui.widgets.IpListPanel;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.common.gui.MaxLengthDocument;
import com.l7tech.common.gui.FilterDocument;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.proxy.datamodel.Ssg;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.util.regex.Pattern;

/**
 * The Network panel for the SSG Property Dialog.
 *
 * @author mike
 * @noinspection UnnecessaryUnboxing
 */
class SsgNetworkPanel extends JPanel {
    private static final Ssg referenceSsg = new Ssg(); // SSG bean with default values for all
    private final int ENDPOINT_MAX = SyspropUtil.getInteger(getClass().getName() + ".endpointMax", 400).intValue();
    private final Pattern ENDPOINT_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final InputValidator validator;
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
    private JPanel outgoingRequestsPanel;
    private JTextField customLabelField;
    private JCheckBox customLabelCb;
    private IpListPanel ipList;
    private boolean enableOutgoingRequestsPanel;

    private String endpointBase;
    private String defaultEndpoint;
    private String initialEndpoint;
    private String wsdlEndpointSuffix;

    public SsgNetworkPanel(InputValidator validator, boolean enableOutgoingRequestsPanel) {
        this.validator = validator;
        this.enableOutgoingRequestsPanel = enableOutgoingRequestsPanel;
        initialize(enableOutgoingRequestsPanel);
    }

    private void initialize(boolean enableOutgoingRequestsPanel) {
        setLayout(new BorderLayout());
        add(networkPane);

        networkPane.setBorder(BorderFactory.createEmptyBorder());

        // Endpoint panel

        WrappingLabel splain01 = new WrappingLabel("The SecureSpan Bridge listens for incoming messages at the " +
                                                   "following local proxy URL, then routes the messages to the " +
                                                   "SecureSpan Gateway:", 2);
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
                                              GridConstraints.ANCHOR_WEST,
                                              GridConstraints.FILL_HORIZONTAL,
                                              GridConstraints.SIZEPOLICY_CAN_GROW,
                                              GridConstraints.SIZEPOLICY_FIXED,
                                              null, null, null));

        ActionListener labelActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean custom = customLabelCb.isSelected();
                customLabelField.setEnabled(custom);
                customLabelField.setEditable(custom);
                if (custom) {
                    if (customLabelField.getText().trim().length() < 1) {
                        customLabelField.setText(defaultEndpoint);
                        customLabelField.selectAll();
                    }
                    customLabelField.requestFocusInWindow();
                }
                fieldLocalEndpoint.setText(getLocalEndpointUrl());
                fieldWsdlEndpoint.setText(getLocalEndpointUrl() + wsdlEndpointSuffix);
            }
        };
        customLabelCb.addActionListener(labelActionListener);
        Utilities.enableGrayOnDisabled(customLabelField);
        Utilities.attachDefaultContextMenu(customLabelField);
        customLabelField.setDocument(new FilterDocument(ENDPOINT_MAX, new FilterDocument.Filter() {
            public boolean accept(String s) {
                return ENDPOINT_PATTERN.matcher(s).matches();
            }
        }));
        validator.constrainTextFieldToBeNonEmpty("Custom label", customLabelField, new InputValidator.ValidationRule() {
            public String getValidationError() {
                String text = customLabelField.getText();
                int len = text.length();
                String ret = len <= ENDPOINT_MAX ? null : "Custom label field must be " + ENDPOINT_MAX + " characters or fewer.";
                if (ret == null)
                    ret = ENDPOINT_PATTERN.matcher(text).matches()
                          ? null 
                          : "Custom label may contain only letters, numbers, or underscore.";
                return ret;
            }
        });
        customLabelField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ')
                    e.setKeyChar('_');
                super.keyTyped(e);
            }
        });
        customLabelField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            public void run() {
                if (customLabelCb.isSelected()) {
                    fieldLocalEndpoint.setText(getLocalEndpointUrl());
                    fieldWsdlEndpoint.setText(getLocalEndpointUrl() + wsdlEndpointSuffix);
                }
            }
        }));

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
        validator.constrainTextFieldToNumberRange("Gateway HTTP port", fieldSsgPort, 1, 65535);
        fieldSsgPort.setPreferredSize(new Dimension(50, 20));
        ssgPortFieldPanel.add(fieldSsgPort, gc);

        fieldSslPort = new ContextMenuTextField("");
        Utilities.enableGrayOnDisabled(fieldSslPort);
        validator.constrainTextFieldToNumberRange("Gateway HTTPS port", fieldSslPort, 1, 65535);
        fieldSslPort.setPreferredSize(new Dimension(50, 20));
        sslPortFieldPanel.add(fieldSslPort, gc);

        ipList = new IpListPanel();
        ipListPanel.removeAll();
        ipListPanel.setLayout(new BorderLayout());
        ipListPanel.add(ipList, BorderLayout.CENTER);

        if (!enableOutgoingRequestsPanel) {
            sslPortFieldPanel.setEnabled(false);
            ssgPortFieldPanel.setEnabled(false);
            radioNonstandardPorts.setEnabled(false);
            radioNonstandardPorts.setSelected(false);
            radioStandardPorts.setEnabled(false);
            radioStandardPorts.setSelected(false);
        }

        updateCustomPortsEnableState();
    }

    String getLocalEndpoint() {
        return customLabelCb.isSelected() ? HexUtils.urlEncode(customLabelField.getText()) : defaultEndpoint;
    }

    private String getLocalEndpointUrl() {
        return endpointBase + getLocalEndpoint();
    }

    private void checkCustom() {
        if (initialEndpoint == null || endpointBase == null || defaultEndpoint == null)
            return;
        boolean custom = !initialEndpoint.equals(defaultEndpoint);
        customLabelCb.setSelected(custom);
        customLabelField.setEnabled(custom);
        customLabelField.setEditable(custom);
        if (custom)
            customLabelField.setText(initialEndpoint);
    }

    boolean isCustomLabel() {
        return customLabelCb.isSelected();
    }

    String getCustomLabel() {
        return customLabelField.getText();
    }

    void updateCustomPortsEnableState() {
        boolean en = enableOutgoingRequestsPanel && radioNonstandardPorts.isSelected();
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

    void setCurrentLocalEndpoint(String endpointUrl) {
        this.initialEndpoint = endpointUrl;
        fieldLocalEndpoint.setText(endpointBase + endpointUrl);
        checkCustom();
    }

    void setWsdlEndpointSuffix(String s) {
        this.wsdlEndpointSuffix = s;
        fieldWsdlEndpoint.setText(getLocalEndpointUrl() + s);
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

    public void setLocalEndpointBase(String endpointBase) {
        this.endpointBase = endpointBase;
        checkCustom();
    }

    public void setDefaultLocalEndpoint(String defaultEndpoint) {
        this.defaultEndpoint = defaultEndpoint;
        checkCustom();
    }
}
