package com.l7tech.console.panels;

import com.l7tech.console.policy.exporter.JdbcConnectionReference;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.jdbcconnection.JdbcAdmin;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.Collections;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * @author ghuang
 */
public class ResolveJdbcConnectionPanel extends WizardStepPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.ResolveJdbcConnectionPanel");
    private static final Logger logger = Logger.getLogger(ResolveJdbcConnectionPanel.class.getName());

    private JPanel mainPanel;
    private JComboBox connectionComboBox;
    private JButton manageConnectionsButton;

    private JdbcConnectionReference connectionReference;

    public ResolveJdbcConnectionPanel(WizardStepPanel next, JdbcConnectionReference connectionReference) {
        super(next);
        this.connectionReference = connectionReference;
        initialize();
    }

    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return MessageFormat.format(resources.getString("label.unresolved.jdbc.conn"), connectionReference.getConnectionName());
    }

    @Override
    public boolean onNextButton() {
        if (connectionComboBox.getSelectedIndex() < 0) return false;

        String connectionName = (String) connectionComboBox.getSelectedItem();
        connectionReference.setLocalizeReplace(connectionName);
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        manageConnectionsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                doManageJdbcConnections();
            }
        });

        populateConnectionCombobox();
    }

    private void doManageJdbcConnections() {
        JdbcConnectionManagerWindow connMgrWindow = new JdbcConnectionManagerWindow(TopComponents.getInstance().getTopParent());
        connMgrWindow.pack();
        Utilities.centerOnScreen(connMgrWindow);
        DialogDisplayer.display(connMgrWindow);
    }

    private void populateConnectionCombobox() {
        java.util.List<String> connNameList;
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) return;
        else {
            try {
                connNameList = admin.getAllJdbcConnectionNames();
            } catch (FindException e) {
                logger.warning("Error getting JDBC connection names");
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connNameList);

        // Add all items into the combox box.
        connectionComboBox.removeAllItems();
        for (String driverClass: connNameList) {
            connectionComboBox.addItem(driverClass);
        }
        //connectionComboBox.setSelectedIndex(0);
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
