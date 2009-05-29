package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.gui.widgets.ValidatedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Properties editing panel for WSS X.509 certificate assertion.
 */
public class WssX509CertPanel extends ValidatedPanel<RequestWssX509Cert> {

    //- PUBLIC

    public WssX509CertPanel( final RequestWssX509Cert model ) {
        this.assertion = model;
        init();
    }

    @Override
    public void focusFirstComponent() {
        allowMultipleSignatures.requestFocus();
    }

    //- PROTECTED

    @Override
    protected void initComponents() {
        // Set initial component values
        allowMultipleSignatures.setSelected( assertion.isAllowMultipleSignatures() );
        allowMultipleSignatures.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkSyntax();
            }
        });
        // Add main panel
        add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    protected void doUpdateModel() {
        assertion.setAllowMultipleSignatures( allowMultipleSignatures.isSelected() );
    }

    @Override
    protected RequestWssX509Cert getModel() {
        return assertion;
    }

    //- PRIVATE

    private JPanel mainPanel;
    private JCheckBox allowMultipleSignatures;
    private final RequestWssX509Cert assertion;    
}
