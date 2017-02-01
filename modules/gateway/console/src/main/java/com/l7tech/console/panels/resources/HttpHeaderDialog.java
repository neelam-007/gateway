package com.l7tech.console.panels.resources;

import com.l7tech.gateway.common.resources.HttpHeader;
import com.l7tech.gui.util.InputValidator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Ekta Khandelwal on 2016-06-20.
 */
public class HttpHeaderDialog extends JDialog {
    private JLabel nameLabel;
    private JTextField valueTextField;
    private JTextField nameTextField;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JLabel valueLabel;
    private HttpHeader data;
    private boolean wasOKed = false;
    private final InputValidator inputValidator;

    public HttpHeaderDialog(JDialog owner, HttpHeader data) {
        super(owner, true);
        this.data = data;
        if (data == null) {
            this.data = new HttpHeader();
        }
        inputValidator = new InputValidator(this, getTitle());
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Custom Header Setting");
        nameLabel.setText(" Name:");

        inputValidator.constrainTextFieldToBeNonEmpty("Header Name", nameTextField, null);
        inputValidator.constrainTextFieldToBeNonEmpty("Header Value", valueTextField, null);

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        nameTextField.setText(data.getName());
        valueTextField.setText(data.getFullValue());
        inputValidator.attachToButton( okButton, new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                ok();
            }
        } );
    }

    private void cancel() {
        dispose();
    }

    public HttpHeader getData() {
        return data;
    }

    public boolean wasOKed() {
        return wasOKed;
    }

    private void ok() {
        wasOKed = true;
        // save data
        String tmp = nameTextField.getText();
        if (tmp != null) tmp = tmp.trim();
        data.setName(tmp);
        tmp = valueTextField.getText();
        if (tmp != null) tmp = tmp.trim();
        data.setFullValue(tmp);
        cancel();
    }
}
