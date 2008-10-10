/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.client.gui.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.ContextMenuTextField;
import com.l7tech.gui.widgets.IpListPanel;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.widgets.WrappingLabel;
import com.l7tech.proxy.Constants;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
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
    private JLabel configureNonstandardPortsLabel;
    private JLabel defaultSsgPortLabel;
    private JLabel defaultSslPortLabel;
    private JLabel defaultSsgPortLabelLabel;
    private JLabel defaultSslPortLabelLabel;
    private JPanel sslPortFieldPanel;
    private JPanel ssgPortFieldPanel;
    private JPanel ipListPanel;
    private JPanel outgoingRequestsPanel;
    private JTextField customLabelField;
    private JCheckBox customLabelCb;
    private JCheckBox gzipCheckBox;
    private JLabel ssgPortFieldLabel;
    private JLabel sslPortFieldLabel;
    private JPanel configureNonstandardPortsSpacerPanel;
    private JLabel wsdlUrlLabel;
    private JPanel wsdlUrlSpacerPanel;
    private JLabel configureIpAddressesLabel;
    private IpListPanel ipList;

    private boolean allowCustomPorts;
    private boolean allowWsdlProxy;

    private String endpointBase;
    private String defaultEndpoint;
    private String initialEndpoint;
    private String wsdlEndpointSuffix;

    private final Ssg ssg;
    private final SsgFinder ssgFinder;

    public SsgNetworkPanel(InputValidator validator, Ssg ssg, SsgFinder ssgFinder, boolean allowWsdlProxy, boolean allowCustomPorts) {
        this.validator = validator;
        this.ssg = ssg;
        this.ssgFinder = ssgFinder;
        this.allowWsdlProxy = allowWsdlProxy;
        this.allowCustomPorts = allowCustomPorts;
        initialize();
    }

    private String serverType() {
        return ssg.isGeneric() ? "Web service" : "SecureSpan Gateway";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(networkPane);

        networkPane.setBorder(BorderFactory.createEmptyBorder());

        // Endpoint panel

        JLabel splain01 = new JLabel("<HTML>The SecureSpan "+ Constants.APP_NAME +" listens for incoming messages at the " +
                                                   "following<br/>local proxy URL, then routes the messages to the " +
                                                   serverType() + ':');
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
        validator.constrainTextFieldToBeNonEmpty("Custom label", customLabelField, new InputValidator.ComponentValidationRule(customLabelField) {
            public String getValidationError() {
                String text = customLabelField.getText();
                int len = text.length();
                String ret = len <= ENDPOINT_MAX ? null : "Custom label field must be " + ENDPOINT_MAX + " characters or fewer.";
                if (ret == null)
                    ret = ENDPOINT_PATTERN.matcher(text).matches()
                          ? null
                          : "Custom label may contain only letters, numbers, or underscore.";

                if (ret == null) {
                    // Check for duplicate label
                    ret = checkForDuplicateLabel(text);
                }

                return ret;
            }
        });

        validator.addRule(new InputValidator.ComponentValidationRule(customLabelCb) {
            public String getValidationError() {
                String ret = checkForDuplicateLabel(defaultEndpoint);
                if (ret == null) {
                    // No problem
                    return null;
                }
                if (customLabelCb.isSelected()) {
                    // Let the custom label validator deal with it
                    return null;
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (!customLabelCb.isSelected()) {
                            customLabelCb.setSelected(true);
                            customLabelField.setEnabled(true);
                            customLabelField.setEditable(true);
                            if (customLabelField.getText().trim().length() < 1) {
                                customLabelField.setText(defaultEndpoint);
                                customLabelField.selectAll();
                            }
                            fieldLocalEndpoint.setText(getLocalEndpointUrl());
                            fieldWsdlEndpoint.setText(getLocalEndpointUrl() + wsdlEndpointSuffix);
                        }
                    }
                });

                return ret + "\nPlease enter a unique label.";
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

        JLabel splain02 = new JLabel("<HTML>The SecureSpan "+ Constants.APP_NAME +" offers proxied WSDL lookups at the " +
                                                   "following<br/>local WSDL URL:");
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

        if (!allowWsdlProxy) {
            hideDeselectAndDisable(wsdlUrlLabel,
                                   wsdlUrlLabelPanel,
                                   wsdlUrlPanel,
                                   wsdlUrlSpacerPanel);
        }

        if (!allowCustomPorts) {
            hideDeselectAndDisable(configureNonstandardPortsLabel,
                                   configureNonstandardPortsSpacerPanel,
                                   sslPortFieldLabel,
                                   sslPortFieldPanel,
                                   ssgPortFieldPanel,
                                   ssgPortFieldLabel,
                                   radioNonstandardPorts,
                                   radioStandardPorts,
                                   defaultSsgPortLabel,
                                   defaultSsgPortLabelLabel,
                                   defaultSslPortLabel,
                                   defaultSslPortLabelLabel);
        }

        if (ssg.isGeneric()) {
            JLabel[] labels = new JLabel[]{configureIpAddressesLabel, configureNonstandardPortsLabel};
            for (JLabel lab : labels)
                lab.setText(lab.getText().replaceAll("SecureSpan Gateway", "Web service"));
        }


        updateCustomPortsEnableState();
    }

    private String checkForDuplicateLabel(String text) {
        if (text == null) return null;
        Collection<Ssg> ssgs = ssgFinder.getSsgList();
        for (Ssg thatSsg : ssgs) {
            if ((!ssg.equals(thatSsg)) && text.equals(thatSsg.getLocalEndpoint())) {
                return "Label is duplicate of label for Gateway Account \"" + thatSsg + "\"";
            }
        }
        return null;
    }

    private static void hideDeselectAndDisable(JComponent... comps) {
        for (JComponent comp : comps) {
            comp.setVisible(false);
            comp.setEnabled(false);
            if (comp instanceof AbstractButton)
                ((AbstractButton)comp).setSelected(false);
        }
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
        boolean en = allowCustomPorts && radioNonstandardPorts.isSelected();
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

    private void createUIComponents() {
        // InputValidator will show feedback if a ComponentValidationRule's component implements ModelessFeedback
        customLabelField = new SquigglyTextField();
    }

    public void setCompression(boolean compress) {
        gzipCheckBox.setSelected(compress);
    }

    public boolean getCompression() {
        return gzipCheckBox.isSelected();
    }
}
