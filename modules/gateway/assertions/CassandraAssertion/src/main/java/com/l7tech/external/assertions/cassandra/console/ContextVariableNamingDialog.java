package com.l7tech.external.assertions.cassandra.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.MutablePair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class ContextVariableNamingDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.cassandra.console.CassandraQueryAssertionPropertiesDialog");

    private JPanel mainPanel;
    private JTextField nameTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JLabel nameLabel;
    private JLabel valueLabel;
    private TargetVariablePanel variableNamePanel;


    private final InputValidator validator = new InputValidator(this, resources.getString("dialog.title.context.variables.naming"));
    private boolean confirmed = false;

    public ContextVariableNamingDialog(Dialog parent, MutablePair<String, String> columnAlias, String prefix) {
        super(parent, resources.getString("dialog.title.context.variables.naming"), true);
        initComponents();
        setData(prefix, columnAlias);
    }

    private void initComponents() {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        validator.constrainTextFieldToBeNonEmpty(nameLabel.getText(), nameTextField, null);
        validator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return !variableNamePanel.isEntryValid() ? resources.getString("column.label.variable.name") + " " + variableNamePanel.getErrorMessage() : null;
            }
        });

        validator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        pack();
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void setData(String prefix, MutablePair<String, String> columnAlias) {
        if(columnAlias == null) {
            return;
        }

        nameTextField.setText(columnAlias.left);
        variableNamePanel.setPrefix(prefix);
        variableNamePanel.setVariable(columnAlias.right);

        pack();
    }

    public MutablePair<String, String> getData(MutablePair<String, String> cassandraNamedParameter) {
        cassandraNamedParameter.setKey(nameTextField.getText().trim());
        cassandraNamedParameter.setValue(variableNamePanel.getSuffix());

        return cassandraNamedParameter;
    }

}
