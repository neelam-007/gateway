package com.l7tech.external.assertions.jdbcquery.console;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.event.*;

public class VariableMappingDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField columnNameTextField;
    private JTextField variableNameSuffixTextField;

    private boolean wasOkButtonPressed;

    private final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
        public void run() {
            enableDisableOkButton();
        }
    });

    public VariableMappingDialog(JDialog owner, String title){
        this(owner, title, null, null);
    }

    public VariableMappingDialog(JDialog owner, String title, String colName, String varName) {
        super(owner, title);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        pack();
        Utilities.centerOnScreen(this);


        columnNameTextField.getDocument().addDocumentListener(changeListener);
        variableNameSuffixTextField.getDocument().addDocumentListener(changeListener);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


        columnNameTextField.setText(colName);
        variableNameSuffixTextField.setText(varName);
        wasOkButtonPressed = false;
        enableDisableOkButton();
    }

    private void enableDisableOkButton(){
        boolean enableOk = columnNameTextField != null && columnNameTextField.getText().length() > 0 &&
                variableNameSuffixTextField != null && variableNameSuffixTextField.getText().length() > 0;

        this.okButton.setEnabled(enableOk);
    }

    public String getColumnName() {
        return columnNameTextField.getText();
    }

    public String getVariableSuffixName() {
        return variableNameSuffixTextField.getText();
    }

    public boolean isConfirmed() {
        return wasOkButtonPressed;
    }

    private void onOK() {
        wasOkButtonPressed = true;
        setVisible(false);
    }

    private void onCancel() {
        setVisible(false);
    }
}
