package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.external.assertions.xmlsec.HasVariablePrefix;
import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.external.assertions.xmlsec.NonSoapSecurityAssertionBase;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.xml.xpath.XpathExpression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NonSoapSecurityAssertionDialog<AT extends NonSoapSecurityAssertionBase> extends AssertionPropertiesOkCancelSupport<AT> {
    private JPanel contentPane;
    private JLabel xpathExpressionLabel;
    private JTextField xpathExpressionField;
    private JButton editXpathButton;
    private JPanel controlsBelowXpath;
    private JTextField variablePrefixField;
    private JLabel variablePrefixLabel;

    private final XpathExpression defaultXpathExpression;
    private XpathExpression xpathExpression;

    /**
     * Subclasses must call {@link #initComponents()} and {@link #setData}, and may override {@link #createPropertyPanel()}
     * to add additional components besides the XPath editor controls.
     *
     * @param owner      owner window.  required
     * @param assertion  assertion bean to edit.  required
     */
    public NonSoapSecurityAssertionDialog(Window owner, AT assertion) {
        //noinspection unchecked
        super((Class<AT>)assertion.getClass(), owner, assertion.getPropertiesDialogTitle(), true);
        editXpathButton.addActionListener(makeEditXpathAction());
        setXpathExpression(null);
        xpathExpressionLabel.setText("Element(s) to " + assertion.getVerb() + " XPath:");
        defaultXpathExpression = assertion.getDefaultXpathExpression();
    }

    protected ActionListener makeEditXpathAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final NonSoapEncryptElementAssertion holder = new NonSoapEncryptElementAssertion();
                holder.setXpathExpression(xpathExpression != null ? xpathExpression : defaultXpathExpression);
                final XpathBasedAssertionPropertiesDialog ape = new XpathBasedAssertionPropertiesDialog(NonSoapSecurityAssertionDialog.this, holder);
                JDialog dlg = ape.getDialog();
                dlg.setTitle(getTitle() + " - XPath Expression");
                dlg.pack();
                dlg.setModal(true);
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        if (!ape.isConfirmed())
                            return;
                        setXpathExpression(holder.getXpathExpression());
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

    /** @return the current XpathExpression in the view */
    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    /** @param xpathExpression the XpathExpression to change the view to */
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
        if (assertion instanceof HasVariablePrefix) {
            HasVariablePrefix hvp = (HasVariablePrefix) assertion;
            String variablePrefix = hvp.getVariablePrefix();
            variablePrefixField.setText(variablePrefix == null ? "" : variablePrefix);
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
        if (assertion instanceof HasVariablePrefix) {
            HasVariablePrefix hvp = (HasVariablePrefix) assertion;
            String variablePrefix = variablePrefixField.getText().trim();
            hvp.setVariablePrefix(variablePrefix.length() < 1 ? null : variablePrefix);
        }
        return assertion;
    }
}
