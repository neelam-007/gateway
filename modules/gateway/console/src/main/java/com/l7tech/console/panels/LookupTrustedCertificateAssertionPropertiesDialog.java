package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion;
import com.l7tech.util.CollectionUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Map;

import static com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion.LookupType;
import static com.l7tech.util.Option.optional;

/**
 * Properties dialog for Look Up Certificate assertion
 */
public class LookupTrustedCertificateAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<LookupTrustedCertificateAssertion> {

    private JPanel mainPanel;
    private JTextField trustedCertificateNameTextField;
    private JCheckBox failMultipleCertificatesCheckBox;
    private TargetVariablePanel targetVariablePanel;
    private JRadioButton lookUpTrustedCertificateRadioButton;
    private JRadioButton lookUpByThumbprintRadioButton;
    private JRadioButton lookUpBySkiRadioButton;
    private JRadioButton lookUpByIssuerSerialRadioButton;
    private JRadioButton lookUpBySubjectDnRadioButton;
    private JTextField certThumbprintField;
    private JTextField certSkiField;
    private JTextField certIssuerField;
    private JTextField certSerialField;
    private JTextField certSubjectDnField;

    private Map<LookupType, JRadioButton> rbByLookup = CollectionUtils.<LookupType, JRadioButton>mapBuilder()
            .put(LookupType.TRUSTED_CERT_NAME, lookUpTrustedCertificateRadioButton)
            .put(LookupType.CERT_THUMBPRINT_SHA1, lookUpByThumbprintRadioButton)
            .put(LookupType.CERT_SKI, lookUpBySkiRadioButton)
            .put(LookupType.CERT_ISSUER_SERIAL, lookUpByIssuerSerialRadioButton)
            .put(LookupType.CERT_SUBJECT_DN, lookUpBySubjectDnRadioButton)
            .unmodifiableMap();

    public LookupTrustedCertificateAssertionPropertiesDialog( final Window owner,
                                                              final LookupTrustedCertificateAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData( final LookupTrustedCertificateAssertion assertion ) {
        optional(rbByLookup.get(assertion.getLookupType())).orSome(lookUpTrustedCertificateRadioButton).setSelected(true);
        trustedCertificateNameTextField.setText(assertion.getTrustedCertificateName());
        certThumbprintField.setText(assertion.getCertThumbprintSha1());
        certSkiField.setText(assertion.getCertSubjectKeyIdentifier());
        certSubjectDnField.setText(assertion.getCertSubjectDn());
        certIssuerField.setText(assertion.getCertIssuerDn());
        certSerialField.setText(assertion.getCertSerialNumber());
        failMultipleCertificatesCheckBox.setSelected(!assertion.isAllowMultipleCertificates());
        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());
        targetVariablePanel.setVariable( assertion.getVariableName() );
        updateEnabledState();
    }

    @Override
    public LookupTrustedCertificateAssertion getData( final LookupTrustedCertificateAssertion assertion ) throws ValidationException {
        assertion.setLookupType(getSelectedLookupType());
        assertion.setTrustedCertificateName(trustedCertificateNameTextField.getText().trim());
        assertion.setCertThumbprintSha1(certThumbprintField.getText());
        assertion.setCertSubjectKeyIdentifier(certSkiField.getText());
        assertion.setCertSubjectDn(certSubjectDnField.getText());
        assertion.setCertIssuerDn(certIssuerField.getText());
        assertion.setCertSerialNumber( certSerialField.getText() );
        assertion.setAllowMultipleCertificates(!failMultipleCertificatesCheckBox.isSelected());
        assertion.setVariableName(targetVariablePanel.getVariable());
        return assertion;
    }

    private LookupType getSelectedLookupType() {
        for (Map.Entry<LookupType, JRadioButton> entry : rbByLookup.entrySet()) {
            if (entry.getValue().isSelected())
                return entry.getKey();
        }
        return LookupType.TRUSTED_CERT_NAME;
    }

    @Override
    protected JPanel createPropertyPanel() {
        final RunOnChangeListener buttonStateUpdateListener = new RunOnChangeListener(){
            @Override
            public void run() {
                updateEnabledState();
            }
        };

        final JTextComponent[] fields = {trustedCertificateNameTextField,
                certIssuerField,
                certSerialField,
                certSkiField,
                certSubjectDnField,
                certThumbprintField};
        addDocumentListener(buttonStateUpdateListener, fields);
        Utilities.enableGrayOnDisabled(fields);

        addActionListener(buttonStateUpdateListener,
                lookUpByIssuerSerialRadioButton,
                lookUpBySkiRadioButton,
                lookUpBySubjectDnRadioButton,
                lookUpByThumbprintRadioButton,
                lookUpTrustedCertificateRadioButton);

        targetVariablePanel.addChangeListener(buttonStateUpdateListener);

        return mainPanel;
    }

    private static void addDocumentListener(DocumentListener listener, JTextComponent... components) {
        for (JTextComponent component : components) {
            component.getDocument().addDocumentListener(listener);
        }
    }

    private static void addActionListener(ActionListener listener, AbstractButton... buttons) {
        for (AbstractButton button : buttons) {
            button.addActionListener(listener);
        }
    }

    private void updateEnabledState() {
        trustedCertificateNameTextField.setEnabled(lookUpTrustedCertificateRadioButton.isSelected());
        certSkiField.setEnabled(lookUpBySkiRadioButton.isSelected());
        certThumbprintField.setEnabled(lookUpByThumbprintRadioButton.isSelected());
        certSubjectDnField.setEnabled(lookUpBySubjectDnRadioButton.isSelected());

        boolean issuerSerial = lookUpByIssuerSerialRadioButton.isSelected();
        certIssuerField.setEnabled(issuerSerial);
        certSerialField.setEnabled(issuerSerial);


        boolean enableOk = true;

        if (lookUpTrustedCertificateRadioButton.isSelected() && trustedCertificateNameTextField.getText().trim().isEmpty())
            enableOk = false;

        if (lookUpBySkiRadioButton.isSelected() && certSkiField.getText().trim().isEmpty())
            enableOk = false;

        if (lookUpByThumbprintRadioButton.isSelected() && certThumbprintField.getText().trim().isEmpty())
            enableOk = false;

        if (lookUpBySubjectDnRadioButton.isSelected() && certSubjectDnField.getText().trim().isEmpty())
            enableOk = false;

        if (lookUpByIssuerSerialRadioButton.isSelected() && (certIssuerField.getText().trim().isEmpty() || certSerialField.getText().trim().isEmpty()))
            enableOk = false;

        if (!targetVariablePanel.isEntryValid())
            enableOk = false;

        getOkButton().setEnabled( !isReadOnly() && enableOk );
    }
}
