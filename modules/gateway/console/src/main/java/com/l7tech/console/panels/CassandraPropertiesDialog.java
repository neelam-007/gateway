package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.MutablePair;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CassandraPropertiesDialog extends JDialog {
    private static final String[] hostDistance = {"LOCAL", "REMOTE", "IGNORED"};
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
    private JLabel propertyNameLabel;
    private JLabel propertyValueLabel;

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

        populatePropNameComboBox();

        inputValidator = new InputValidator(this, this.getTitle());

        inputValidator.ensureComboBoxSelection(getFieldLabelTrimmed(propertyNameLabel), propNameComboBox);

        inputValidator.constrainTextFieldToBeNonEmpty(getFieldLabelTrimmed(propertyValueLabel), propValueTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String propName = (String) propNameComboBox.getSelectedItem();
                switch(propName) {
                    case CORE_CONNECTION_PER_HOST:
                    case QUERY_FETCH_SIZE:
                        return getNumberValidationErrorString(propName, 1, Integer.MAX_VALUE);
                    case MAX_RECORDS:
                        return getNumberValidationErrorString(propName, 1, 10000);
                    case MAX_CONNECTION_PER_HOST:
                        return getNumberValidationErrorString(propName, 2, Integer.MAX_VALUE);
                    case MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD:
                    case MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD:
                    case CONNECTION_TIMEOUT_MILLIS:
                    case READ_TIMEOUT_MILLIS:
                    case RECEIVE_BUFFER_SIZE:
                    case SEND_BUFFER_SIZE:
                    case SO_LINGER:
                        return getNumberValidationErrorString(propName, 0, Integer.MAX_VALUE);
                    case HOST_DISTANCE:
                        return ArrayUtils.contains(hostDistance, propValueTextField.getText()) ? null : propName + " value must be one of " + ArrayUtils.toString(hostDistance);
                }

                return null;
            }
        });

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
                        else {
                            //set default values
                            propValueTextField.setText(getDefaultValueForProperty(propName));
                        }
                    } catch (FindException fe) {
                        JOptionPane.showMessageDialog(CassandraPropertiesDialog.this, "Unable to retrieve defaults for property " + propName, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        propNameComboBox.addItemListener(propNameListener);
        propNameComboBox.setSelectedIndex(-1);
    }

    private static final Pattern labelPattern = Pattern.compile("^(.*)(:\\s+\\*)\\s*");

    private String getFieldLabelTrimmed(JLabel label) {
        Matcher m =labelPattern.matcher(label.getText());
        if(m.find()) {
            return m.group(1);
        }
        return label.getText();
    }

    private String getDefaultValueForProperty(String propName) {
        if(propName == null) return null;
        switch (propName) {
            case HOST_DISTANCE:
                return "LOCAL";
            case CORE_CONNECTION_PER_HOST:
                return "1";
            case MAX_CONNECTION_PER_HOST:
                return "2";
            case MAX_SIMUL_REQ_PER_CONNECTION_THRESHOLD:
                return "128";
            case MIN_SIMUL_REQ_PER_CONNECTION_THRESHOLD:
                return "25";
            case CONNECTION_TIMEOUT_MILLIS:
                return "5000";
            case READ_TIMEOUT_MILLIS:
                return "12000";
            case KEEP_ALIVE:
                return "false";
            case TCP_NO_DELAY:
                return "false";
            case MAX_RECORDS:
                return "10";
            case QUERY_FETCH_SIZE:
                return "5000";
            default:
                return null;

        }
    }

    private String getNumberValidationErrorString(String propName, int minValue, int maxVal) {
        if(!NumberUtils.isNumber(propValueTextField.getText()) || (NumberUtils.toInt(propValueTextField.getText()) < minValue || NumberUtils.toInt(propValueTextField.getText()) > maxVal)) {
            return propName + " value must be the number between "+ Integer.toString(minValue) + " and " + Integer.toString(maxVal);
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
