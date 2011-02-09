package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.LookupOutboundSecureConversationSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author ghuang
 */
public class LookupOutboundSecureConversationSessionPropertiesDialog extends AssertionPropertiesEditorSupport<LookupOutboundSecureConversationSession> {
    private JPanel mainPanel;
    private JTextField serviceUrlTextField;
    private JPanel varPrefixPanel;
    private JButton okButton;
    private JButton cancelButton;
    private TargetVariablePanel varPrefixTextField;

    private LookupOutboundSecureConversationSession assertion;
    private boolean confirmed;

    public LookupOutboundSecureConversationSessionPropertiesDialog(Window owner, LookupOutboundSecureConversationSession assertion) {
        super(owner, assertion);
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(LookupOutboundSecureConversationSession assertion) {
        this.assertion = assertion;
        modelToView();
    }

    @Override
    public LookupOutboundSecureConversationSession getData(LookupOutboundSecureConversationSession assertion) {
        viewToModel(assertion);
        return assertion;
    }

    private void initialize() {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            protected void run() {
                enableOrDisableComponents();
            }
        };

        serviceUrlTextField.getDocument().addDocumentListener(enableDisableListener);

        varPrefixTextField = new TargetVariablePanel();
        varPrefixTextField.addChangeListener(enableDisableListener);

        varPrefixPanel.setLayout(new BorderLayout());
        varPrefixPanel.add(varPrefixTextField, BorderLayout.CENTER);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });


    }

    private void modelToView() {
        serviceUrlTextField.setText(assertion.getServiceUrl());

        varPrefixTextField.setSuffixes(assertion.getVariableSuffixes());
        varPrefixTextField.setVariable(assertion.getVariablePrefix());
        varPrefixTextField.setAssertion(assertion,getPreviousAssertion());
    }

    private void viewToModel(LookupOutboundSecureConversationSession assertion) {
        assertion.setServiceUrl(serviceUrlTextField.getText());

        String prefix = varPrefixTextField.getVariable();
        if (prefix == null || prefix.trim().isEmpty()) prefix = LookupOutboundSecureConversationSession.DEFAULT_VARIABLE_PREFIX;
        assertion.setVariablePrefix(prefix);
    }

    private void enableOrDisableComponents() {
        final String serviceUrl = serviceUrlTextField.getText();

        final boolean okEnabled = 
            (serviceUrl != null && !serviceUrl.trim().isEmpty()) && // Check Service URL
            varPrefixTextField.isEntryValid();                      // Check Variable Prefix

        okButton.setEnabled(okEnabled);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}