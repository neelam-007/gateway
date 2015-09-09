package com.l7tech.external.assertions.swagger.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * Created by moiyu01 on 15-07-24.
 */
public class SwaggerPropertiesDialog extends AssertionPropertiesOkCancelSupport<SwaggerAssertion> {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SwaggerPropertiesDialog.class.getName());
       
    private JPanel mainPanel;
    private TargetVariablePanel swaggerPrefix;
    private TargetVariablePanel swaggerDocumentVariable;
    private JCheckBox validateMethodCheckBox;
    private JCheckBox validatePathCheckBox;
    private JCheckBox validateSchemeCheckBox;
    private JCheckBox requireSecurityCredentialsToCheckBox;
    private JTextField serviceBaseTextField;

    public SwaggerPropertiesDialog(final Window parent, final SwaggerAssertion assertion) {
        super(SwaggerAssertion.class, parent, assertion, true);
        initComponents(assertion);
    }

    @Override
    public void setData(SwaggerAssertion assertion) {
        if(assertion.getSwaggerDoc() != null) {
            swaggerDocumentVariable.setVariable(assertion.getSwaggerDoc());
        }
        if(assertion.getServiceBase() != null) {
            serviceBaseTextField.setText(assertion.getServiceBase());
        }
        validateMethodCheckBox.setSelected(assertion.isValidateMethod());
        validatePathCheckBox.setSelected(assertion.isValidatePath());
        validateSchemeCheckBox.setSelected(assertion.isValidateScheme());
        requireSecurityCredentialsToCheckBox.setSelected(assertion.isRequireSecurityCredentials());
        swaggerPrefix.setVariable(assertion.getPrefix());
    }

    @Override
    public SwaggerAssertion getData(SwaggerAssertion assertion) throws ValidationException {
        assertion.setSwaggerDoc(swaggerDocumentVariable.getVariable());
        assertion.setServiceBase(serviceBaseTextField.getText());
        assertion.setRequireSecurityCredentials(requireSecurityCredentialsToCheckBox.isSelected());
        assertion.setValidateMethod(validateMethodCheckBox.isSelected());
        assertion.setValidatePath(validatePathCheckBox.isSelected());
        assertion.setValidateScheme(validateSchemeCheckBox.isSelected());
        assertion.setPrefix(swaggerPrefix.getVariable());
        return assertion;
    }

    protected void initComponents(final SwaggerAssertion assertion) {
        super.initComponents();
        swaggerDocumentVariable.setAcceptEmpty(false);
        swaggerPrefix.setAcceptEmpty(false);

        swaggerPrefix.setAssertion(assertion, getPreviousAssertion());
        swaggerDocumentVariable.setAssertion(assertion, getPreviousAssertion());

        validateMethodCheckBox.setSelected(true);
        validatePathCheckBox.setSelected(true);
        validateSchemeCheckBox.setSelected(true);
        requireSecurityCredentialsToCheckBox.setSelected(true);

        InputValidator validator = new InputValidator(this, getTitle());
        validator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!swaggerDocumentVariable.isEntryValid()) {
                    return resourceBundle.getString("swaggerDocumentInvalidEntryErrMsg");
                }
                return null;
            }
        });

        validator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!swaggerPrefix.isEntryValid()) {
                    return resourceBundle.getString("variablePrefixInvalidErrMsg");
                }
                return null;
            }
        });

        validator.attachToButton(getOkButton(), super.createOkAction());

        RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        };

        validatePathCheckBox.addChangeListener(enableDisableListener);
        validateMethodCheckBox.addChangeListener(enableDisableListener);
    }

    private void enableDisableComponents() {
        validateMethodCheckBox.setEnabled(validatePathCheckBox.isSelected());
        validateSchemeCheckBox.setEnabled(validateMethodCheckBox.isSelected());
        requireSecurityCredentialsToCheckBox.setEnabled(validateMethodCheckBox.isSelected());

        if(!validatePathCheckBox.isSelected()) {
            validateMethodCheckBox.setSelected(false);
        }
        if(!validateMethodCheckBox.isSelected()){
            validateSchemeCheckBox.setSelected(false);
            requireSecurityCredentialsToCheckBox.setSelected(false);
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected ActionListener createOkAction() {
        return new RunOnChangeListener();
    }

}
