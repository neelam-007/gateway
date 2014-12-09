package com.l7tech.external.assertions.cassandra.console;

import com.datastax.driver.core.DataType;
import com.l7tech.external.assertions.cassandra.CassandraNamedParameter;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.MutablePair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class ContextVariableNamingDialog extends JDialog {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField valueTextField;
    private JLabel nameLabel;
    private JLabel valueLabel;

    private static final String TITLE = "Context Variable Naming";

    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    public ContextVariableNamingDialog(Dialog parent, MutablePair<String, String> columnAlias) {
        super(parent, TITLE, true);

        initComponents();
        setData(columnAlias);
    }

    public ContextVariableNamingDialog(Dialog parent, MutablePair<String, String> columnAlias, boolean edit) {
        super(parent, TITLE, true);

            initComponents();
            setData(columnAlias);
            if (edit){
                nameTextField.setEditable(false);
            }
        }

    private void initComponents() {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

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

        validator.constrainTextFieldToBeNonEmpty(nameLabel.getText(), nameTextField, null);

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

    private void setData(MutablePair<String, String> columnAlias) {
        if(columnAlias == null) {
            return;
        }

        nameTextField.setText(columnAlias.left);
        valueTextField.setText(columnAlias.right);

        pack();
    }

    public MutablePair<String, String> getData(MutablePair<String, String> cassandraNamedParameter) {
        cassandraNamedParameter.setKey(nameTextField.getText().trim());
        cassandraNamedParameter.setValue(valueTextField.getText());

        return cassandraNamedParameter;
    }

}
