/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: AuthorizationStatementWizardStepPanel.java 21045 2008-11-05 01:46:35Z vchan $
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import com.l7tech.external.assertions.samlpassertion.SamlpAuthorizationStatement;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextEntryPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The SAML Conditions <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class AuthorizationStatementWizardStepPanel extends SamlpWizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JTextField textFieldResource;
    private JRadioButton radioButtonEvidenceDefault;
    private JRadioButton radioButtonEvidenceFromVar;
    private JTextField textFieldEvidenceVariable;
    private JLabel evidenceLabel;
    // new Action/Namespace table
    private JTable actionTable;
    private DefaultTableModel actionTableModel;
    private JScrollPane actionTableScrollPane;
    private JButton buttonAddAction;
    private JButton buttonEditAction;
    private JButton buttonRemoveAction;
    private boolean showTitleLabel;

    /**
     * Creates new form AuthorizationStatementWizardStepPanel
     */
    public AuthorizationStatementWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, AssertionMode mode) {
        super(next, mode);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
     * Creates new form AuthorizationStatementWizardStepPanel
     */
    public AuthorizationStatementWizardStepPanel(WizardStepPanel next, AssertionMode mode) {
        this(next, true, mode);
    }


    /**
     * Creates new form AuthorizationStatementWizardStepPanel
     */
    public AuthorizationStatementWizardStepPanel(WizardStepPanel next,  boolean showTitleLabel, AssertionMode mode, JDialog owner) {
        super(next, mode);
        this.showTitleLabel = showTitleLabel;
        setOwner(owner);
        initialize();
    }

//    public AuthorizationStatementWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog parent) {
//        this(next, showTitleLabel, false, parent);
//    }
//
//    public AuthorizationStatementWizardStepPanel(WizardStepPanel next) {
//        this(next, true, false);
//    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlProtocolAssertion assertion = SamlProtocolAssertion.class.cast(settings);

        // skip this step if Authorization is not selected
        SamlpAuthorizationStatement statement = assertion.getAuthorizationStatement();
        setSkipped(statement == null);
        if (statement == null) {
            return;
        }

        textFieldResource.setText(statement.getResource());

        String action;
        for (int i=0; i<statement.getActions().length; i++) {
            action = statement.getActions()[i];
            if (actionTableModel.getRowCount() <= i) {
                actionTableModel.addRow(new String[] {action.substring(0, action.indexOf("||")), action.substring(action.indexOf("||")+2)});
            }
        }

        if (isRequestMode()) {
            readRequestSpecific(assertion);
        }
    }

    /**
     * Parses the request specific configurations from the assertion and updates the appropriate UI component.
     *
     * @param samlpAssertion the SAMLP assertion instance
     */
    private void readRequestSpecific(SamlProtocolAssertion samlpAssertion) {

        SamlpRequestBuilderAssertion assertion = SamlpRequestBuilderAssertion.class.cast(samlpAssertion);

        if (assertion.getEvidence() != null && assertion.getEvidence() == 1) {
            radioButtonEvidenceFromVar.setSelected(true);
            textFieldEvidenceVariable.setEnabled(true);
            textFieldEvidenceVariable.setText(assertion.getEvidenceVariable());
        } else {
            radioButtonEvidenceDefault.setSelected(true);
            textFieldEvidenceVariable.setEnabled(false);
        }
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlProtocolAssertion assertion = SamlProtocolAssertion.class.cast(settings);
        SamlpAuthorizationStatement statement = assertion.getAuthorizationStatement();
         if (statement == null) {
             throw new IllegalArgumentException();
         }

        String[] actions = new String[actionTableModel.getRowCount()];
        String act, ns;
        for (int i=0; i<actionTableModel.getRowCount(); i++) {
            act = (String) actionTable.getValueAt(i, 0);
            act = (act != null ? act.trim() : act);
            ns = (String) actionTable.getValueAt(i, 1);
            ns = (ns != null ? ns.trim() : ns);
            actions[i] = new StringBuffer(act).append("||").append(ns).toString();
        }

        statement.setActions(actions);
        statement.setResource(textFieldResource.getText());

        if (isRequestMode()) {
            storeRequestSpecific(assertion);
        }
    }

    /**
     * Updates request specific configurations to the assertion object.
     *
     * @param samlpAssertion the SAMLP assertion instance
     */
    private void storeRequestSpecific(SamlProtocolAssertion samlpAssertion) {

        SamlpRequestBuilderAssertion assertion = SamlpRequestBuilderAssertion.class.cast(samlpAssertion);

        if (radioButtonEvidenceDefault.isSelected()) {
            assertion.setEvidence(0);
            assertion.setEvidenceVariable(null);
        } else {
            assertion.setEvidence(1);
            assertion.setEvidenceVariable(textFieldEvidenceVariable.getText());
        }

    }

    private void initialize() {
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        initializeActionTable();
        initializeEvidenceButtons();

        DocumentListener docListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                notifyListeners();
            }

            public void insertUpdate(DocumentEvent e) {
                notifyListeners();
            }

            public void removeUpdate(DocumentEvent e) {
                notifyListeners();
            }
        };
        textFieldResource.getDocument().addDocumentListener(docListener);
