/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.treetable.AbstractTreeTableModel;
import com.l7tech.console.util.treetable.JTreeTable;
import com.l7tech.console.util.treetable.TreeTableModel;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class VariablesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(VariablesDialog.class.getName());

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel innerPanel;

    private VariableMetadata selectedVariable;

    private final ResourceBundle variableProperties = ResourceBundle.getBundle("com.l7tech.common.resources.variables");
    private Object FAKE = new Object() {
        public String toString() {
            return "You shouldn't be seeing this!";
        }
    };
    private final JTreeTable treeTable;
    private static final String BUILTIN_DESC = "Variables that are built-in to the SSG and available to all policies";
    private static final String POLICY_DESC = "Variables that are set by policy assertions and available to subsequent assertions in the policy";

    public VariableMetadata getSelectedVariable() {
        return selectedVariable;
    }

    private final VariableMetadata[] builtinVars;
    private final String BUILTIN = "Built-in Variables";

    private final VariableMetadata[] policyVars;
    private final String POLICY = "Policy Variables";

    public VariablesDialog(Frame owner, Assertion assertion, boolean modal) throws HeadlessException {
        super(owner, "Variables", modal);

        TreeMap sorted = new TreeMap(BuiltinVariables.getAllMetadata());
        java.util.List bv = new ArrayList();
        for (Iterator i = sorted.keySet().iterator(); i.hasNext();) {
            String name = (String)i.next();
            VariableMetadata meta = (VariableMetadata)sorted.get(name);
            final String cname = meta.getCanonicalName();
            if (meta.getName().equals(cname)) {
                // Avoid showing non-canonical names
                bv.add(meta);
            }
        }
        builtinVars = (VariableMetadata[]) bv.toArray(new VariableMetadata[0]);

        if (assertion == null) {
            // We're not focused on an assertion; only display built-in variables
            policyVars = null;
        } else {
            // Find variables set by this assertion's predecessors
            Map vars = PolicyVariableUtils.getVariablesSetByPredecessors(assertion);

            java.util.List pv = new ArrayList();
            for (Iterator i = vars.keySet().iterator(); i.hasNext();) {
                String var = (String)i.next();
                VariableMetadata meta = (VariableMetadata) vars.get(var);
                pv.add(meta);
            }
            policyVars = (VariableMetadata[]) pv.toArray(new VariableMetadata[0]);
        }

        TreeCellRenderer tcr = new DefaultTreeCellRenderer() {
            public Icon getLeafIcon() {
                return null;
            }

            public String getToolTipText() {
                String text = getText();
                if (text.equals(BUILTIN)) {
                    return BUILTIN_DESC;
                } else if (text.equals(POLICY)) {
                    return POLICY_DESC;
                } else
                    return null;
            }
        };

        treeTable = new JTreeTable(new VariableTreeTableModel(policyVars == null ? BUILTIN : FAKE), tcr, false);
        treeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    checkDone();
                }
            }
        });

        treeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                okButton.setEnabled(selectedVariable() != null);
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                checkDone();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                selectedVariable = null;
                dispose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        innerPanel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(treeTable);
        innerPanel.add(scrollPane, BorderLayout.CENTER);

        okButton.setEnabled(false);

        add(mainPanel);
    }

    private void checkDone() {
        VariableMetadata meta = selectedVariable();
        if (meta != null) {
            selectedVariable = meta;
            dispose();
        }
    }

    private VariableMetadata selectedVariable() {
        Object selected = treeTable.getModel().getValueAt(treeTable.getSelectedRow(), 0);
        if (selected instanceof VariableMetadata) {
            return (VariableMetadata)selected;
        }
        return null;
    }

    private class VariableTreeTableModel extends AbstractTreeTableModel implements TreeTableModel {
        private final String[] NAMES = {"Variable Name", "Description"};
        private final Class[] CLASSES = {TreeTableModel.class, String.class};

        public VariableTreeTableModel(Object root) {
            super(root);
        }

        public int getColumnCount() {
            return NAMES.length;
        }

        public String getColumnName(int column) {
            return NAMES[column];
        }

        public Object getValueAt(Object node, int column) {
            if ((node == BUILTIN || node == POLICY)) {
                if (column == 0) return node;
                return null;
            }

            if (node instanceof VariableMetadata) {
                VariableMetadata meta = (VariableMetadata) node;
                switch(column) {
                    case 0:
                        return meta;
                    case 1:
                        try {
                            return variableProperties.getString(meta.getCanonicalName());
                        } catch (MissingResourceException e) {
                            logger.log(Level.WARNING, "Can't get description for '" + meta.getName() + "'", e);
                            return "<unknown>";
                        }
                }
            }
            return "<unknown: " + node + " @ " + column + ">";
        }

        public Class getColumnClass(int column) {
            return CLASSES[column];
        }

        public Object getChild(Object object, int i) {
            if (object == POLICY) {
                return policyVars[i];
            } else if (object == BUILTIN) {
                return builtinVars[i];
            } else if (object == FAKE) {
                switch(i) {
                    case 0:
                        return POLICY;
                    case 1:
                        return BUILTIN;
                }
            }
            return "<unknown: " + object + " @ " + i + ">";
        }

        public int getChildCount(Object object) {
            if (object == POLICY) {
                return policyVars.length;
            } else if (object == BUILTIN) {
                return builtinVars.length;
            } else if (object == FAKE) {
                return 2;
            } else {
                return 0;
            }
        }
    }

}
