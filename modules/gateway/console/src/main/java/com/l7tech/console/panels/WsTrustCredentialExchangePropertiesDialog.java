package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.WsTrustRequestType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author alex
 */
public class WsTrustCredentialExchangePropertiesDialog extends LegacyAssertionPropertyDialog {
    private WsTrustCredentialExchange wsTrustAssertion;
    private boolean assertionChanged = false;
    private boolean readOnly = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JComboBox requestTypeCombo;
    private JTextField appliesToField;
    private JTextField tokenServiceUrlField;
    private JTextField issuerField;
    private JComboBox wsTrustNsComboBox;

    public WsTrustCredentialExchangePropertiesDialog(WsTrustCredentialExchange assertion, Frame owner, boolean modal, boolean readOnly) throws HeadlessException {
        super(owner, assertion, modal);
        this.wsTrustAssertion = assertion;
        this.readOnly = readOnly;

        wsTrustNsComboBox.setModel( new DefaultComboBoxModel( new Object[]{
                "<Not Specified>",
                SoapConstants.WST_NAMESPACE3,
                SoapConstants.WST_NAMESPACE2,
                SoapConstants.WST_NAMESPACE,
        } ) );
        wsTrustNsComboBox.setRenderer( TextListCellRenderer.<Object>basicComboBoxRenderer() );
        if ( assertion.getWsTrustNamespace() != null ) {
            wsTrustNsComboBox.setSelectedItem( assertion.getWsTrustNamespace() );
            if ( wsTrustNsComboBox.getSelectedItem() == null ) {
                wsTrustNsComboBox.setSelectedIndex( 0 );   
            }
        }

        requestTypeCombo.setModel(new DefaultComboBoxModel(new WsTrustRequestType[] {WsTrustRequestType.ISSUE, WsTrustRequestType.VALIDATE}));

        WsTrustRequestType type = assertion.getRequestType();
        if (type == null) {
            requestTypeCombo.setSelectedIndex(0);
        } else {
            requestTypeCombo.setSelectedItem(type);
        }
        tokenServiceUrlField.setText(assertion.getTokenServiceUrl());
        appliesToField.setText(assertion.getAppliesTo());
        issuerField.setText(assertion.getIssuer());

        getContentPane().add(mainPanel);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String wsTrustNs = (String) wsTrustNsComboBox.getSelectedItem();
                wsTrustAssertion.setWsTrustNamespace( wsTrustNs.startsWith("<" ) ? null : wsTrustNs );
                wsTrustAssertion.setAppliesTo(appliesToField.getText());
                wsTrustAssertion.setIssuer(issuerField.getText());
                wsTrustAssertion.setTokenServiceUrl(tokenServiceUrlField.getText());
                wsTrustAssertion.setRequestType((WsTrustRequestType)requestTypeCombo.getSelectedItem());
                assertionChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wsTrustAssertion = null;
                dispose();
            }
        });

        final RunOnChangeListener updateListener = new RunOnChangeListener() {
            @Override
            public void run() {
                updateButtons();
            }
        };

        tokenServiceUrlField.getDocument().addDocumentListener(updateListener);
        appliesToField.getDocument().addDocumentListener(updateListener);
        issuerField.getDocument().addDocumentListener(updateListener);

        pack();
        updateButtons();
        setMinimumSize( getContentPane().getMinimumSize() );
        Utilities.centerOnParentWindow( this );
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    private void updateButtons() {
        boolean ok;
        String url = tokenServiceUrlField.getText();
        ok = url != null && url.length() > 0;
        try {
            new URL(url);
            ok = ok && !readOnly;
        } catch (MalformedURLException e) {
            ok = false;
        }
        okButton.setEnabled(ok);
    }
}
