package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.MutablePair;
import org.apache.commons.lang.math.NumberUtils;

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
    private static final String READ_TIMEOUT_MILLIS = "readTimeoutMillis";
    private static final String KEEP_ALIVE = "keepAlive";
    private static final String RECEIVE_BUFFER_SIZE = "receiveBufferSize";
    private static final String REUSE_ADDRESS = "reuseAddress";
    private static final String SEND_BUFFER_SIZE = "sendBufferSize";
    private static final String SO_LINGER = "soLinger";
    private static final String TCP_NO_DELAY = "tcpNoDelay";
    // Query options
    private static final String QUERY_FETCH_SIZE = "fetchSize";
    private static final String MAX_RECORDS = "maxRecords";

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
        inputValidator.constrainTextFieldToBeNonEmpty("Value", propValueTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String propName = (String) propNameComboBox.getSelectedItem();
                if(QUERY_FETCH_SIZE.equals(propName)) {
                    return getNumberValidationErrorString(propName, Integer.MAX_VALUE);
                }
                else if(MAX_RECORDS.equals(propName)) {
                    return getNumberValidationErrorString(propName, 10000);
                }

                return null;
            }
        });

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
                        JOptionPane.showMessageDialog(CassandraPropertiesDialog.this, "Unable to retrieve defaults for property " + propName, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
//        ((JTextField)propNameComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(propNameListener);
        propNameComboBox.addItemListener(propNameListener);
    }

    private String getNumberValidationErrorString(String propName, int maxVal) {
        if(!NumberUtils.isNumber(propValueTextField.getText()) || (NumberUtils.toInt(propValueTextField.getText()) < 1 || NumberUtils.toInt(propValueTextField.getText()) > maxVal)) {
            return propName + " value must be the number between 1 and " + Integer.toString(maxVal);
        }
        return null;
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
                MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD, MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD, CONNECTION_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS,
                KEEP_ALIVE, RECEIVE_BUFFER_SIZE, REUSE_ADDRESS, SEND_BUFFER_SIZE, SO_LINGER, TCP_NO_DELAY,MAX_RECORDS, QUERY_FETCH_SIZE
        }));
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
