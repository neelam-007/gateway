package com.l7tech.console.panels.resources;

import com.l7tech.gateway.common.resources.HttpConfigurationProperty;
import com.l7tech.gui.util.InputValidator;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * HttpHeaderDialog  dialog.
 */
public class HttpHeaderDialog extends JDialog {
    private JLabel nameLabel;
    private JTextField valueTextField;
    private JTextField nameTextField;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JLabel valueLabel;
    private HttpConfigurationProperty data;
    private boolean wasOKed = false;
    private final InputValidator inputValidator;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(HttpHeaderDialog.class.getName());

    public HttpHeaderDialog(JDialog owner, HttpConfigurationProperty data) {
        super(owner, true);
        this.data = data;
        if (data == null) {
            this.data = new HttpConfigurationProperty();
        }
        inputValidator = new InputValidator(this, getTitle());
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle(bundle.getString("title"));
        nameLabel.setText(bundle.getString("label.name"));
        valueLabel.setText(bundle.getString("label.value"));

        inputValidator.constrainTextFieldToBeNonEmpty(bundle.getString("header.name.error"), nameTextField, null);
        inputValidator.constrainTextFieldToBeNonEmpty(bundle.getString("header.value.error"), valueTextField, null);

        cancelButton.addActionListener( e -> cancel());

        nameTextField.setText(data.getName());
        valueTextField.setText(data.getFullValue());
        inputValidator.attachToButton( okButton, e -> ok());
    }

    private void cancel() {
        cleanup();
    }

    public HttpConfigurationProperty getData() {
        return data;
    }

    public boolean wasOKed() {
        return wasOKed;
    }

    private void ok() {
        wasOKed = true;
        // save data
        String tmp = nameTextField.getText().trim();
        data.setName(tmp);
        tmp = valueTextField.getText().trim();
        data.setFullValue(tmp);
        cleanup();
    }

    private void cleanup() {
        dispose();
    }
}