//        textFieldAction.getDocument().addDocumentListener(docListener);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                notifyListeners();
            }
        });
    }

    /**
     * Initializes the Evidence radio button group.
     */
    protected void initializeEvidenceButtons() {

        ButtonGroup evidenceGroup = new ButtonGroup();
        evidenceGroup.add(radioButtonEvidenceDefault);
        evidenceGroup.add(radioButtonEvidenceFromVar);

        radioButtonEvidenceDefault.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (radioButtonEvidenceDefault.isSelected()) {
                    textFieldEvidenceVariable.setEnabled(false);
                }
            }
        });

        radioButtonEvidenceFromVar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (radioButtonEvidenceFromVar.isSelected()) {
                    textFieldEvidenceVariable.setEnabled(true);
                }
            }
        });
    }

    protected void initializeActionTable() {

        actionTableModel = new DefaultTableModel(new String[]{"Action", "Namespace"}, 0){
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        actionTableScrollPane.getViewport().setBackground(actionTable.getBackground());
        actionTable.setModel(actionTableModel);
        actionTable.getTableHeader().setReorderingAllowed(false);
        ListSelectionModel selectionModel = actionTable.getSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                buttonRemoveAction.setEnabled(actionTable.getSelectedRow() != -1);
                buttonEditAction.setEnabled(actionTable.getSelectedRow() != -1);
            }
        });

        // create action listeners for buttons
        buttonAddAction.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // open dialog box
                AuthorizationActionDialog dialog = new AuthorizationActionDialog(getOwner(), null, null);
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if (dialog.wasOk()) {
                    actionTableModel.addRow(new String[] {dialog.getAction(), dialog.getActionNamespace()});
                }
                notifyListeners();
            }
        });

        buttonEditAction.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (actionTable.getSelectedColumn() != -1) {
                    String action = (String) actionTableModel.getValueAt(actionTable.getSelectedRow(), 0);
                    String namespace = (String) actionTableModel.getValueAt(actionTable.getSelectedRow(), 1);

                    // open dialog box
                    AuthorizationActionDialog dialog = new AuthorizationActionDialog(getOwner(), action, namespace);
                    dialog.pack();
                    Utilities.centerOnScreen(dialog);
                    dialog.setVisible(true);

                    if (dialog.wasOk()) {
                        actionTableModel.setValueAt(dialog.getAction(), actionTable.getSelectedRow(), 0);
                        actionTableModel.setValueAt(dialog.getActionNamespace(), actionTable.getSelectedRow(), 1);
                    }

                    notifyListeners();
                }
            }
        });

        buttonRemoveAction.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (actionTable.getSelectedRow() != -1)
                    actionTableModel.removeRow(actionTable.getSelectedRow());

                notifyListeners();
            }
        });

        if (actionTableModel.getRowCount() == 0) {
            buttonEditAction.setEnabled(false);
            buttonRemoveAction.setEnabled(false);
        }
    }


    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        if (isRequestMode())
            return "Authorization Decision Query";
        return "Authorization Response";
    }

    public String getDescription() {

        StringBuffer sb = new StringBuffer("<html>Specify the Resource [required] that the SAMLP authorization ");
        if (isRequestMode()) {
            sb.append("request will be created for;");
        } else {
            sb.append("response must describe;");
        }
        return sb.append(" at least one Resource Action [required] and corresponding Action Namespace [optional] must be specified.</html>").toString();
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     * The resource and action must be specified
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        String resource = textFieldResource.getText();
//        return (notNullOrEmpty(resource) && actionTableModel.getRowCount() > 0);
        return (notNullOrEmpty(resource) && actionTable.getRowCount() > 0);
//        String action = textFieldAction.getText();
//        return (notNullOrEmpty(resource) && notNullOrEmpty(action));
    }

    private boolean notNullOrEmpty(String s) {
        return s != null && !"".equals(s.trim());
    }


    public static class ActionPanel extends TextEntryPanel {
        public ActionPanel() {
            this(null);
        }

        public ActionPanel(String initialValue) {
            super("Authorization action and namespace:", "action", initialValue);
        }

//        @Override
//        protected String getSemanticError(String model) {
//
//            StringTokenizer stok = new StringTokenizer(model, ":");
//            if (stok.countTokens() == 2) {
//            }
//
//            return null;
//        }

        @Override
        protected String getSyntaxError(String model) {
//            if (model == null || model.length() == 0) return null;
//            // if the URL contains context variable, you just can't check syntax
//
//            StringTokenizer stok = new StringTokenizer(model, ":");
//            if (stok.countTokens() != 2) {
//
//                return "Expected format is &lt;host&gt;:&lt;port number&gt;";
//
//            } else {
//                // check for valid hostname or IP address
//                String h = stok.nextToken();
//                if( !ValidationUtils.isValidDomain(h) ) {
//                    return "Invalid host value";
//                }
//
//                String p = stok.nextToken();
//                if( !ValidationUtils.isValidInteger(p, false, 1, 65535) ) {
//                    return "Port number must be from 1 to 65535";
//                }
//            }
            return null;
        }

    }

}