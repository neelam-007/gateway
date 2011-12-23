package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
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
 * Panel for editing
 */
public class XmlElementEncryptionConfigPanel extends ValidatedPanel<XmlElementEncryptionConfig> {
    private static final Logger logger = Logger.getLogger(XmlElementEncryptionConfigPanel.class.getName());

    private JPanel contentPane;
    private JComboBox encryptionMethodComboBox;
    private JLabel recipientCertLabel;
    private JButton setRecipientCertificateButton;
    private JRadioButton specifyCertificateRadioButton;
    private JRadioButton useContextVariableRadioButton;
    private TargetVariablePanel contextVariableField;
    private String certb64;

    private XmlElementEncryptionConfig model;

    public XmlElementEncryptionConfigPanel(XmlElementEncryptionConfig model) {
        this.model = model;
        init();
        setData(model);
    }

    public XmlElementEncryptionConfigPanel() {
        this.model = new XmlElementEncryptionConfig();
        init();
        setData(model);
    }

    @Override
    protected XmlElementEncryptionConfig getModel() {
        return model;
    }

    @Override
    protected void initComponents() {
        encryptionMethodComboBox.setModel(new DefaultComboBoxModel(new String[] {
                XencUtil.TRIPLE_DES_CBC,
                XencUtil.AES_128_CBC,
                XencUtil.AES_192_CBC,
                XencUtil.AES_256_CBC,
                XencUtil.AES_128_GCM,
                XencUtil.AES_256_GCM
        }));
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

                final AddCertificateWizard w = new AddCertificateWizard(SwingUtilities.getWindowAncestor(XmlElementEncryptionConfigPanel.this), sp);
                w.setTitle("Configure Recipient Certificate");
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

        RunOnChangeListener enableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        });
        specifyCertificateRadioButton.addActionListener(enableListener);
        useContextVariableRadioButton.addActionListener(enableListener);

        contextVariableField.setValueWillBeRead(true);
        contextVariableField.setValueWillBeWritten(false);

        updateRecipientCertLabel();
        add(contentPane, BorderLayout.CENTER);
    }

    @Override
    public void focusFirstComponent() {
        encryptionMethodComboBox.requestFocus();
    }

    public void enableOrDisableComponents() {
        setRecipientCertificateButton.setEnabled(specifyCertificateRadioButton.isSelected());
        contextVariableField.setEnabled(useContextVariableRadioButton.isSelected());
    }

    @Override
    protected void doUpdateModel() {
        model.setEncryptContentsOnly(false);
        model.setRecipientCertificateBase64(certb64);
        model.setXencAlgorithm((String)encryptionMethodComboBox.getSelectedItem());
        if (specifyCertificateRadioButton.isSelected()) {
            model.setRecipientCertContextVariableName(null);
        } else {
            model.setRecipientCertContextVariableName(contextVariableField.getVariable());
        }
        validateModel();
    }

    public void setData(XmlElementEncryptionConfig model) {
        encryptionMethodComboBox.setSelectedItem(model.getXencAlgorithm());
        final String varname = model.getRecipientCertContextVariableName();
        final boolean useVar = varname != null;
        contextVariableField.setEnabled(useVar);
        contextVariableField.setVariable(useVar ? varname : "");
        specifyCertificateRadioButton.setSelected(!useVar);
        useContextVariableRadioButton.setSelected(useVar);
        certb64 = model.getRecipientCertificateBase64();
        updateRecipientCertLabel();
    }

    public XmlElementEncryptionConfig getData() throws AssertionPropertiesOkCancelSupport.ValidationException {
        doUpdateModel();
        return model;
    }

    private void validateModel() {
        if (useContextVariableRadioButton.isSelected()) {
            String err = contextVariableField.getErrorMessage();
            if (err != null) throw new AssertionPropertiesOkCancelSupport.ValidationException("Unable to save: " + err);
        }
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
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.INFO, "Unable to decode recipient certificate Base-64: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return "<html><i>&lt;Invalid certificate Base-64&gt;";
        } catch (CertificateException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.INFO, "Unable to parse recipient certificate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return "<html><i>&lt;Invalid X.509 certificate&gt;";
        }
    }

    public void setPolicyPosition(Assertion assertion, Assertion previousAssertion) {
        contextVariableField.setAssertion(assertion, previousAssertion);
    }
}
