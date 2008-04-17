package com.l7tech.external.assertions.ldapquery.console;

import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Something to edit a mapping between an LDAP attribute name and a context variable.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 7, 2007<br/>
 */
public class AttributeVariableMapDialog extends JDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton OKButton;
    private JTextField attributeName;
    private JTextField variableName;
    private JCheckBox multivaluedCheckBox;

    private QueryAttributeMapping data;
    private boolean wasOKed = false;
    InputValidator validator;

    public AttributeVariableMapDialog(Dialog owner, QueryAttributeMapping data) throws HeadlessException {
        super(owner, "Attribute Variable Mapping", true);
        this.data = data;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        validator = new InputValidator(this, "Attribute Variable Mapping");
        validator.disableButtonWhenInvalid(OKButton);
        validator.attachToButton(OKButton, new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                viewToModel(); 
                ok();
            }
        });

        validator.constrainTextFieldToBeNonEmpty("LDAP Attribute Name", attributeName, new InputValidator.ComponentValidationRule(attributeName) {
            public String getValidationError() {
                String val = attributeName.getText();
                if (val == null || val.trim().length() == 0)
                    return "LDAP Attribute Name cannot be empty";
                else
                    return null;
            }
        });

        validator.constrainTextFieldToBeNonEmpty("Context Variable Name", variableName , new InputValidator.ComponentValidationRule(variableName) {
            public String getValidationError() {
                String val = variableName.getText();
                if (val == null || val.trim().length() == 0)
                    return "Context Variable Name cannot be empty.";
                else if ( !ValidationUtils.isValidCharacters(variableName.getText().trim(), ValidationUtils.ALPHA_NUMERIC + "_-") ) {
                    return "Invalid character in name. Valid characters are letters, numbers '_' and '-'.";
                }
                return null;
            }
        });

        cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });
        modelToView();
        validator.validate();
    }

    private void ok() {
        String maybeattrval = attributeName.getText();
        if (maybeattrval == null || maybeattrval.length() < 1) {
            return;
        }
        String maybevarval = variableName.getText();
        try {
            if (maybevarval == null || maybevarval.length() < 1) {
                return;
            }
            maybevarval = maybevarval.trim();
            if (maybevarval.startsWith("$")) {
                maybevarval = maybevarval.substring(1);
            }
            if (maybevarval.startsWith("{")) {
                maybevarval = maybevarval.substring(1);
            }
            if (maybevarval.endsWith("}")) {
                maybevarval = maybevarval.substring(0, maybevarval.length() - 1);
            }
        } catch (Exception e) {
            return;
        }

        if (maybevarval == null || maybevarval.length() < 1) {
            return;
        }

        data.setAttributeName(maybeattrval);
        data.setMatchingContextVariableName(maybevarval);
        data.setMultivalued(multivaluedCheckBox.isSelected());

        wasOKed = true;
        dispose();
    }

    private void modelToView() {
        // update gui from assertion
        attributeName.setText(data.getAttributeName());
        variableName.setText(data.getMatchingContextVariableName());
        multivaluedCheckBox.setSelected(data.isMultivalued());
    }

    public void viewToModel() {
        data.setAttributeName(attributeName.getText().trim());
        data.setMatchingContextVariableName(variableName.getText().trim());
        data.setMultivalued(multivaluedCheckBox.isSelected());
    }

    public boolean isWasOKed() {
        return wasOKed;
    }
}
