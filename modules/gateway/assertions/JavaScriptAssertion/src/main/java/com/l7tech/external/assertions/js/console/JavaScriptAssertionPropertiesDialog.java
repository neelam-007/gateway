package com.l7tech.external.assertions.js.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.js.JavaScriptAssertion;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;

import static com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants.*;

public class JavaScriptAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<JavaScriptAssertion> {

    private final InputValidator validator;

    private JPanel contentPane;
    private JTextArea scriptEditor;
    private JCheckBox executionTimeoutCheckBox;
    private JTextField executionTimeoutTextfield;
    private JCheckBox strictVariableSyntaxCheckBox;

    public JavaScriptAssertionPropertiesDialog( final Frame owner, final JavaScriptAssertion assertion ) {
        super( JavaScriptAssertion.class, owner, assertion, true );
        this.validator = new InputValidator(this, getTitle());

        initComponents();
    }

    @Override
    public void initComponents() {
        super.initComponents();

        executionTimeoutCheckBox.addActionListener(e -> updateEnableDisableExecutionTimeout());
        validator.constrainTextFieldToBeNonEmpty("Execution Timeout", executionTimeoutTextfield, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!Syntax.isAnyVariableReferenced(executionTimeoutTextfield.getText()) &&
                        !ValidationUtils.isValidInteger(executionTimeoutTextfield.getText(), false, 0, Integer.MAX_VALUE)) {
                    return "The value for the Execution Timeout must be a valid positive number or use context variables.";
                }

                return null;
            }
        });

        updateEnableDisableExecutionTimeout();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private void createUIComponents() {
        RSyntaxTextArea rs = new RSyntaxTextArea();
        scriptEditor = rs;

        Utilities.attachClipboardKeyboardShortcuts( scriptEditor );
        ActionMap am = scriptEditor.getActionMap();
        am.put( "paste-from-clipboard", ClipboardActions.getPasteAction() );
        am.put( "copy-to-clipboard", ClipboardActions.getCopyAction() );
        am.put( "cut-to-clipboard", ClipboardActions.getCutAction() );
        am.put( "paste", ClipboardActions.getPasteAction() );
        am.put( "copy", ClipboardActions.getCopyAction() );
        am.put( "cut", ClipboardActions.getCutAction() );

        rs.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT );
    }

    /**
     * Ensure correct button states through view configuration.
     */
    @Override
    protected void configureView() {
        super.configureView();
        updateEnableDisableExecutionTimeout();
    }

    @Override
    public void setData( final JavaScriptAssertion assertion ) {
        scriptEditor.setText(assertion.getScript());
        strictVariableSyntaxCheckBox.setSelected(assertion.isStrictModeEnabled());

        if (StringUtils.isBlank(assertion.getExecutionTimeout()) ||
                DEFAULT_EXECUTION_TIMEOUT_STRING.equals(assertion.getExecutionTimeout().trim())) {
            executionTimeoutCheckBox.setSelected(false);
            executionTimeoutTextfield.setText(DEFAULT_EXECUTION_TIMEOUT_STRING);
        } else {
            executionTimeoutCheckBox.setSelected(true);
            executionTimeoutTextfield.setText(assertion.getExecutionTimeout());
        }
    }

    @Override
    public JavaScriptAssertion getData( final JavaScriptAssertion assertion ) throws ValidationException {
        final String error = validator.validate();
        if (error != null) {
            throw new ValidationException(error);
        }

        assertion.setScript(scriptEditor.getText());
        assertion.setStrictModeEnabled(strictVariableSyntaxCheckBox.isSelected());
        assertion.setExecutionTimeout(executionTimeoutCheckBox.isSelected() ?
                executionTimeoutTextfield.getText().trim() : DEFAULT_EXECUTION_TIMEOUT_STRING);

        return assertion;
    }

    private void updateEnableDisableExecutionTimeout() {
        executionTimeoutTextfield.setEnabled(executionTimeoutCheckBox.isSelected());
    }
}
