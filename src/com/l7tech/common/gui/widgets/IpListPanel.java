/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import com.l7tech.common.gui.util.Utilities;

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

    public IpListPanel() {
        init();
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

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                Container rootPane = IpListPanel.this.getTopLevelAncestor();
                GetIpDialog dlg;
                if (rootPane instanceof Frame)
                    dlg = new GetIpDialog((Frame)rootPane);
                else if (rootPane instanceof Dialog)
                    dlg = new GetIpDialog((Dialog)rootPane);
                else
                    dlg = new GetIpDialog((Dialog)null);

                dlg.pack();
                Utilities.centerOnScreen(dlg);
                dlg.show();
                String addr = dlg.getAddress();
                dlg.dispose();
                if (addr != null) {
                    java.util.List addrList = getAddressesList();
                    addrList.add(addr);
                    setAddresses((String[])addrList.toArray(new String[0]));
                }
            }
        });

        Utilities.enableGrayOnDisabled(ipList);
        Utilities.enableGrayOnDisabled(addButton);
        Utilities.enableGrayOnDisabled(removeButton);
        updateEnableState();
    }

    /** Check for any widgets that should be enabled or disabled after a GUI state change. */
    private void updateEnableState() {
        boolean ips = rbSpecify.isSelected();
        ipList.setEnabled(ips);
        addButton.setEnabled(ips);
        removeButton.setEnabled(ips && ipList.getSelectedValue() != null);
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

    /** @param enableAddresses true if the "Specify IP addresses" radio buttons should be selected; otherwise, false. */
    public void setAddressesEnabled(boolean enableAddresses) {
        rbSpecify.setSelected(enableAddresses);
        rbLookupInDns.setSelected(!enableAddresses);
        updateEnableState();
    }

}
