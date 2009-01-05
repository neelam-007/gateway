/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gui.widgets;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * A widget for enabling and editing a custom IP address list.
 * This widget contains two radio buttons, a list of IP addresses, and an Add and Remove button.
 */
public class IpListPanel extends JPanel {
    private JPanel rootPanel;
    private JButton removeButton;
    private JButton addButton;
    private JList ipList;
    private JRadioButton rbSpecify;
    private JRadioButton rbLookupInDns;
    private JComboBox cbStrategy;
    private JRadioButton useDifferentURLsRadioButton;
    private JButton editButton;

    public IpListPanel() {
        init();
    }

    public void alsoEnableDiffURLS() {
        useDifferentURLsRadioButton.setVisible(true);
    }

    private void init() {
        this.removeAll();
        this.setLayout(new BorderLayout());
        this.add(rootPanel, BorderLayout.CENTER);

        final ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableState();
            }
        };

        ButtonGroup ipBg = new ButtonGroup();
        rbLookupInDns.addChangeListener(changeListener);
        ipBg.add(rbLookupInDns);
        rbSpecify.addChangeListener(changeListener);
        ipBg.add(rbSpecify);
        useDifferentURLsRadioButton.addChangeListener(changeListener);
        ipBg.add(useDifferentURLsRadioButton);
        useDifferentURLsRadioButton.setVisible(false);

        ipList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ipList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateEnableState();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object val = ipList.getSelectedValue();
                if (val != null) {
                    java.util.List addrList = getAddressesList();
                    addrList.remove(val);
                    setAddresses((String[])addrList.toArray(new String[0]));
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Container rootPane = SwingUtilities.getWindowAncestor(IpListPanel.this);
                final GetIpDialog dlg;
                if (rootPane instanceof Frame)
                    dlg = new GetIpDialog((Frame)rootPane);
                else if (rootPane instanceof Dialog)
                    dlg = new GetIpDialog((Dialog)rootPane);
                else
                    dlg = new GetIpDialog((Frame)null);

                //update 'Edit' title
                if (!rbSpecify.isSelected()) {
                    dlg.noValidateFormat();
                    dlg.setTitle("Edit URL");
                } else {
                    dlg.setTitle("Edit IP Address");
                }

                //populate with the selected value
                Object selectedVal = ipList.getSelectedValue();
                final int selectedIndex = ipList.getSelectedIndex();
                if (selectedVal != null) {
                    dlg.setTextField((String) selectedVal);
                }

                dlg.pack();
                Utilities.centerOnScreen(dlg);

                DialogDisplayer.display(dlg, rootPane, new Runnable() {
                    public void run() {
                        String addr = dlg.getAddress();
                        if (addr != null) {
                            java.util.List addrList = getAddressesList();
                            addrList.remove(selectedIndex);
                            addrList.add(selectedIndex, addr);
                            setAddresses((String[])addrList.toArray(new String[0]));
                        }
                    }
                });
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                Container rootPane = SwingUtilities.getWindowAncestor(IpListPanel.this);
                final GetIpDialog dlg;
                if (rootPane instanceof Frame)
                    dlg = new GetIpDialog((Frame)rootPane);
                else if (rootPane instanceof Dialog)
                    dlg = new GetIpDialog((Dialog)rootPane);
                else
                    dlg = new GetIpDialog((Frame)null);

                if (!rbSpecify.isSelected()) {
                    dlg.noValidateFormat();
                    dlg.setTitle("Add URL");
                }
                dlg.pack();
                Utilities.centerOnScreen(dlg);

                DialogDisplayer.display(dlg, rootPane, new Runnable() {
                    public void run() {
                        String addr = dlg.getAddress();
                        if (addr != null) {
                            java.util.List addrList = getAddressesList();
                            addrList.add(addr);
                            setAddresses((String[])addrList.toArray(new String[0]));
                        }
                    }
                });
            }
        });

        cbStrategy.setModel(new DefaultComboBoxModel(FailoverStrategyFactory.getFailoverStrategyNames()) {

        });

        Utilities.enableGrayOnDisabled(ipList);
        Utilities.enableGrayOnDisabled(addButton);
        Utilities.enableGrayOnDisabled(removeButton);
        Utilities.enableGrayOnDisabled(editButton);
        Utilities.enableGrayOnDisabled(cbStrategy);
        updateEnableState();
    }

    public static final int DNS = 0;
    public static final int CUSTOM_IPS = 1;
    public static final int CUSTOM_URLS = 2;
    public interface StateCallback {
        void stateChanged(int newState);
    }
    private StateCallback stateCallback;
    public void registerStateCallback (StateCallback callback) {
        this.stateCallback = callback;
    }
    private JRadioButton lastmode = null;

    /** Check for any widgets that should be enabled or disabled after a GUI state change. */
    private void updateEnableState() {
        boolean ips = rbSpecify.isSelected() || useDifferentURLsRadioButton.isSelected();
        JRadioButton newmode = null;
        if (useDifferentURLsRadioButton.isSelected()) newmode = useDifferentURLsRadioButton;
        if (rbSpecify.isSelected()) newmode = rbSpecify;
        if (newmode != null && newmode != lastmode) {
            // clear list if switch between incompatible modes
            ipList.setListData(new String[0]);
        }
        if (newmode != null) lastmode = newmode;
        ipList.setEnabled(ips);
        addButton.setEnabled(ips);
        cbStrategy.setEnabled(ips);
        removeButton.setEnabled(ips && ipList.getSelectedValue() != null);
        editButton.setEnabled(ips && ipList.getSelectedValue() != null);
        if (stateCallback != null) {
            if (rbSpecify.isSelected()) stateCallback.stateChanged(CUSTOM_IPS);
            else if (useDifferentURLsRadioButton.isSelected()) stateCallback.stateChanged(CUSTOM_URLS);
            else stateCallback.stateChanged(DNS);
        }
    }

    private java.util.List getAddressesList() {
        ListModel model = ipList.getModel();
        java.util.List got = new ArrayList();
        int size = model.getSize();
        for (int i = 0; i < size; ++i) {
            Object element = model.getElementAt(i);
            if (element != null)
                got.add(element.toString());
        }
        return got;
    }

    /** @return the current list of configured IP addresses. */
    public String[] getAddresses() {
        java.util.List got = getAddressesList();
        return (String[])got.toArray(new String[0]);
    }

    /** @param ips the new list of IP addresses. */
    public void setAddresses(String[] ips) {
        if (ips == null) ips = new String[0];
        ipList.setListData(ips);
        updateEnableState();
    }

    /** @return true if the "Specify IP addreses" radio button is selected; otherwise false. */
    public boolean isAddressesEnabled() {
        return rbSpecify.isSelected();
    }

    public boolean isURLsEnabled() {
        return useDifferentURLsRadioButton.isSelected();
    }

    /** @param enableAddresses true if the "Specify IP addresses" radio buttons should be selected; otherwise, false. */
    public void setAddressesEnabled(boolean enableAddresses) {
        useDifferentURLsRadioButton.setSelected(!enableAddresses);
        rbSpecify.setSelected(enableAddresses);
        rbLookupInDns.setSelected(!enableAddresses);
        updateEnableState();
    }

    public void setURLsEnabled(boolean enableAddresses) {
        useDifferentURLsRadioButton.setSelected(enableAddresses);
        rbSpecify.setSelected(!enableAddresses);
        rbLookupInDns.setSelected(!enableAddresses);
        updateEnableState();
    }

    public String getFailoverStrategyName() {
        return ((FailoverStrategy)cbStrategy.getSelectedItem()).getName();
    }

    public void setFailoverStrategyName(String name) {
        DefaultComboBoxModel m = (DefaultComboBoxModel)cbStrategy.getModel();
        for (int i = 0; i < m.getSize(); ++i) {
            FailoverStrategy strat = (FailoverStrategy)m.getElementAt(i);
            if (strat.getName().equalsIgnoreCase(name)) {
                cbStrategy.setSelectedIndex(i);
                break;
            }
        }
    }

}
