package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Operation;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog to edit the property of the Operation Assertion.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2006<br/>
 * @see Operation
 */
public class WSDLOperationPropertiesDialog extends JDialog {
    private static final String TITLE = "WSDL Operation Assertion Properties";
    private Operation assertion;
    public boolean oked = false;
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JComboBox operationCombo;
    private final String[] possibleOperationNames;

    public WSDLOperationPropertiesDialog(Frame owner, Operation assertion, String[] possibleOperationNames) throws HeadlessException {
        super(owner, TITLE, true);
        this.assertion = assertion;
        this.possibleOperationNames = possibleOperationNames;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        Utilities.equalizeButtonSizes(new AbstractButton[] {okButton, cancelButton, helpButton});

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        operationCombo.setModel(new DefaultComboBoxModel(possibleOperationNames));

        setInitialData();
    }

    public void setInitialData() {
        if (assertion.getOperationName() != null) {
            for (String possibleOperationName : possibleOperationNames) {
                if (possibleOperationName.equals(assertion.getOperationName())) {
                    operationCombo.setSelectedItem(possibleOperationName);
                    break;
                }
            }
        }
        oked = false;
    }

    private void ok() {
        assertion.setOperationName(operationCombo.getSelectedItem().toString());
        oked = true;
        cancel();
    }

    private void cancel() {
        dispose();
    }

    private void help() {
        Actions.invokeHelp(this);
    }
}
