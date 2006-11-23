package com.l7tech.server.config.ui.gui;

import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.common.gui.NumberField;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class EditEndpointDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox ipAddress;
    private JTextField portNumber;
    private JCheckBox isSecure;
    private JCheckBox isClientCert;
    private PartitionInformation.EndpointHolder endpointHolder;

    public EditEndpointDialog(Frame parent, PartitionInformation.EndpointHolder holder) {
        super(parent, true);
        setup(holder);
    }

    public EditEndpointDialog(Dialog parent, PartitionInformation.EndpointHolder holder) {
        super(parent, true);
        setup(holder);
    }

    public PartitionInformation.EndpointHolder getEndpoint() {
        return endpointHolder;
    }

    private void setup(PartitionInformation.EndpointHolder holder) {
        setTitle(holder == null?"Add New Endpoint":"Edit Endpoint");
        endpointHolder = holder;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        initListeners();

        initComponents();
        pack();
    }

    private void initComponents() {
        portNumber.setDocument(new NumberField(String.valueOf(Short.MAX_VALUE).length()));
        if (endpointHolder == null) {
            //set up the controls for a new endpoint
            isSecure.setSelected(true);
            ipAddress.setModel(getAvailableIpAddresses());
        } else {
            //set up the controls for an existing endpoint
        }
    }

    private ComboBoxModel getAvailableIpAddresses() {
        String localHostName;
        java.util.List<String> allIpAddresses = new ArrayList<String>();
        try {
            localHostName = InetAddress.getLocalHost().getCanonicalHostName();
            InetAddress[] localAddresses = InetAddress.getAllByName(localHostName);
            for (InetAddress localAddress : localAddresses) {
                allIpAddresses.add(localAddress.getHostAddress());
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not determine the network interfaces for this gateway. Please run the system configuration wizard");
        }

        return new DefaultComboBoxModel(allIpAddresses.toArray(new String[0]));
    }

    private void initListeners() {
        isSecure.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                isClientCert.setEnabled(isSecure.isSelected());
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(endpointHolder == null) endpointHolder = new PartitionInformation.EndpointHolder();

                endpointHolder.ipAddress = (String) ipAddress.getSelectedItem();
                endpointHolder.port = portNumber.getText();
                endpointHolder.isSecure = isSecure.isSelected();
                endpointHolder.isClientCert = isClientCert.isSelected();
                dispose();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
    // add your code here
        dispose();
    }

    private void onCancel() {
    // add your code here if necessary
        dispose();
    }

//    private class EndpointModel extends AbstractTableModel {
//        String[] columnNames = new String[] {
//            "IP Address",
//            "Port",
//            "Secure",
//            "With Client Certificate"
//        };
//
//        java.util.List<PartitionInformation.EndpointHolder> endpoints = new ArrayList<PartitionInformation.EndpointHolder>();
//
//        public int getRowCount() {
//            return endpoints.size();
//        }
//
//        public int getColumnCount() {
//            return columnNames.length;
//        }
//
//        public Object getValueAt(int rowIndex, int columnIndex) {
//            if (rowIndex < 0 || rowIndex > endpoints.size()) return null;
//
//            PartitionInformation.EndpointHolder holder = endpoints.get(rowIndex);
//            switch(columnIndex) {
//                case 0:
//                    return holder.ipAddress;
//                case 1:
//                    return holder.port;
//                case 2:
//                    return holder.isSecure?"yes":"no";
//                case 4:
//                    if (holder.isSecure) return false;
//                    return holder.isClientCert?"true":"false";
//            }
//            return null;
//        }
//
//
//        public String getColumnName(int columnIndex) {
//            if (columnIndex < 0 || columnIndex > columnNames.length)
//                throw new RuntimeException("Unexpected Column Index [" + columnIndex + "] for endpoint table");
//            return columnNames[columnIndex];
//        }
//
//        public void addEndpoint(PartitionInformation.EndpointHolder endpoint) {
//            try {
//                if (!endpoints.contains(endpoint)) endpoints.add(endpoint);
//            } finally {
//                fireTableDataChanged();
//            }
//        }
//
//        public void removeEndpoint(PartitionInformation.EndpointHolder endpoint) {
//            try {
//                if (endpoints.contains(endpoint)) endpoints.remove(endpoint);
//            } finally {
//                fireTableDataChanged();
//            }
//        }
//    }
}
