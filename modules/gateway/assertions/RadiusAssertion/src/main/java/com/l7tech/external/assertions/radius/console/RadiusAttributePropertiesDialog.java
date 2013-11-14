package com.l7tech.external.assertions.radius.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.radius.RadiusAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.MutablePair;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

public class RadiusAttributePropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField valueTextField;
    private JTextField nameTextField;
    private boolean canceled;
    private MutablePair<String, String> targetProp;

    public RadiusAttributePropertiesDialog(final Window parent,
                                           final MutablePair<String, String> targetProp,
                                           final Map<String, String> properties) {
        super(parent, DEFAULT_MODALITY_TYPE);
        this.targetProp = targetProp;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                checkFieldsForText();
            }
        });
        nameTextField.getDocument().addDocumentListener(changeListener);
        valueTextField.getDocument().addDocumentListener(changeListener);

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        if (this.targetProp != null) {
            nameTextField.setEnabled(false);
            nameTextField.setEditable(false);
            nameTextField.setText(this.targetProp.getKey());
            valueTextField.setText(this.targetProp.getValue());
            valueTextField.setCaretPosition(0);
        } else {
            nameTextField.setText("");
            valueTextField.setText("");
            nameTextField.setEnabled(true);
            nameTextField.setEditable(true);
        }

        InputValidator okValidator =
                new InputValidator(this, "Radius Attribute Configuration");

        InputValidator.ValidationRule attributeRule =
                new InputValidator.ComponentValidationRule(nameTextField) {
                    @Override
                    public String getValidationError() {
                        String[] tmp = Syntax.getReferencedNames(valueTextField.getText(), false);
                        if (tmp == null || tmp.length == 0) {
                            if (!getRadiusAdmin().isAttributeValid(nameTextField.getText(), valueTextField.getText())) {
                                return "The attribute " + nameTextField.getText() + " is invalid.";

                            }
                        }
                        return null;
                    }
                };

        okValidator.addRule(attributeRule);

        okValidator.attachToButton(buttonOK, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        checkFieldsForText();
        Utilities.setMinimumSize(this);
    }

    private static RadiusAdmin getRadiusAdmin() {
        return Registry.getDefault().getExtensionInterface(RadiusAdmin.class, null);
    }

    private void checkFieldsForText() {
        if (nameTextField.getText().trim().length() > 0) {
            buttonOK.setEnabled(true);
        } else {
            buttonOK.setEnabled(false);
        }
    }

    private void onOk() {
        if (targetProp == null) {
            targetProp = new MutablePair<String, String>("", "");
        }

        targetProp.left = nameTextField.getText();
        targetProp.right = valueTextField.getText();

        dispose();
    }

    private void onCancel() {
        canceled = true;
        dispose();
    }

    public boolean isCanceled() {
        return canceled;
    }

    public Pair<String, String> getTheProperty() {
        return targetProp.asPair();
    }
}
