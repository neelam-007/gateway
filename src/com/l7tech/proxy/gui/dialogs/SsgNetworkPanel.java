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
import com.l7tech.common.gui.widgets.SquigglyTextField;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.proxy.datamodel.Ssg;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private JRadioButton radioDefaultIpAddresses;
    private JRadioButton radioCustomIpAddresses;
    private JList jlistCustomIpAddresses;
    private JButton buttonRemoveIpAddress;
    private JButton buttonAddIpAddress;
    private JPanel wsdlUrlLabelPanel;
    private JPanel proxyUrlLabelPanel;
    private JPanel wsdlUrlPanel;
    private JPanel proxyUrlPanel;
    private JLabel defaultSsgPortLabel;
    private JLabel defaultSslPortLabel;
    private JPanel sslPortFieldPanel;
    private JPanel ssgPortFieldPanel;

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

        ButtonGroup ipBg = new ButtonGroup();
        radioDefaultIpAddresses.addChangeListener(changeListener);
        ipBg.add(radioDefaultIpAddresses);
        radioCustomIpAddresses.addChangeListener(changeListener);
        ipBg.add(radioCustomIpAddresses);

        jlistCustomIpAddresses.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateCustomPortsEnableState();
            }
        });

        buttonRemoveIpAddress.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object val = jlistCustomIpAddresses.getSelectedValue();
                if (val != null) {
                    java.util.List addrList = getCustomIpAddressesList();
                    addrList.remove(val);
                    setCustomIpAddresses((String[])addrList.toArray(new String[0]));
                }
            }
        });

        buttonAddIpAddress.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                Container rootPane = SsgNetworkPanel.this.getTopLevelAncestor();
                AddIpDialog dlg;
                if (rootPane instanceof Frame)
                    dlg = new AddIpDialog((Frame)rootPane);
                else if (rootPane instanceof Dialog)
                    dlg = new AddIpDialog((Dialog)rootPane);
                else
                    dlg = new AddIpDialog((Dialog)null);

                dlg.pack();
                Utilities.centerOnScreen(dlg);
                dlg.show();
                String addr = dlg.getAddress();
                dlg.dispose();
                if (addr != null) {
                    java.util.List addrList = getCustomIpAddressesList();
                    addrList.add(addr);
                    setCustomIpAddresses((String[])addrList.toArray(new String[0]));
                }
            }
        });

        Utilities.enableGrayOnDisabled(jlistCustomIpAddresses);
        Utilities.enableGrayOnDisabled(buttonAddIpAddress);
        Utilities.enableGrayOnDisabled(buttonRemoveIpAddress);

        updateCustomPortsEnableState();
    }

    void updateCustomPortsEnableState() {
        boolean en = radioNonstandardPorts.isSelected();
        fieldSsgPort.setEnabled(en);
        fieldSslPort.setEnabled(en);

        boolean ips = radioCustomIpAddresses.isSelected();
        jlistCustomIpAddresses.setEnabled(ips);
        buttonAddIpAddress.setEnabled(ips);
        buttonRemoveIpAddress.setEnabled(ips && jlistCustomIpAddresses.getSelectedValue() != null);
    }


    void setCustomIpAddresses(String[] ips) {
        if (ips == null) ips = new String[0];
        jlistCustomIpAddresses.setListData(ips);
        updateCustomPortsEnableState();
    }

    String[] getCustomIpAddresses() {
        java.util.List got = getCustomIpAddressesList();
        return (String[])got.toArray(new String[0]);
    }

    private java.util.List getCustomIpAddressesList() {
        ListModel model = jlistCustomIpAddresses.getModel();
        java.util.List got = new ArrayList();
        int size = model.getSize();
        for (int i = 0; i < size; ++i) {
            Object element = model.getElementAt(i);
            if (element != null)
                got.add(element.toString());
        }
        return got;
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
        return radioCustomIpAddresses.isSelected();
    }

    public void setUseOverrideIpAddresses(boolean useOverrideIpAddresses) {
        radioCustomIpAddresses.setSelected(useOverrideIpAddresses);
        radioDefaultIpAddresses.setSelected(!useOverrideIpAddresses);
        updateCustomPortsEnableState();
    }

    private static class AddIpDialog extends JDialog {
        private static final String ADD_IP = "Add IP Address";
        private String retval = null;
        private SquigglyTextField ipRangeTextField = null;
        private JButton okButton;

        AddIpDialog(Frame owner) {
            super(owner, ADD_IP, true);
            init();
        }

        AddIpDialog(Dialog owner) {
            super(owner, ADD_IP, true);
            init();
        }

        /** @return the address that was entered, if Ok button was pressed; else null. */
        String getAddress() {
            return retval;
        }

        private void init() {
            Container p = getContentPane();
            p.setLayout(new GridBagLayout());
            p.add(new JLabel("IP Address: "),
                  new GridBagConstraints(0, 0, 3, 1, 1.0, 1.0,
                                         GridBagConstraints.NORTHWEST,
                                         GridBagConstraints.BOTH,
                                         new Insets(5, 5, 5, 5), 0, 0));
            p.add(getIpRangeTextField(),
                  new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0,
                                         GridBagConstraints.NORTHWEST,
                                         GridBagConstraints.BOTH,
                                         new Insets(0, 5, 0, 5), 0, 0));

            okButton = new JButton("Ok");
            JButton cancelButton = new JButton("Cancel");
            Action buttonAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == okButton) {
                        String s = getIpRangeTextField().getText();
                        if (isValidIp(s))
                            retval = s;
                    }
                    AddIpDialog.this.hide();
                }
            };
            okButton.addActionListener(buttonAction);
            cancelButton.addActionListener(buttonAction);
            p.add(Box.createGlue(),
                  new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(0, 20, 0, 0), 0, 0));
            Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });
            p.add(okButton,
                  new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(5, 0, 5, 0), 0, 0));
            p.add(cancelButton,
                  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(5, 5, 5, 5), 0, 0));
            Utilities.runActionOnEscapeKey(getIpRangeTextField(), buttonAction);
            getRootPane().setDefaultButton(okButton);
            pack();
            checkValid();

        }

        private static final Pattern ipPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)");
        private static final Pattern badIpChars = Pattern.compile("[^0-9.]");
        private boolean isValidIp(String s) {
            boolean ret = false;
            int pos = -1;
            int end = -1;
            try {
                Matcher matcher = ipPattern.matcher(s);
                if (!matcher.matches())
                    return false;

                for (int i = 0; i < 4; ++i) {
                    final String matched = matcher.group(i + 1);
                    pos = matcher.start(i + 1);
                    end = matcher.end(i + 1);
                    int c = Integer.parseInt(matched);
                    if (c < 0 || c > 255)
                        return false;
                }

                getIpRangeTextField().setNone();
                ret = true;
                return true;
            } catch (NumberFormatException nfe) {
                return false;
            } finally {
                if (ret) {
                    getIpRangeTextField().setNone();
                } else {
                    if (pos < 0)
                        getIpRangeTextField().setAll();
                    else
                        getIpRangeTextField().setRange(pos, end);
                }
            }
        }

        private void checkValid() {
            okButton.setEnabled(isValidIp(getIpRangeTextField().getText()));
        }

        private SquigglyTextField getIpRangeTextField() {
            if (ipRangeTextField == null) {
                ipRangeTextField = new SquigglyTextField();
                // Block bad inserts immediately
                ipRangeTextField.getDocument().addUndoableEditListener(new UndoableEditListener() {
                    public void undoableEditHappened(UndoableEditEvent e) {
                        if (badIpChars.matcher(ipRangeTextField.getText()).find())
                            e.getEdit().undo();
                        checkValid();
                    }
                });
            }
            return ipRangeTextField;
        }
    }
}
