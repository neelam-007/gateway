package com.l7tech.external.assertions.ldapquery.console;

import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    private JComboBox multivaluedComboBox;

    private QueryAttributeMapping data;
    private boolean wasOKed = false;
    InputValidator validator;

    private static final String MV_USE_FIRST = "Use first value";
    private static final String MV_JOIN_COMMAS = "Join with commas";
    private static final String MV_MULTIVALUED = "Set multi-valued context variable";
    private static final String[] MV_MODEL = { MV_USE_FIRST, MV_JOIN_COMMAS, MV_MULTIVALUED };

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
        viewToModelMultivaluedFlags();

        wasOKed = true;
        dispose();
    }

    private void modelToView() {
        // update gui from assertion
        attributeName.setText(data.getAttributeName());
        variableName.setText(data.getMatchingContextVariableName());
        multivaluedComboBox.setModel(new DefaultComboBoxModel(MV_MODEL));
        String mv = MV_USE_FIRST;
        if (data.isMultivalued())
            mv = data.isJoinMultivalued() ? MV_JOIN_COMMAS : MV_MULTIVALUED;
        multivaluedComboBox.setSelectedItem(mv);
    }

    public void viewToModel() {
        data.setAttributeName(attributeName.getText().trim());
        data.setMatchingContextVariableName(variableName.getText().trim());
        viewToModelMultivaluedFlags();
    }

    private void viewToModelMultivaluedFlags() {
        Object mv = multivaluedComboBox.getSelectedItem();
        if (MV_MULTIVALUED.equals(mv)) {
            data.setMultivalued(true);
            data.setJoinMultivalued(false);
        } else if (MV_JOIN_COMMAS.equals(mv)) {
            data.setMultivalued(true);
            data.setJoinMultivalued(true);
        } else { // MV_USE_FIRST
            data.setMultivalued(false);
            data.setJoinMultivalued(false);
        }
    }

    public boolean isWasOKed() {
        return wasOKed;
    }
}
