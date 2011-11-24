package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.ssh.SshCredentialAssertion;

import javax.swing.*;
import java.awt.*;

public class SshCredentialAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<SshCredentialAssertion> {

    private JPanel contentPane;
    private JCheckBox passwordCheckBox;
    private JCheckBox publicKeyCheckBox;

    public SshCredentialAssertionPropertiesDialog(Window owner, SshCredentialAssertion assertion) {
        super(SshCredentialAssertion.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(SshCredentialAssertion assertion) {
        Boolean permitPassword = assertion.getPermitPasswordCredential();
        passwordCheckBox.setSelected(permitPassword == null || permitPassword);
        Boolean permitPublicKey = assertion.getPermitPublicKeyCredential();
        publicKeyCheckBox.setSelected(permitPublicKey == null || permitPublicKey);
    }

    @Override
    public SshCredentialAssertion getData(SshCredentialAssertion assertion) throws ValidationException {
        assertion.setPermitPasswordCredential(passwordCheckBox.isSelected());
        assertion.setPermitPublicKeyCredential(publicKeyCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
