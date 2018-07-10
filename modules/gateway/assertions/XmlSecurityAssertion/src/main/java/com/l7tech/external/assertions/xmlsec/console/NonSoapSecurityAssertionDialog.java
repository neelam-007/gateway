package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.external.assertions.xmlsec.HasVariablePrefix;
import com.l7tech.external.assertions.xmlsec.NonSoapCheckVerifyResultsAssertion;
import com.l7tech.external.assertions.xmlsec.NonSoapSecurityAssertionBase;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.xml.xpath.XpathExpression;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NonSoapSecurityAssertionDialog<AT extends NonSoapSecurityAssertionBase> extends AssertionPropertiesOkCancelSupport<AT> {
    private JPanel contentPane;
    private JLabel xpathExpressionLabel;
    private JTextField xpathExpressionField;
    private JButton editXpathButton;
    private JPanel controlsBelowXpath;
    private JPanel variablePrefixPanel;
    private TargetVariablePanel variablePrefixField;
    private JTextField variablePrefixTextField;
    private JLabel variablePrefixLabel;

    protected final AT assertion;
    private final XpathExpression defaultXpathExpression;
    private XpathExpression xpathExpression;

    /**
     * Subclasses must call {@link #initComponents()} and {@link #setData}, and may override {@link #createPropertyPanel()}
     * to add additional components besides the XPath editor controls.
     *
     * @param owner     owner window.  required
     * @param assertion assertion bean to edit.  required
     */
    public NonSoapSecurityAssertionDialog(Window owner, AT assertion) {
        //noinspection unchecked
        super((Class<AT>) assertion.getClass(), owner, assertion.getPropertiesDialogTitle(), true);
        editXpathButton.addActionListener(makeEditXpathAction());
        setXpathExpression(null);
        xpathExpressionLabel.setText("Element(s) to " + assertion.getVerb() + " XPath:");

        // Initial default is null (<Not Yet Set>); but the default we offer in the first opening of the "Config Xpath" dialog is the example Xpath
        defaultXpathExpression = new XpathExpression(assertion.getDefaultXpathExpressionString(), assertion.getDefaultNamespaceMap());


        variablePrefixPanel.setLayout(new BorderLayout());
        if (assertion instanceof NonSoapCheckVerifyResultsAssertion) {
            variablePrefixTextField = new JTextField();
            variablePrefixPanel.add(variablePrefixTextField, BorderLayout.CENTER);
            variablePrefixTextField.addActionListener(new RunOnChangeListener() {
                @Override
                public void run() {
                    updateState();
                }
            });
        } else {
            variablePrefixField = new TargetVariablePanel();
            variablePrefixPanel.setLayout(new BorderLayout());
            variablePrefixPanel.add(variablePrefixField, BorderLayout.CENTER);
            variablePrefixField.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    updateState();
                }
            });

        }

        this.assertion = assertion;
    }

    protected void updateState() {
        getOkButton().setEnabled(!isReadOnly() && inputsValid());
    }

    protected ActionListener makeEditXpathAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final XpathExpression oldAssertionXpath = assertion.getXpathExpression();
                if (assertion.getXpathExpression() == null) assertion.setXpathExpression(defaultXpathExpression);
                final XpathBasedAssertionPropertiesDialog ape = new XpathBasedAssertionPropertiesDialog(NonSoapSecurityAssertionDialog.this, assertion);
                JDialog dlg = ape.getDialog();
                dlg.setTitle(getTitle() + " - XPath Expression");
                dlg.pack();
                dlg.setModal(true);
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        final XpathExpression result = assertion.getXpathExpression();
                        assertion.setXpathExpression(oldAssertionXpath);
                        if (!ape.isConfirmed())
                            return;
                        setXpathExpression(result);
                    }
                });
            }
        };
    }


    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    protected JLabel getXpathExpressionLabel() {
        return xpathExpressionLabel;
    }

    protected JPanel getControlsBelowXpath() {
        return controlsBelowXpath;
    }

    /**
     * @return the current XpathExpression in the view
     */
    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    /**
     * @param xpathExpression the XpathExpression to change the view to
     */
    public void setXpathExpression(XpathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
        final String label = xpathExpression == null ? "<Not Yet Set>" : xpathExpression.getExpression();
        setXpathExpressionFieldText(label);
    }

    public void setXpathExpressionFieldText(String label) {
        xpathExpressionField.setText(label);
    }

    public String getXpathExpressionFieldText() {
        return xpathExpressionField.getText();
    }

    @Override
    public void setData(AT assertion) {
        setXpathExpression(assertion.getXpathExpression());
        if (assertion instanceof NonSoapCheckVerifyResultsAssertion) {
            variablePrefixTextField.setText(((NonSoapCheckVerifyResultsAssertion) assertion).getVariablePrefix());
        } else if (assertion instanceof HasVariablePrefix) {
            HasVariablePrefix hvp = (HasVariablePrefix) assertion;
            String variablePrefix = hvp.getVariablePrefix();
            variablePrefixField.setVariable(variablePrefix == null ? "" : variablePrefix);
            variablePrefixField.setAssertion(assertion, getPreviousAssertion());
            variablePrefixField.setSuffixes(hvp.suffixes());
            variablePrefixField.setAcceptEmpty(true);
            variablePrefixField.setVisible(true);
            variablePrefixLabel.setVisible(true);
        } else {
            variablePrefixField.setVisible(false);
            variablePrefixLabel.setVisible(false);
        }
    }

    @Override
    public AT getData(AT assertion) throws ValidationException {
        assertion.setXpathExpression(getXpathExpression());
        if (assertion instanceof NonSoapCheckVerifyResultsAssertion) {
            String variablePrefix = variablePrefixTextField.getText().trim();
            ((NonSoapCheckVerifyResultsAssertion) assertion).setVariablePrefix(variablePrefix.length() < 1 ? null : variablePrefix);
        } else if (assertion instanceof HasVariablePrefix) {
            HasVariablePrefix hvp = (HasVariablePrefix) assertion;
            String variablePrefix = variablePrefixField.getVariable().trim();
            hvp.setVariablePrefix(variablePrefix.length() < 1 ? null : variablePrefix);
        }
        return assertion;
    }

    @Override
    public void setPolicyPosition(PolicyPosition policyPosition) {
        super.setPolicyPosition(policyPosition);
        policyPositionUpdated();
    }

    protected void policyPositionUpdated() {
    }

    // for child dialogs to check if OK button should be enabled
    protected boolean inputsValid() {
        if (assertion instanceof NonSoapCheckVerifyResultsAssertion) {
            return !variablePrefixTextField.isVisible();
        } else {
            return (!variablePrefixField.isVisible() || variablePrefixField.isEntryValid());
        }
    }
}
