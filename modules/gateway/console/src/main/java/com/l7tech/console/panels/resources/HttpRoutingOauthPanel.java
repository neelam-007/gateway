package com.l7tech.console.panels.resources;

import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for configuring OAuth authentication for HTTP Routing dialog.
 */
public class HttpRoutingOauthPanel extends JPanel {
    private JPanel mainPanel;

    private JComboBox<String> oauthVersionComboBox;
    private JTextField tokenVariableField;

    private final HttpRoutingAssertion assertion;

    public HttpRoutingOauthPanel( HttpRoutingAssertion assertion ) {
        this.assertion = assertion;
        setLayout( new BorderLayout() );
        add( mainPanel, BorderLayout.CENTER );

        String oauthVersion = assertion.getAuthOauthVersion();
        if ( !"1.0".equals( oauthVersion ) ) {
            oauthVersion = "2.0";
        }
        oauthVersionComboBox.setSelectedItem( oauthVersion );
        tokenVariableField.setText( assertion.getAuthOauthTokenVar() );
    }

    public void updateModel() {
        assertion.setAuthOauthVersion( (String) oauthVersionComboBox.getSelectedItem() );
        assertion.setAuthOauthTokenVar( Syntax.stripSyntax( tokenVariableField.getText() ) );
    }

    public String getTokenVariable() {
        return tokenVariableField.getText();
    }
}
