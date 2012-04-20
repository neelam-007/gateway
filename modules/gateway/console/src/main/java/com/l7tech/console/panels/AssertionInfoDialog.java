package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog which displays some basic information about an assertion.
 */
public class AssertionInfoDialog extends JDialog {
    public AssertionInfoDialog(@NotNull final Frame parent, @NotNull final Assertion assertion) {
        super(parent, "Assertion Information", true);
        setContentPane(contentPanel);
        nameLabel.setText(assertion.meta().get(AssertionMetadata.SHORT_NAME).toString());
        descriptionTextPane.setText(assertion.meta().get(AssertionMetadata.DESCRIPTION).toString());
        descriptionTextPane.setCaretPosition(0);
        descriptionScrollPane.setBorder(BorderFactory.createEmptyBorder());
        displayVariablesUsed(assertion);
        displayVariablesSet(assertion);
        pack();
        setLocationRelativeTo(parent);
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private static final String NOT_AVAILABLE = "N/A";
    private static final String NAME = "Name";
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final String TYPE = "Type";
    private static final String MULTIVALUED = "Multivalued";
    private static final Logger LOGGER = Logger.getLogger(AssertionInfoDialog.class.getName());
    private JPanel contentPanel;
    private JPanel setsVariablesTablePanel;
    private JTable setsVariablesTable;
    private JLabel setsVariablesLabel;
    private JLabel nameLabel;
    private JScrollPane descriptionScrollPane;
    // using a text pane because some assertion descriptions contain html code for styling
    private JTextPane descriptionTextPane;
    private JLabel usesVariablesLabel;
    private JPanel usesVariablesTablePanel;
    private JTable usesVariablesTable;

    private void displayVariablesUsed(Assertion assertion) {
        if (assertion instanceof UsesVariables) {
            final UsesVariables usesVariables = (UsesVariables) assertion;
            final String[] variablesUsed = usesVariables.getVariablesUsed();
            if (variablesUsed.length > 0) {
                final Object[][] data = new Object[variablesUsed.length][1];
                for (int i = 0; i < variablesUsed.length; i++) {
                    String var = variablesUsed[i];
                    data[i][0] = var;
                }
                usesVariablesTable.setModel(new UneditableTableModel(data, new String[]{NAME}));
                usesVariablesTable.setCellSelectionEnabled(true);
                usesVariablesTablePanel.setLayout(new BorderLayout());
                usesVariablesTablePanel.add(usesVariablesTable.getTableHeader(), BorderLayout.NORTH);
                usesVariablesTablePanel.add(usesVariablesTable, BorderLayout.CENTER);
            } else {
                disableUsesVariableTable();
            }
        } else {
            disableUsesVariableTable();
        }
    }

    private void displayVariablesSet(Assertion assertion) {
        if (assertion instanceof SetsVariables) {
            try {
                final SetsVariables setsVariables = (SetsVariables) assertion;
                final VariableMetadata[] variablesSet = setsVariables.getVariablesSet();
                if (variablesSet.length > 0) {
                    final Object[][] data = new Object[variablesSet.length][3];
                    for (int i = 0; i < variablesSet.length; i++) {
                        final VariableMetadata var = variablesSet[i];
                        data[i][0] = var.getName();
                        data[i][1] = var.getType().getShortName();
                        data[i][2] = var.isMultivalued() ? YES : NO;
                    }
                    setsVariablesTable.setModel(new UneditableTableModel(data, new String[]{NAME, TYPE, MULTIVALUED}));
                    // want to be able to copy-paste context variables
                    setsVariablesTable.setCellSelectionEnabled(true);
                    setsVariablesTablePanel.setLayout(new BorderLayout());
                    setsVariablesTablePanel.add(setsVariablesTable.getTableHeader(), BorderLayout.NORTH);
                    setsVariablesTablePanel.add(setsVariablesTable, BorderLayout.CENTER);
                } else {
                    disableSetsVariableTable();
                }
            } catch (final VariableNameSyntaxException e) {
                // can happen if a variable name is set with incorrect syntax
                LOGGER.log(Level.WARNING, "Error retrieving variables set: " + e.getMessage(), ExceptionUtils.getDebugException(e));
                disableSetsVariableTable();
            }
        } else {
            disableSetsVariableTable();
        }
    }

    /**
     * Hides sets variables table.
     */
    private void disableSetsVariableTable() {
        setsVariablesLabel.setText(NOT_AVAILABLE);
        setsVariablesTablePanel.setEnabled(false);
        setsVariablesTablePanel.setVisible(false);
    }

    /**
     * Hides uses variables table.
     */
    private void disableUsesVariableTable() {
        usesVariablesLabel.setText(NOT_AVAILABLE);
        usesVariablesTablePanel.setEnabled(false);
        usesVariablesTablePanel.setVisible(false);
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
