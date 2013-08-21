package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.TextUtils;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
    private JCheckBox typeAttributeCheckBox;
    private JCheckBox encryptOnlyElementContentsCheckBox;
    private SquigglyTextField typeSquigglyField;
    private JCheckBox recipientAttributeCheckBox;
    private SquigglyTextField recipientSquigglyField;
    private JCheckBox oaepCheckBox;
    private String certb64;

    private final XmlElementEncryptionConfig model;
    private final boolean truncateCertName;
    private final boolean allowEncryptContentsOnly;

    public XmlElementEncryptionConfigPanel(final XmlElementEncryptionConfig model, final boolean truncateCertName, final boolean allowEncryptContentsOnly) {
        this.model = model;
        this.truncateCertName = truncateCertName;
        this.allowEncryptContentsOnly = allowEncryptContentsOnly;
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

        contextVariableField.setAlwaysPermitSyntax(true);
        contextVariableField.setValueWillBeRead(false);
        contextVariableField.setValueWillBeWritten(false);

        updateRecipientCertLabel();

        typeAttributeCheckBox.addActionListener(enableListener);
        recipientAttributeCheckBox.addActionListener(enableListener);

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(typeSquigglyField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                SquigglyFieldUtils.validateSquigglyFieldForUris(typeSquigglyField, false);
            }
        }, 500, new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (typeAttributeCheckBox.isSelected() && typeSquigglyField.getText().trim().isEmpty()) {
                    typeSquigglyField.setText(XmlElementEncryptionConfig.TYPE_ATTRIBUTE_DEFAULT);
                }
            }
        });

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(recipientSquigglyField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                SquigglyFieldUtils.validateSquigglyFieldForVariableReference(recipientSquigglyField);
            }
        }, 500);

        encryptOnlyElementContentsCheckBox.setVisible(allowEncryptContentsOnly);

        add(contentPane, BorderLayout.CENTER);
    }

    @Override
    public void focusFirstComponent() {
        encryptionMethodComboBox.requestFocus();
    }

    public void enableOrDisableComponents() {
        setRecipientCertificateButton.setEnabled(specifyCertificateRadioButton.isSelected());
        contextVariableField.setEnabled(useContextVariableRadioButton.isSelected());
        typeSquigglyField.setEnabled(typeAttributeCheckBox.isSelected());
        recipientSquigglyField.setEnabled(recipientAttributeCheckBox.isSelected());
    }

    @Override
    protected void doUpdateModel() {
        model.setEncryptContentsOnly(false);
        model.setXencAlgorithm((String)encryptionMethodComboBox.getSelectedItem());
        if (specifyCertificateRadioButton.isSelected()) {
            model.setRecipientCertificateBase64(certb64);
            model.setRecipientCertContextVariableName(null);
        } else {
            model.setRecipientCertificateBase64(null);
            model.setRecipientCertContextVariableName(contextVariableField.getVariable());
        }

        final boolean includeTypeAttribute = typeAttributeCheckBox.isSelected();
        model.setIncludeEncryptedDataTypeAttribute(includeTypeAttribute);
        if (includeTypeAttribute) {
            final String typeAttribute = typeSquigglyField.getText().trim();
            model.setEncryptedDataTypeAttribute(typeAttribute);
        } else {
            model.setEncryptedDataTypeAttribute(null);
        }

        final boolean addRecipient = recipientAttributeCheckBox.isSelected();
        final String recipientText = recipientSquigglyField.getText().trim();
        if (addRecipient && !recipientText.isEmpty()) {
            model.setEncryptedKeyRecipientAttribute(recipientText);
        } else {
            model.setEncryptedKeyRecipientAttribute(null);
        }

        final boolean contentsOnly = encryptOnlyElementContentsCheckBox.isSelected();
        model.setEncryptContentsOnly(contentsOnly);

        model.setUseOaep(oaepCheckBox.isSelected());

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

        typeAttributeCheckBox.setSelected(model.isIncludeEncryptedDataTypeAttribute());
        typeSquigglyField.setText(model.getEncryptedDataTypeAttribute());
        encryptOnlyElementContentsCheckBox.setSelected(model.isEncryptContentsOnly());

        final String recipientAttribute = model.getEncryptedKeyRecipientAttribute();
        if (recipientAttribute != null) {
            recipientAttributeCheckBox.setSelected(true);
            recipientSquigglyField.setText(recipientAttribute);
        }

        oaepCheckBox.setSelected(model.isUseOaep());

        enableOrDisableComponents();
    }

    public XmlElementEncryptionConfig getData() throws AssertionPropertiesOkCancelSupport.ValidationException {
        doUpdateModel();
        return model;
    }

    private void validateModel() {
        if (useContextVariableRadioButton.isSelected()) {
            String err = contextVariableField.getErrorMessage();
            if (err != null) throw new AssertionPropertiesOkCancelSupport.ValidationException("Unable to save: " + err);
        } else {
            if (model.getRecipientCertificateBase64() == null) {
                throw new AssertionPropertiesOkCancelSupport.ValidationException("Unable to save: No Recipient Certificate is configured");
            }
        }

        // will only happen for very fast keyboard users - if they are quicker than the focus listener
        if (typeAttributeCheckBox.isSelected() && typeSquigglyField.getText().trim().isEmpty()) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("Unable to save: No value for Type attribute is configured");
        }

        final String typeError = SquigglyFieldUtils.validateSquigglyFieldForUris(typeSquigglyField, false);
        if (typeError != null) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("Unable to save: " + typeError);
        }

        if (recipientAttributeCheckBox.isSelected() && recipientSquigglyField.getText().trim().isEmpty()) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("Unable to save: No value for Recipient attribute is configured");
        }

        final String recipientError = SquigglyFieldUtils.validateSquigglyFieldForVariableReference(recipientSquigglyField);
        if (recipientError != null) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("Unable to save: " + recipientError);
        }

    }

    private void updateRecipientCertLabel() {
        final FontMetrics fontMetrics = recipientCertLabel.getFontMetrics(recipientCertLabel.getFont());
        final String certInfo = getCertInfo(certb64);
        final String certText = (truncateCertName)? TextUtils.truncateBelowActualScreenSize(fontMetrics, certInfo, 390): certInfo;
        recipientCertLabel.setText(certText);
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

    /**
     * Call to ensure context variables known in the policy are available to the TargetVariablePanel
     * @param assertion the assertion being configured
     * @param previousAssertion the previous assertion
     */
    public void setPolicyPosition(Assertion assertion, Assertion previousAssertion) {
        contextVariableField.setAssertion(assertion, previousAssertion);
    }
}
