package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.VariableMetadata;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Dialog which displays some basic information about an assertion.
 */
public class AssertionInfoDialog extends JDialog {
    private JPanel contentPanel;
    private JPanel tablePanel;
    private JTable setsVariablesTable;
    private JLabel setsVariablesLabel;
    private JLabel nameLabel;
    private JScrollPane descriptionScrollPane;
    // using a text pane because some assertion descriptions contain html code for styling
    private JTextPane descriptionTextPane;

    public AssertionInfoDialog(@NotNull final Frame parent, @NotNull final Assertion assertion) {
        super(parent, "Assertion Information", true);
        setContentPane(contentPanel);
        nameLabel.setText(assertion.meta().get(AssertionMetadata.SHORT_NAME).toString());
        descriptionTextPane.setText(assertion.meta().get(AssertionMetadata.DESCRIPTION).toString());
        descriptionScrollPane.setBorder(BorderFactory.createEmptyBorder());
        if (assertion instanceof SetsVariables) {
            final SetsVariables setsVariables = (SetsVariables) assertion;
            final VariableMetadata[] variablesSet = setsVariables.getVariablesSet();
            if (variablesSet.length > 0) {
                final Object[][] data = new Object[variablesSet.length][3];
                for (int i = 0; i < variablesSet.length; i++) {
                    final VariableMetadata var = variablesSet[i];
                    data[i][0] = var.getName();
                    data[i][1] = var.getType().getShortName();
                    data[i][2] = var.isMultivalued() ? "yes" : "no";
                }
                setsVariablesTable.setModel(new UneditableTableModel(data, new String[]{"Name", "Type", "Multivalued"}));
                // want to be able to copy-paste context variables
                setsVariablesTable.setCellSelectionEnabled(true);
                tablePanel.setLayout(new BorderLayout());
                tablePanel.add(setsVariablesTable.getTableHeader(), BorderLayout.NORTH);
                tablePanel.add(setsVariablesTable, BorderLayout.CENTER);
            } else {
                disableVariableTable();
            }
        } else {
            disableVariableTable();
        }
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Hides context variable table.
     */
    private void disableVariableTable() {
        setsVariablesLabel.setText("N/A");
        tablePanel.setEnabled(false);
        tablePanel.setVisible(false);
    }

    private class UneditableTableModel extends DefaultTableModel {
        private UneditableTableModel(final Object[][] data, final Object[] headers) {
            super(data, headers);
        }

        @Override
        public boolean isCellEditable(int i, int i1) {
            return false;
        }
    }
}
