package com.l7tech.console.panels.bundles;

import com.l7tech.console.action.ManageJdbcConnectionsAction;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Logger;

/**
 * UI Component for showing bundle meta data
 */
public class BundleComponent extends JPanel{
    private JPanel bundlePanel;
    private JCheckBox installCheckBox;
    private JPanel jdbcPanel;
    private JLabel policyJdbcConnLabel;
    private JComboBox<String> availableJdbcConnsComboBox;
    private JButton manageJDBCConnectionsButton;

    public BundleComponent(BundleInfo bundleInfo) {
        this(bundleInfo.getName(), bundleInfo.getDescription(), bundleInfo.getVersion(), bundleInfo.getJdbcConnectionReferences());
    }

    /**
     *
     * @param bundleName
     * @param bundleDesc
     * @param version
     * @param jdbcConnectionNames Note currently only the first returned via iterator.next will be used.
     */
    public BundleComponent(@NotNull String bundleName, @NotNull String bundleDesc, @NotNull String version, @NotNull Set<String> jdbcConnectionNames) {

        final TitledBorder titledBorder = BorderFactory.createTitledBorder(bundleName);
        bundlePanel.setBorder(titledBorder);

        installCheckBox.setText(bundleDesc + " (v" + version + ")");
        if (!jdbcConnectionNames.isEmpty()) {
            //todo make the jdbc mapping a separate component so that we can support more than one.
            final String firstConn = jdbcConnectionNames.iterator().next();
            policyJdbcConnLabel.setText(firstConn);

            final ManageJdbcConnectionsAction action = new ManageJdbcConnectionsAction();
            manageJDBCConnectionsButton.setEnabled(action.isEnabled());

            manageJDBCConnectionsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.actionPerformed(e);
                    refreshJdbcConnections();
                }
            });
            refreshJdbcConnections();
            availableJdbcConnsComboBox.setSelectedIndex(-1);

            // remember all existing connections
            final int itemCount = availableJdbcConnsComboBox.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                final String existingConn = availableJdbcConnsComboBox.getItemAt(i).toString();
                mappedJdbcConnections.put(existingConn, existingConn);
            }
        } else {
            jdbcPanel.setVisible(false);
        }

    }

    public JPanel getBundlePanel() {
        return bundlePanel;
    }

    public JCheckBox getInstallCheckBox() {
        return installCheckBox;
    }

    @NotNull
    public Map<String, String> getMappedJdbcConnections() {

        final Map<String, String> returnMap = new HashMap<String, String>();
        final Object selectedConn = availableJdbcConnsComboBox.getSelectedItem();
        if (selectedConn != null) {
            final String mappedConn = selectedConn.toString();
            final String policyConn = policyJdbcConnLabel.getText();
            returnMap.put(policyConn, mappedConn);

        }
        return returnMap;
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(BundleComponent.class.getName());
    private final Map<String, String> mappedJdbcConnections = new HashMap<String, String>();

    private void refreshJdbcConnections() {
        Registry registry = Registry.getDefault();
        if (registry.isAdminContextPresent()) {
            final JdbcAdmin jdbcAdmin = registry.getJdbcConnectionAdmin();
            // remember currently selected item
            boolean keepExistingSelection = false;

            final Object selectedItem = availableJdbcConnsComboBox.getSelectedItem();
            final String existingSelection;
            if (selectedItem != null) {
                existingSelection = selectedItem.toString();
            } else {
                existingSelection = policyJdbcConnLabel.getText();
            }
            try {
                availableJdbcConnsComboBox.removeAllItems();
                final List<String> allConnNames = jdbcAdmin.getAllJdbcConnectionNames();
                Collections.sort(allConnNames);
                for (String connName : allConnNames) {
                    availableJdbcConnsComboBox.addItem(connName);
                    if (existingSelection != null && connName.equals(existingSelection)) {
                        keepExistingSelection = true;
                    }
                }

                if (keepExistingSelection) {
                    availableJdbcConnsComboBox.setSelectedItem(existingSelection);
                }

            } catch (FindException e) {
                logger.warning("Could not retrieve list of JDBC Connections on Gateway.");
                availableJdbcConnsComboBox.removeAllItems();
            }
        } else {
            availableJdbcConnsComboBox.removeAllItems();
        }
    }

}
