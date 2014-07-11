package com.l7tech.external.assertions.jsonwebtoken.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;

/**
 * User: rseminoff
 * Date: 28/11/12
 */
public class TextOptionPanel extends JPanel {
    private JLabel optionLabel;
    private JPanel optPanel;
    private TargetVariablePanel variablePanel;
    private Assertion myAssertion;

    public TextOptionPanel() {
        super();
        optionLabel.setText("");
        variablePanel.setVariable("");
        variablePanel.setValueWillBeRead(true);
    }

    protected void setPanelWithValues(String title, String value) {
        optionLabel.setText(title);
        variablePanel.setVariable((value == null ? "" : value));
    }

    protected String getValueFromPanel() {
        String returnVar = variablePanel.getVariable();
        if (returnVar.startsWith(Syntax.SYNTAX_PREFIX)) {
            String[] names = Syntax.getReferencedNames(variablePanel.getVariable().trim());
            if (names.length >= 1) returnVar = names[0];    // Only the first variable is used.
        }
        return returnVar;
    }

    public void setAssertion(Assertion assertion, Assertion previousAssertion) {
        this.myAssertion = assertion;
        if (assertion != null) {
            variablePanel.setAssertion(this.myAssertion, previousAssertion);
        }
    }

    public Assertion getAssertion() {
        return myAssertion;
    }

    public void setVariableAsWritable() {
        variablePanel.setValueWillBeRead(false);
        variablePanel.setValueWillBeWritten(true);
    }

}
