package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.external.assertions.saml2attributequery.SignResponseElementAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 28-Jan-2009
 * Time: 9:24:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class SignResponseElementAssertionPropertiesEditor implements AssertionPropertiesEditor<SignResponseElementAssertion> {
    protected boolean readOnly;
    private SignResponseElementAssertion assertion;
    //private XPathExpressionPanel dialog;
    private SignResponseElementAssertionPropertiesDialog dialog;
    private boolean confirmed = false;

    public SignResponseElementAssertionPropertiesEditor(SignResponseElementAssertion assertion) {
        this.assertion = assertion;
        createDialog();
    }

    public boolean isConfirmed() {
        /*if(dialog == null) {
            return false;
        } else {
            return !dialog.wasCanceled();
        }*/
        return confirmed;
    }

    public SignResponseElementAssertion getData(SignResponseElementAssertion ass) {
        ass.setXpathExpression(assertion.getXpathExpression());

        return ass;
    }

    public void setData(SignResponseElementAssertion ass) {
        assertion = ass;

        createDialog();
    }

    public JDialog getDialog() {
        confirmed = false;
        return dialog;
    }

    public Object getParameter( final String name ) {
        Object value = null;

        if ( PARAM_READONLY.equals( name )) {
            value = readOnly;
        }

        return value;
    }

    public void setParameter( final String name, Object value ) {
        if ( PARAM_READONLY.equals( name ) && value instanceof Boolean ) {
            readOnly = (Boolean) value;
        }
    }

    private void createDialog() {
        Frame mw = TopComponents.getInstance().getTopParent();

        AssertionTreeNode node = (AssertionTreeNode)TopComponents.getInstance().getPolicyTree().getModel().getRoot();

        dialog = new SignResponseElementAssertionPropertiesDialog(mw, true, node, assertion, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmed = true;
            }
        }, false, readOnly);

        /*String initialExpression = (assertion.getXpathExpression() == null) ? "" : assertion.getXpathExpression().getExpression();
        Map initialNamespaces = (assertion.getXpathExpression() == null) ? new HashMap<String, String>() : assertion.getXpathExpression().getNamespaces();
        dialog = new XPathExpressionPanel(mw, "XPath of element to sign", initialExpression, initialNamespaces);
        dialog.addWindowListener(new WindowListener() {
            public void windowActivated(WindowEvent evt) {
            }

            public void windowClosed(WindowEvent evt) {
            }

            public void windowClosing(WindowEvent evt) {
            }

            public void windowDeactivated(WindowEvent evt) {
                assertion.setXpathExpression(new XpathExpression(dialog.newXpathValue(), dialog.newXpathNamespaceMap()));
            }

            public void windowDeiconified(WindowEvent evt) {
            }

            public void windowIconified(WindowEvent evt) {
            }

            public void windowOpened(WindowEvent evt) {
            }
        });*/
    }
}
