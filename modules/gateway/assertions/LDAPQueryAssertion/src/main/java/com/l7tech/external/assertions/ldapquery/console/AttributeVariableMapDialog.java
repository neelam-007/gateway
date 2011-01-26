package com.l7tech.external.assertions.ldapquery.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.ldapquery.LDAPQueryAssertion;
import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
    private JPanel variableNamePanel;
    private TargetVariablePanel variableName;
    private JComboBox multivaluedComboBox;

    private QueryAttributeMapping data;
    private boolean wasOKed = false;
    InputValidator validator;

    private static final String MV_USE_FIRST = "Use first value";
    private static final String MV_JOIN_COMMAS = "Join with commas";
    private static final String MV_MULTIVALUED = "Set multivalued context variable";
    private static final String MV_FAIL = "Fail assertion";
    private static final String[] MV_MODEL = { MV_USE_FIRST, MV_JOIN_COMMAS, MV_MULTIVALUED, MV_FAIL };

    public AttributeVariableMapDialog(Dialog owner, QueryAttributeMapping data, final LDAPQueryAssertion assertion) throws HeadlessException {
        super(owner, "Attribute Variable Mapping", true);
        this.data = data;
        initialize();
        variableName.setAssertion(assertion);
    }

    private void initialize() {
        setContentPane(mainPanel);
        Utilities.setEscKeyStrokeDisposes( this );

        variableName = new TargetVariablePanel();
        variableNamePanel.setLayout(new BorderLayout());
        variableNamePanel.add(variableName, BorderLayout.CENTER);
        variableName.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                OKButton.setEnabled(variableName.isEntryValid());
            }
        });

        validator = new InputValidator(this, "Attribute Variable Mapping");
        validator.disableButtonWhenInvalid(OKButton);
        validator.attachToButton(OKButton, new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                viewToModel(); 
                ok();
            }
        });

        validator.constrainTextFieldToBeNonEmpty("LDAP Attribute Name", attributeName, new InputValidator.ComponentValidationRule(attributeName) {
            @Override
            public String getValidationError() {
                String val = attributeName.getText();
                if (val == null || val.trim().length() == 0)
                    return "LDAP Attribute Name cannot be empty";
                else
                    return null;
            }
        });

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
        String maybeattrval = attributeName.getText();
        if (maybeattrval == null || maybeattrval.length() < 1) {
            return;
        }


        if (!variableName.isEntryValid()) {
            return;
        }

        data.setAttributeName(maybeattrval);
        data.setMatchingContextVariableName(variableName.getVariable());
        viewToModelMultivaluedFlags();

        wasOKed = true;
        dispose();
    }

    private void modelToView() {
        // update gui from assertion
        attributeName.setText(data.getAttributeName()); 
        variableName.setVariable(data.getMatchingContextVariableName());
        multivaluedComboBox.setModel(new DefaultComboBoxModel(MV_MODEL));
        String mv = MV_USE_FIRST;
        if ( data.isMultivalued() ) {
            mv = data.isJoinMultivalued() ? MV_JOIN_COMMAS : MV_MULTIVALUED;
        } else if ( data.isFailMultivalued() ) {
            mv = MV_FAIL;
        }
        multivaluedComboBox.setSelectedItem(mv);
    }

    public void viewToModel() {
        data.setAttributeName(attributeName.getText().trim());
        data.setMatchingContextVariableName(variableName.getVariable());
        viewToModelMultivaluedFlags();
    }

    private void viewToModelMultivaluedFlags() {
        Object mv = multivaluedComboBox.getSelectedItem();
        if (MV_MULTIVALUED.equals(mv)) {
            data.setMultivalued(true);
            data.setJoinMultivalued(false);
            data.setFailMultivalued(false);
        } else if (MV_JOIN_COMMAS.equals(mv)) {
            data.setMultivalued(true);
            data.setJoinMultivalued(true);
            data.setFailMultivalued(false);
        } else if (MV_FAIL.equals(mv)) {
            data.setMultivalued(false);
            data.setJoinMultivalued(false);
            data.setFailMultivalued(true);
        } else { // MV_USE_FIRST
            data.setMultivalued(false);
            data.setJoinMultivalued(false);
            data.setFailMultivalued(false);
        }
    }

    public boolean isWasOKed() {
        return wasOKed;
    }
}
