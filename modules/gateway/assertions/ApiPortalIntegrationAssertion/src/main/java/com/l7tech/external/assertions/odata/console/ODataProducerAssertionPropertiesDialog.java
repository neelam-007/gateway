package com.l7tech.external.assertions.odata.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.odata.ODataProducerAssertion;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Properties dialog for the JDBC Query Assertion.
 */
public class ODataProducerAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<ODataProducerAssertion> {
    private static final Logger logger = Logger.getLogger(ODataProducerAssertionPropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.odata.console.resources.ODataProducerAssertionPropertiesDialog");

    private JPanel mainPanel;
    /**
     * Should never have a null selection as a default is always set in @{link #modelToView}
     */
    private JComboBox<String> connectionComboBox;
    private JButton cancelButton;
    private JButton okButton;

    private ODataProducerAssertion assertion;
    private boolean confirmed;
    private PermissionFlags jdbcConnPermFlags;
    private final Map<String, String> connToDriverMap = new HashMap<String, String>();

    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));

    public ODataProducerAssertionPropertiesDialog(Window owner, ODataProducerAssertion assertion) {
        super(owner, assertion);
        this.assertion = assertion;
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(ODataProducerAssertion assertion) {
        this.assertion = assertion;
        modelToView();
        configureView();
    }

    @Override
    public ODataProducerAssertion getData(final ODataProducerAssertion assertion) {
        viewToModel(assertion);
        return assertion;
    }

    @Override
    protected void configureView() {
        enableOrDisableOkButton();
        enableOrDisableJdbcConnList();
    }

    private void initialize() {
        jdbcConnPermFlags = PermissionFlags.get(EntityType.JDBC_CONNECTION);

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        final RunOnChangeListener connectionListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
            }
        });
        ((JTextField) connectionComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(connectionListener);
        connectionComboBox.addItemListener(connectionListener);


        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        });

        final RunOnChangeListener queryPanelChangeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        });

        final InputValidator inputValidator = new InputValidator(this, resources.getString("dialog.title.jdbc.query.props"));

        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });
    }

    private void modelToView() {
        populateConnectionCombobox();

        final String connName = assertion.getConnectionName();
        if (connName != null) {
            connectionComboBox.setSelectedItem(connName);

        } else {
            // default selection is no selection
            connectionComboBox.setSelectedItem("");
        }


    }

    private void viewToModel(final ODataProducerAssertion assertion) {
        assertion.setConnectionName((connectionComboBox.getSelectedItem()).toString());
    }


    private boolean isSchemaCapable(final String connName) {
        if (connToDriverMap.containsKey(connName)) {
            final String driverClass = connToDriverMap.get(connName);
            return driverClass.contains("oracle") || driverClass.contains("sqlserver");
        }

        // if we don't know about the connection and a variable is entered, then we need to allow it
        return Syntax.isAnyVariableReferenced(connName);
    }

    private void populateConnectionCombobox() {
        java.util.List<JdbcConnection> connectionList;
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) {
            return;
        } else {
            try {
                connectionList = admin.getAllJdbcConnections();
            } catch (FindException e) {
                logger.warning("Error getting JDBC connection names");
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connectionList);

        connectionComboBox.removeAllItems();

        // Add an empty driver class at the first position of the list
        connectionComboBox.addItem("");

        for (JdbcConnection conn : connectionList) {
            final String connName = conn.getName();
            connToDriverMap.put(connName, conn.getDriverClass());
            connectionComboBox.addItem(connName);
        }

    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    private void enableOrDisableOkButton() {
        boolean enabled = !isReadOnly() &&
                isNonEmptyRequiredTextField(((JTextField) connectionComboBox.getEditor().getEditorComponent()).getText());

        okButton.setEnabled(enabled);
    }

    private void enableOrDisableJdbcConnList() {
        connectionComboBox.setEnabled(jdbcConnPermFlags.canReadAll());
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private void doOk() {

        // validate an oracle query does not try and reference a schema name. This is a heuristic as it cannot be
        // validated for sure without knowing the list of valid package names.
        boolean isValidQuery = true;
        final String connName = connectionComboBox.getSelectedItem().toString();

        getData(assertion);
        confirmed = true;
        ODataProducerAssertionPropertiesDialog.this.dispose();
    }


    private void doCancel() {
        ODataProducerAssertionPropertiesDialog.this.dispose();
    }

    private JdbcAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return reg.getJdbcConnectionAdmin();
    }
}