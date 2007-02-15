/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.logic.*;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.external.assertions.comparison.ComparisonAssertion;
import com.l7tech.policy.variable.DataType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class ComparisonPropertiesDialog extends JDialog implements AssertionPropertiesEditor<ComparisonAssertion> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.comparison.console.resources.ComparisonAssertion");

    private JTextField expressionField;
    private JComboBox dataTypeComboBox;
    private JTable predicatesTable;
    private JButton addPredicateButton;
    private JButton okButton;
    private JButton cancelButton;
    private JButton removePredicateButton;
    private JPanel mainPanel;
    private JButton editPredicateButton;

    private boolean ok;

    private ComparisonAssertion assertion;
    private final java.util.List<Predicate> predicates = new ArrayList<Predicate>();
    private final PredicatesTableModel predicatesTableModel = new PredicatesTableModel();

    public ComparisonPropertiesDialog(Frame owner, ComparisonAssertion assertion) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.assertion = assertion;
        init();
    }

    public ComparisonPropertiesDialog(Dialog owner, ComparisonAssertion assertion) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.assertion = assertion;
        init();
    }

    public JDialog getDialog() {
        return this;
    }

    public boolean isConfirmed() {
        return ok;
    }

    public void setData(ComparisonAssertion assertion) {
        this.assertion = assertion;
        updateModel();
    }

    public ComparisonAssertion getData(ComparisonAssertion assertion) {
        return assertion;
    }

    private static class PredicateSelection {
        private final String name;
        private final Class<? extends Predicate> predClass;

        private PredicateSelection(String name, Class<? extends Predicate> predClass) {
            this.name = name;
            this.predClass = predClass;
        }

        public String toString() {
            return name;
        }
    }

    private static final PredicateSelection BINARY = new PredicateSelection(resources.getString("binaryPredicate.name"), BinaryPredicate.class);
    private static final PredicateSelection CARDINALITY = new PredicateSelection(resources.getString("cardinalityPredicate.name"), CardinalityPredicate.class);
    private static final PredicateSelection REGEX = new PredicateSelection(resources.getString("regexPredicate.name"), RegexPredicate.class);
    private static final PredicateSelection LENGTH = new PredicateSelection(resources.getString("stringLengthPredicate.name"), StringLengthPredicate.class);
    private static final PredicateSelection[] VALUES = { BINARY, CARDINALITY, REGEX, LENGTH };

    private void init() {
        DataTypePredicate dtp = null;
        for (Predicate predicate : assertion.getPredicates()) {
            if (predicate instanceof DataTypePredicate) {
                if (dtp != null) throw new RuntimeException("Multiple DataTypePredicates found");
                dtp = (DataTypePredicate) predicate;
                continue;
            }
            predicates.add(predicate);
        }

        expressionField.setText(assertion.getExpression1());

        predicatesTable.setModel(predicatesTableModel);
        predicatesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        predicatesTable.setTableHeader(null);
        predicatesTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelected(); 
            }
        });
        predicatesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });

        dataTypeComboBox.setModel(new DefaultComboBoxModel(DataType.VALUES));
        dataTypeComboBox.setSelectedItem(dtp == null ? DataType.UNKNOWN : dtp.getType());

        addPredicateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                DialogDisplayer.showInputDialog(ComparisonPropertiesDialog.this, resources.getString("predicateSelection.text"), resources.getString("predicateSelection.title"), JOptionPane.QUESTION_MESSAGE, null, VALUES, BINARY, new DialogDisplayer.InputListener() {
                    public void reportResult(Object option) {
                        PredicateSelection sel = (PredicateSelection) option;
                        if (sel == null) return;
                        Class<? extends Predicate> predClass = sel.predClass;
                        try {
                            Constructor<? extends Predicate> ctor = predClass.getConstructor();
                            Predicate pred = ctor.newInstance();
                            edit(pred, -1);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

            }
        });

        editPredicateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editSelected();
            }
        });

        removePredicateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int sel = predicatesTable.getSelectionModel().getMinSelectionIndex();
                if (sel < 0 || sel > predicates.size()-1) return;
                predicates.remove(sel);
                predicatesTableModel.fireTableDataChanged();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateModel();
                ok = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        add(mainPanel);
    }

    private void editSelected() {
        int sel = predicatesTable.getSelectionModel().getMinSelectionIndex();
        if (sel < 0 || sel > predicates.size()-1) return;
        Predicate pred = predicates.get(sel);
        edit(pred, sel);
    }

    public boolean isOk() {
        return ok;
    }

    private void updateModel() {
        DataType type = (DataType) dataTypeComboBox.getSelectedItem();
        java.util.List<Predicate> newPreds = new ArrayList<Predicate>();
        if (type != null && type != DataType.UNKNOWN)
            newPreds.add(new DataTypePredicate(type));
        for (Predicate pred : predicates) {
            newPreds.add(pred);
        }
        assertion.setExpression1(expressionField.getText());
        assertion.setPredicates(newPreds.toArray(new Predicate[0]));
    }

    void enableButtons() {
        String expr = expressionField.getText();

        boolean canOk = expr != null && expr.length() > 0;

        okButton.setEnabled(canOk);

        int sel = predicatesTable.getSelectionModel().getMinSelectionIndex();
        editPredicateButton.setEnabled(sel >= 0);
        removePredicateButton.setEnabled(sel >= 0);
    }

    /**
     * @param pos negative if the predicate is new
     */
    private void edit(final Predicate predicate, final int pos) {
        DataType type = (DataType) dataTypeComboBox.getSelectedItem();
        final PredicateDialog dlg = PredicateDialog.make(this, type, predicate, expressionField.getText());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    if (pos < 0) predicates.add(predicate);
                    predicatesTableModel.fireTableDataChanged();
                }
            }
        });
    }


    private class PredicatesTableModel extends AbstractTableModel {
        public int getRowCount() {
            return predicates.size();
        }

        public int getColumnCount() {
            return 1; // TODO make room for pretty version
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex > predicates.size()) return null;
            return predicates.get(rowIndex); // TODO make this prettier
        }
    }
}
