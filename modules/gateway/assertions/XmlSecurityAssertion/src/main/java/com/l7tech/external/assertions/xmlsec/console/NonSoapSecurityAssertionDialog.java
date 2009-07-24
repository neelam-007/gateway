package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.external.assertions.xmlsec.NonSoapSecurityAssertionBase;
import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.xml.xpath.XpathExpression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class NonSoapSecurityAssertionDialog<AT extends NonSoapSecurityAssertionBase> extends AssertionPropertiesOkCancelSupport<AT> {
    private JPanel contentPane;
    private JLabel xpathExpressionLabelLabel;
    private JLabel xpathExpressionLabel;
    private JButton editXpathButton;

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
        super((Class<AT>)assertion.getClass(), owner, String.valueOf(assertion.meta().get(AssertionMetadata.SHORT_NAME)) + " Properties", true);
        editXpathButton.addActionListener(makeEditXpathAction());
        setXpathExpression(null);
    }

    protected ActionListener makeEditXpathAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final NonSoapEncryptElementAssertion holder = new NonSoapEncryptElementAssertion();
                holder.setXpathExpression(xpathExpression);
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

    protected JLabel getXpathExpressionLabelLabel() {
        return xpathExpressionLabelLabel;
    }

    /** @return the current XpathExpression in the view */
    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    /** @param xpathExpression the XpathExpression to change the view to */
    public void setXpathExpression(XpathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
        final String label = xpathExpression == null ? "<html><i>&lt;Not yet set&gt;" : xpathExpression.getExpression();
        setXpathExpressionLabelText(label);
    }

    public void setXpathExpressionLabelText(String label) {
        xpathExpressionLabel.setText(label);
    }

    public String getXpathExpressionLabelText() {
        return xpathExpressionLabel.getText();
    }

    @Override
    public void setData(AT assertion) {
        setXpathExpression(assertion.getXpathExpression());
    }

    @Override
    public AT getData(AT assertion) throws ValidationException {
        assertion.setXpathExpression(getXpathExpression());
        return assertion;
    }
}
