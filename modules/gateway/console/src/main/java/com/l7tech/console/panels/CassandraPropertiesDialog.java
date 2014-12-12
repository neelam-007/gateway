package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.MutablePair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CassandraPropertiesDialog extends JDialog {
    // Connection pooling options
    public static final String HOST_DISTANCE = "hostDistance";
    private static final String CORE_CONNECTION_PER_HOST = "coreConnectionsPerHost";
    private static final String MAX_CONNECTION_PER_HOST = "maxConnectionPerHost";
    private static final String MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD = "maxSimultaneousRequestsPerConnectionThreshold";
    private static final String MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD = "minSimultaneousRequestsPerConnectionThreshold";
    // Socket options
    private static final String CONNECTION_TIMEOUT_MILLIS = "connectTimeoutMillis";
    private static final String KEEP_ALIVE = "keepAlive";
    private static final String RECEIVE_BUFFER_SIZE = "receiveBufferSize";
    private static final String REUSE_ADDRESS = "reuseAddress";
    private static final String SEND_BUFFER_SIZE = "sendBufferSize";
    private static final String SO_LINGER = "soLinger";
    private static final String TCP_NO_DELAY = "tcpNoDelay";
    // Query options
    private static final String QUERY_FETCH_SIZE = "maxRecords";

    private JPanel mainPanel;
    private JTextField propValueTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox propNameComboBox;

    private MutablePair<String, String> property;
    private InputValidator inputValidator;
    private boolean confirmed;

    public CassandraPropertiesDialog(Dialog owner, MutablePair<String, String> property) {
        super(owner, "Additional Properties");
        initialize(property);
        modelToView();
    }

    private void initialize(MutablePair<String, String> property) {
        this.property = property;

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);

        inputValidator = new InputValidator(this, this.getTitle());
        inputValidator.constrainTextFieldToBeNonEmpty("Value", propValueTextField, null);

        populatePropNameComboBox();

        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                viewToModel();
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        final RunOnChangeListener propNameListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                String propName = (String) propNameComboBox.getSelectedItem();
                if (propName != null) {
                    ClusterStatusAdmin clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
                    try {
                        ClusterProperty clusterProperty = clusterStatusAdmin.findPropertyByName("cassandra." + propName);
                        if(clusterProperty != null && clusterProperty.getValue() != null) {
                            propValueTextField.setText(clusterProperty.getValue());
                        }
                    } catch (FindException fe) {
                        JOptionPane.showMessageDialog(CassandraPropertiesDialog.this, "Unable to retrieve defaults for property " + propName, "Error", JOptionPane.ERROR);
                    }
                }
            }
        });
//        ((JTextField)propNameComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(propNameListener);
        propNameComboBox.addItemListener(propNameListener);
    }

    private void modelToView() {
        if (property == null || property.left == null || property.right == null) {
            throw new IllegalStateException("An additional property object must be initialized first.");
        }
        propNameComboBox.setSelectedItem(property.left);
        propValueTextField.setText(property.right);
    }

    private void viewToModel() {
        if (property == null || property.left == null || property.right == null) {
            throw new IllegalStateException("An additional property object must be initialized first.");
        }
        property.left = (String) propNameComboBox.getSelectedItem();
        property.right = propValueTextField.getText();
    }

    @SuppressWarnings("unchecked")
    private void populatePropNameComboBox() {
        propNameComboBox.setModel(new DefaultComboBoxModel(new String[]{
                HOST_DISTANCE, CORE_CONNECTION_PER_HOST, MAX_CONNECTION_PER_HOST,
                MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD, MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD, CONNECTION_TIMEOUT_MILLIS,
                KEEP_ALIVE, RECEIVE_BUFFER_SIZE, REUSE_ADDRESS, SEND_BUFFER_SIZE, SO_LINGER, TCP_NO_DELAY,QUERY_FETCH_SIZE
        }));
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
