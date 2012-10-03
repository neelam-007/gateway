package com.l7tech.external.assertions.apiportalintegration.console;

import com.l7tech.external.assertions.apiportalintegration.server.upgrade.UpgradedEntity;
import com.l7tech.gui.util.Utilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

/**
 * Displays portal upgrade results.
 */
public class UpgradePortalResultDialog extends JDialog {
    private JLabel resultLabel;
    private JScrollPane resultScrollPane;
    private JTable resultTable;
    private JPanel contentPanel;
    private JLabel errorLabel;
    private JButton closeButton;

    public UpgradePortalResultDialog(@NotNull final Frame parent, @NotNull final List<UpgradedEntity> upgradedEntities, @Nullable final String error) {
        super(parent, "Upgrade Portal Results", true);
        setContentPane(contentPanel);
        getRootPane().setDefaultButton(closeButton);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                dispose();
            }
        });

        if (error != null) {
            errorLabel.setEnabled(true);
            errorLabel.setVisible(true);
            errorLabel.setText(error);
        } else {
            errorLabel.setEnabled(false);
            errorLabel.setVisible(false);
        }

        if (upgradedEntities.isEmpty()) {
            resultLabel.setText("No entities were upgraded.");
            resultTable.setEnabled(false);
            resultTable.setVisible(false);
            resultScrollPane.setEnabled(false);
            resultScrollPane.setVisible(false);
        } else {
            resultLabel.setText(upgradedEntities.size() + " entities upgraded:");
            final Vector<Vector> data = new Vector<Vector>();
            for (int i = 0; i < upgradedEntities.size(); i++) {
                final Vector<String> v = new Vector<String>(4);
                final UpgradedEntity entity = upgradedEntities.get(i);
                v.add(String.valueOf(i + 1));
                v.add(entity.getType());
                v.add(entity.getId());
                v.add(entity.getDescription());
                data.add(v);
            }

            final Vector<String> headers = new Vector<String>(2);
            headers.add("");
            headers.add("type");
            headers.add("id");
            headers.add("description");

            resultTable.setEnabled(true);
            resultTable.setVisible(true);
            resultTable.setModel(new UneditableTableModel(data, headers));
            resultTable.setCellSelectionEnabled(true);
            resultTable.getColumnModel().getColumn(0).setPreferredWidth(5);
            resultTable.getColumnModel().getColumn(1).setPreferredWidth(5);
            resultScrollPane.setEnabled(true);
            resultScrollPane.setVisible(true);
            resultScrollPane.setBorder(BorderFactory.createEmptyBorder());
        }

        pack();
        setLocationRelativeTo(parent);
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private class UneditableTableModel extends DefaultTableModel {
        private UneditableTableModel(@NotNull final Vector<Vector> data, @NotNull final Vector<String> headers) {
            super(data, headers);
        }

        @Override
        public boolean isCellEditable(final int i, final int i1) {
            return false;
        }
    }
}
