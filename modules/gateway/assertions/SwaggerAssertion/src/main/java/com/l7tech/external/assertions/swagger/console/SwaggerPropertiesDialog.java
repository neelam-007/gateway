package com.l7tech.external.assertions.swagger.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;

/**
 * Created by moiyu01 on 15-07-24.
 */
public class SwaggerPropertiesDialog extends AssertionPropertiesOkCancelSupport<SwaggerAssertion> {

    private JPanel mainPanel;
    private JPanel prefixPanel;
    private TargetVariablePanel swaggerPrefix;
    private TargetVariablePanel swaggerDocumentVariable;
    private JCheckBox validateMethodCheckBox;
    private JCheckBox validatePathCheckBox;
    private JCheckBox validateSchemeCheckBox;
    private JCheckBox validateRequestArgumentsCheckBox;
    private JCheckBox requireSecurityCredentialsToCheckBox;
    private TargetMessagePanel targetMessagePanel = new TargetMessagePanel();

    public SwaggerPropertiesDialog(final Window parent, final SwaggerAssertion assertion) {
        super(SwaggerAssertion.class, parent, assertion, true);
        initComponents(assertion);
    }

    @Override
    public void setData(SwaggerAssertion assertion) {
        if(assertion.getSwaggerDoc() != null) {
            swaggerDocumentVariable.setVariable(assertion.getSwaggerDoc());
        }
        validateMethodCheckBox.setSelected(assertion.isValidateMethod());
        validatePathCheckBox.setSelected(assertion.isValidatePath());
        validateSchemeCheckBox.setSelected(assertion.isValidateScheme());
        validateRequestArgumentsCheckBox.setSelected(assertion.isValidateRequestArguments());
        requireSecurityCredentialsToCheckBox.setSelected(assertion.isRequireSecurityCredentials());
        swaggerPrefix.setVariable(assertion.getPrefix());

        updateTargetModel(assertion);
    }

    @Override
    public SwaggerAssertion getData(SwaggerAssertion assertion) throws ValidationException {
        if ( !targetMessagePanel.isValidTarget() ) {
            throw new ValidationException("Invalid Target Message: " + targetMessagePanel.check());
        }

        assertion.setSwaggerDoc(swaggerDocumentVariable.getVariable());
        assertion.setRequireSecurityCredentials(requireSecurityCredentialsToCheckBox.isSelected());
        assertion.setValidateMethod(validateMethodCheckBox.isSelected());
        assertion.setValidatePath(validatePathCheckBox.isSelected());
        assertion.setValidateScheme(validateSchemeCheckBox.isSelected());
        assertion.setValidateRequestArguments(validateRequestArgumentsCheckBox.isSelected());
        assertion.setPrefix(swaggerPrefix.getVariable());
        targetMessagePanel.updateModel(assertion);
        return assertion;
    }


    protected void initComponents(final SwaggerAssertion assertion) {
        validateMethodCheckBox.setSelected(true);
        validatePathCheckBox.setSelected(true);
        validateRequestArgumentsCheckBox.setSelected(true);
        validateSchemeCheckBox.setSelected(true);
        requireSecurityCredentialsToCheckBox.setSelected(true);
        super.initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    private void createUIComponents() {
        prefixPanel = new TargetVariablePanel();
        swaggerDocumentVariable = new TargetVariablePanel();
    }


    private void updateTargetModel(final SwaggerAssertion assertion) {
        targetMessagePanel.setModel(new MessageTargetableAssertion() {{
            TargetMessageType targetMessageType = assertion.getTarget();
            if ( targetMessageType != null ) {
                setTarget(targetMessageType);
            } else {
                clearTarget();
            }
            setOtherTargetMessageVariable(assertion.getOtherTargetMessageVariable());
        }},getPreviousAssertion());
    }
}
