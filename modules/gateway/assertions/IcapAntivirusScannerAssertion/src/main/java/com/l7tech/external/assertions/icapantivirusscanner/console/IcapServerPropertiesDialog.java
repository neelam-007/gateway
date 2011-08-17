package com.l7tech.external.assertions.icapantivirusscanner.console;

import com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ken Diep
 */
public final class IcapServerPropertiesDialog extends JDialog {

    private static final int NAME_COLUMN = 0;
    private static final int VALUE_COLUMN = 1;
    private static final int MAX_PORT = 65535;

    private static final String PARAMETER_NAME_HEADER = "Name";
    private static final String PARAMETER_VALUE_HEADER = "Value";

    private JPanel contentPane;

    private JTextField txtConnectionName;
    private JTextField txtHostname;
    private JTextField txtPort;
    private JTextField txtServiceName;

    private JButton btnAddParameter;
    private JButton btnRemoveButton;
    private JButton btnCancel;
    private JButton btnOk;

    private JLabel lbConnectionName;
    private JLabel lbHost;
    private JLabel lbPort;
    private JLabel lbServiceName;
    private JLabel lbParameters;

    private JTable parameters;
    private JLabel lbTimeout;
    private JTextField txtTimeout;
    private DefaultTableModel tableModel;

    private IcapConnectionDetail connectionDetail;

    public IcapServerPropertiesDialog(final Frame owner, final String title, final IcapConnectionDetail connectionDetail) {
        super(owner, title, true);
        this.connectionDetail = connectionDetail == null ? new IcapConnectionDetail() : connectionDetail;
        initComponents();
        toView();
    }

    private void initComponents() {

        tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(new String[]{PARAMETER_NAME_HEADER, PARAMETER_VALUE_HEADER});
        parameters.setModel(tableModel);
        parameters.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        parameters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        btnAddParameter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableModel.addRow(new Object[]{"", ""});
            }
        });

        btnRemoveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                int selected = parameters.getSelectedRow();
                if (selected >= 0) {
                    tableModel.removeRow(selected);
                }
            }
        });
        btnOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (toModel()) {
                    setVisible(false);
                }
            }
        });
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                setVisible(false);
            }
        });

        getRootPane().setDefaultButton(btnOk);
        setContentPane(createPropertyPanel());
    }

    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private boolean toModel() {
        final String connectionName = txtConnectionName.getText().trim();
        if (connectionName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a connection name.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        final String hostname = txtHostname.getText().trim();
        if (hostname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a host name.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        final String serviceName = txtServiceName.getText().trim();
        if (serviceName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a service name.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        final String portText = txtPort.getText().trim();
        if (portText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a port number.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            final int port = Integer.parseInt(portText);
            if (port < 1 || port > MAX_PORT) {
                JOptionPane.showMessageDialog(this, "Port number must be between 1 and 65535", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            connectionDetail.setPort(port);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number entered.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        final String timeoutText = txtTimeout.getText().trim();
        if (timeoutText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a timeout value.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            final int timeout = Integer.parseInt(timeoutText);
            if (timeout < 1) {
                JOptionPane.showMessageDialog(this, "Timeout value must be greater than 1", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            connectionDetail.setTimeout(timeout * 1000);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid timeout value entered.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        connectionDetail.setConnectionName(connectionName);
        connectionDetail.setHostname(hostname);
        connectionDetail.setServiceName(serviceName);
        final Map<String, String> serviceParams = new HashMap<String, String>();
        for (int i = 0; i < tableModel.getRowCount(); ++i) {
            String key = tableModel.getValueAt(i, NAME_COLUMN).toString();
            String value = tableModel.getValueAt(i, VALUE_COLUMN).toString();
            serviceParams.put(key, value);
        }
        connectionDetail.setServiceParameters(serviceParams);
        return true;
    }

    private void toView() {
        txtConnectionName.setText(connectionDetail.getConnectionName());
        txtHostname.setText(connectionDetail.getHostname());
        txtPort.setText(String.valueOf(connectionDetail.getPort()));
        txtServiceName.setText(connectionDetail.getServiceName());
        txtTimeout.setText(String.valueOf(connectionDetail.getTimeout() / 1000));

        for (Map.Entry<String, String> ent : connectionDetail.getServiceParameters().entrySet()) {
            tableModel.addRow(new String[]{ent.getKey(), ent.getValue()});
        }
    }

}
