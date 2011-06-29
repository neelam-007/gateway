package com.l7tech.external.assertions.api3scale.console;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * Something to edit a transaction usage map
 */
public class UsageDialog extends JDialog {
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(UsageDialog.class.getName());

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton OKButton;
    private JTextField metricTextField;
    private JTextField valueTextField;

    private String metric, value;
    private boolean wasOKed = false;
    InputValidator validator;


    public UsageDialog(Dialog owner, String metric, String value) throws HeadlessException {
        super(owner, "Transaction", true);
        this.metric = metric;
        this.value = value;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        Utilities.setEscKeyStrokeDisposes( this );

        validator = new InputValidator(this, "Usage");
        validator.disableButtonWhenInvalid(OKButton);
        validator.attachToButton(OKButton, new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                viewToModel(); 
                ok();
            }
        });

        validator.constrainTextFieldToBeNonEmpty(getPropertyValue("metric"), metricTextField, null);
        validator.constrainTextFieldToBeNonEmpty(getPropertyValue("value"), valueTextField, null);

        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });
        modelToView();
        validator.validate();
    }

    private void ok() {
        wasOKed = true;
        dispose();
    }

    private void modelToView() {
        metricTextField.setText(metric);
        valueTextField.setText(value);
    }

    public void viewToModel() {        
        validator.validate();

        value = valueTextField.getText();
        metric = metricTextField.getText();
    }

    public boolean isWasOKed() {
        return wasOKed;
    }


    private String getPropertyValue(String propKey){
        String propertyName = resourceBundle.getString(propKey);
        if(propertyName.charAt(propertyName.length() - 1) == ':'){
            propertyName = propertyName.substring(0, propertyName.length() - 1);
        }
        return propertyName;
    }

    public String getMetric() {
        return metric;
    }

    public String getValue(){
        return value;
    }
}
