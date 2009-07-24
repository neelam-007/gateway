package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.AddCertificateWizard;
import com.l7tech.console.panels.CertDetailsPanel;
import com.l7tech.console.panels.CertImportMethodsPanel;
import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class NonSoapEncryptElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapEncryptElementAssertion> {
    private static final Logger logger = Logger.getLogger(NonSoapEncryptElementAssertionPropertiesDialog.class.getName());

    private JPanel mainPane;
    private JLabel recipientCertLabel;
    private JButton setRecipientCertificateButton;
    private JComboBox encryptionMethodComboBox;
    private JButton editXpathButton;
    private JLabel xpathExpressionLabel;

    private String certb64;

    public NonSoapEncryptElementAssertionPropertiesDialog(Frame parent, NonSoapEncryptElementAssertion assertion) {
        super(parent, assertion);
        initComponents();
        setData(assertion);
    }

    @Override
    protected JPanel createPropertyPanel() {
        Utilities.equalizeButtonSizes(editXpathButton, setRecipientCertificateButton);
        encryptionMethodComboBox.setModel(new DefaultComboBoxModel(new String[] {
                XencUtil.TRIPLE_DES_CBC,
                XencUtil.AES_128_CBC,
                XencUtil.AES_192_CBC,
                XencUtil.AES_256_CBC
        }));
        editXpathButton.addActionListener(makeEditXpathAction());
        setRecipientCertificateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final CertImportMethodsPanel sp = new CertImportMethodsPanel(
                                    new CertDetailsPanel(null) {
                                        @Override
                                        public boolean canFinish() {
                                            return true;
                                        }
                                    }, false);

                final AddCertificateWizard w = new AddCertificateWizard(NonSoapEncryptElementAssertionPropertiesDialog.this, sp);
                w.setTitle("Assign Certificate to Private Key");
                w.addWizardListener(new WizardAdapter() {
                    @Override
                    public void wizardFinished(WizardEvent we) {
                        Object o = w.getWizardInput();

                        if (o == null) return;
                        if (!(o instanceof TrustedCert)) {
                            // shouldn't happen
                            throw new IllegalStateException("Wizard returned a " + o.getClass().getName() + ", was expecting a " + TrustedCert.class.getName());
                        }

                        X509Certificate[] chain = sp.getCertChain();
                        if (chain.length < 1 || chain[0] == null)
                            return;

                        try {
                            certb64 = HexUtils.encodeBase64(chain[0].getEncoded());
                            updateRecipientCertLabel();
                        } catch (CertificateEncodingException e1) {
                            showCertError(e1);
                        }
                    }
                });

                w.pack();
                Utilities.centerOnScreen(w);
                DialogDisplayer.display(w);
            }

            private void showCertError(Throwable e) {
                String msg = "Error setting cert: " + ExceptionUtils.getMessage(e);
                e = ExceptionUtils.getDebugException(e);
                logger.log(Level.INFO, msg, e);
                DialogDisplayer.showMessageDialog(setRecipientCertificateButton, "Error Setting Certificate", msg, e);
            }
        });
        return mainPane;
    }

    @Override
    public void setData(NonSoapEncryptElementAssertion assertion) {
        super.setData(assertion);
        encryptionMethodComboBox.setSelectedItem(assertion.getXencAlgorithm());
        certb64 = assertion.getRecipientCertificateBase64();
        updateRecipientCertLabel();
    }

    @Override
    public void setXpathExpressionLabelText(String label) {
        if (this.xpathExpressionLabel == null)
            super.setXpathExpressionLabelText(label);
        else
            this.xpathExpressionLabel.setText(label);
    }

    private void updateRecipientCertLabel() {
        recipientCertLabel.setText(getCertInfo(certb64));
    }

    private String getCertInfo(String certb64) {
        if (certb64 == null || certb64.trim().length() < 1)
            return "<html><i>&lt;Not yet set&gt;";
        try {
            X509Certificate cert = CertUtils.decodeFromPEM(certb64, false);
            return cert.getSubjectX500Principal().getName(X500Principal.RFC2253);
        } catch (IOException e) {
            logger.log(Level.INFO, "Unable to decode recipient certificate Base-64: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return "<html><i>&lt;Invalid certificate Base-64&gt;";
        } catch (CertificateException e) {
            logger.log(Level.INFO, "Unable to parse recipient certificate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return "<html><i>&lt;Invalid X.509 certificate&gt;";
        }
    }

    @Override
    public NonSoapEncryptElementAssertion getData(NonSoapEncryptElementAssertion assertion) throws ValidationException {
        assertion = super.getData(assertion);
        assertion.setRecipientCertificateBase64(certb64);
        assertion.setXencAlgorithm((String)encryptionMethodComboBox.getSelectedItem());
        return assertion;
    }
}
