package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;
import com.l7tech.xml.xpath.XpathVersion;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class XpathCredentialSourcePropertiesDialog extends LegacyAssertionPropertyDialog {

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.XpathCredentialSourcePropertiesDialog");

    private XpathCredentialSource xpathCredsAssertion;
    private boolean assertionChanged = false;
    private boolean readOnly = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JTextField loginXpathField;
    private JTextField passwordXpathField;
    private JCheckBox removeLoginCheckbox;
    private JCheckBox removePasswordCheckbox;
    private JButton editLoginXPathButton;
    private JButton editPasswordXPathButton;
    private Map<String,String> loginNamespaces;
    private Map<String,String> passwordNamespaces;

    public XpathCredentialSourcePropertiesDialog(XpathCredentialSource assertion, Frame owner, boolean modal, boolean readOnly) throws HeadlessException {
        super(owner, assertion, modal);
        setTitle(resources.getString("dialog.title.xpath.credentials.props"));
        this.setContentPane( mainPanel );

        this.xpathCredsAssertion = assertion;
        this.readOnly = readOnly;

        loginNamespaces = initNamespaces( xpathCredsAssertion.getXpathExpression() );
        passwordNamespaces = initNamespaces( xpathCredsAssertion.getPasswordExpression() );

        XpathExpression loginExpr = assertion.getXpathExpression();
        XpathExpression passExpr = assertion.getPasswordExpression();

        if (loginExpr != null && loginExpr.getExpression() != null) {
            loginXpathField.setText(loginExpr.getExpression());
            loginXpathField.setCaretPosition( 0 );
        }

        if (passExpr != null && passExpr.getExpression() != null) {
            passwordXpathField.setText(passExpr.getExpression());
            passwordXpathField.setCaretPosition( 0 );
        }

        removeLoginCheckbox.setSelected(assertion.isRemoveLoginElement());
        removePasswordCheckbox.setSelected(assertion.isRemovePasswordElement());

        editLoginXPathButton.addActionListener( makeEditXpathAction(resources.getString("Login"), loginXpathField, loginNamespaces) );
        editPasswordXPathButton.addActionListener( makeEditXpathAction(resources.getString("Password"), passwordXpathField, passwordNamespaces) );

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                xpathCredsAssertion.setXpathExpression(new XpathExpression(loginXpathField.getText(), loginNamespaces));
                xpathCredsAssertion.setPasswordExpression(new XpathExpression(passwordXpathField.getText(), passwordNamespaces));
                xpathCredsAssertion.setRemoveLoginElement(removeLoginCheckbox.isSelected());
                xpathCredsAssertion.setRemovePasswordElement(removePasswordCheckbox.isSelected());
                assertionChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                xpathCredsAssertion = null;
                dispose();
            }
        });

        updateButtons();

        pack();
    }

    private Map<String, String> initNamespaces( final XpathExpression xpathExpression ) {
        Map<String, String> namespaces = null;

        if ( xpathExpression != null ) {
            namespaces = xpathExpression.getNamespaces();
        }
        if ( namespaces == null ) {
            namespaces = new HashMap<String,String>();
        }

        return namespaces;
    }

    protected ActionListener makeEditXpathAction( final String name,
                                                  final JTextComponent xpathTextComponent,
                                                  final Map<String,String> namespaces ) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final XpathBasedAssertion holder = new XpathBasedAssertion(){
                    @Override
                    public CompositeAssertion getParent() {
                        return xpathCredsAssertion.getParent();
                    }
                };
                holder.setXpathExpression( new XpathExpression(xpathTextComponent.getText(), namespaces) );
                final XpathBasedAssertionPropertiesDialog ape = new XpathBasedAssertionPropertiesDialog(XpathCredentialSourcePropertiesDialog.this, holder);
                JDialog dlg = ape.getDialog();
                dlg.setTitle(getTitle() + " - "+name+ " " +resources.getString("xpath.expression"));
                dlg.pack();
                dlg.setModal(true);
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        if ( ape.isConfirmed() ) {
                            xpathTextComponent.setText( holder.getXpathExpression().getExpression() );
                            xpathTextComponent.setCaretPosition( 0 );
                            namespaces.clear();
                            namespaces.putAll( holder.getXpathExpression().getNamespaces() );
                            updateButtons();
                        }
                    }
                });
            }
        };
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    private XpathVersion getXpathVersion() {
        // For now this assertion only exposes XPath 1.0 and does not provide any way to select XPath 2.0
        // TODO update GUI to expose XPath version control
        return XpathVersion.XPATH_1_0;
    }

    private void updateButtons() {
        boolean ok;
        try {
            XpathUtil.validate(loginXpathField.getText(), getXpathVersion(), loginNamespaces);
            XpathUtil.validate(passwordXpathField.getText(), getXpathVersion(), passwordNamespaces);
            ok = !readOnly;
        } catch (Exception e) {
            ok = false;
        }
        okButton.setEnabled(ok);
    }
}
