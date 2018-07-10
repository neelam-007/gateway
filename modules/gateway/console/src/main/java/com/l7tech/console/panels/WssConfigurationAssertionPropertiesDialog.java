package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion;
import com.l7tech.security.xml.*;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.SoapConstants;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Assertion properties editor dialog for {@link WssConfigurationAssertion}.
 */
public class WssConfigurationAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<WssConfigurationAssertion> {
    private JPanel mainPanel;
    private JComboBox wssVersionCombo;
    private JComboBox signatureDigestCombo;
    private JComboBox signatureReferenceDigestCombo;
    private JComboBox encryptionAlgCombo;
    private JComboBox keyEncryptionAlgCombo;
    private JComboBox signatureKeyReferenceCombo;
    private JComboBox encryptionKeyReferenceCombo;
    private JComboBox secureConversationNsComboBox;
    private JLabel secureConversationNsLabel;
    private JCheckBox useDerivedKeysCheckBox;
    private JCheckBox signSecurityTokensCheckBox;
    private JCheckBox encryptSignatureCheckBox;
    private JCheckBox signWSAddressingHeadersCheckBox;
    private JComboBox timestampSignatureCombo;
    private JComboBox timestampCombo;

    private static final String UNCHANGED = "<Unchanged>";

    public WssConfigurationAssertionPropertiesDialog(Window owner, final WssConfigurationAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(WssConfigurationAssertion assertion) {
        setSelectedItemOrUnchangedIfNull(wssVersionCombo, assertion.getWssVersion());
        setSelectedItemOrUnchangedIfNull(signatureDigestCombo, assertion.getDigestAlgorithmName());
        setSelectedItemOrUnchangedIfNull(signatureReferenceDigestCombo, assertion.getReferenceDigestAlgorithmName());
        final String sigRef = assertion.getKeyReference();
        setSelectedItemOrUnchangedIfNull(signatureKeyReferenceCombo, sigRef == null ? null : KeyReference.valueOf(sigRef));
        final String encRef = assertion.getEncryptionKeyReference();
        setSelectedItemOrUnchangedIfNull(encryptionKeyReferenceCombo, encRef == null ? null : KeyReference.valueOf(encRef));
        setSelectedItemOrUnchangedIfNull(encryptionAlgCombo, assertion.getEncryptionAlgorithmUri());
        setSelectedItemOrUnchangedIfNull(keyEncryptionAlgCombo, assertion.getKeyWrappingAlgorithmUri());
        useDerivedKeysCheckBox.setSelected(assertion.isUseDerivedKeys());
        setSelectedItemOrUnchangedIfNull(secureConversationNsComboBox, assertion.getSecureConversationNamespace());
        final String timestampString =  assertion.getTimestampValue();
        setSelectedItemOrUnchangedIfNull(timestampCombo, timestampString == null ? null : TimestampEnum.valueOf(timestampString));
        final String timestampSingatureString =  assertion.getSignTimestampValue();
        setSelectedItemOrUnchangedIfNull(timestampSignatureCombo, timestampSingatureString == null ? null : TimestampSignatureEnum.valueOf(timestampSingatureString));
        signSecurityTokensCheckBox.setSelected(assertion.isProtectTokens());
        encryptSignatureCheckBox.setSelected(assertion.isEncryptSignature());
        signWSAddressingHeadersCheckBox.setSelected(assertion.isSignWsAddressingHeaders());
        enableAndDisableComponents();
    }

    @Override
    public WssConfigurationAssertion getData(WssConfigurationAssertion assertion) throws ValidationException {
        assertion.setWssVersion((WsSecurityVersion)getSelectedItemOrNullIfUnchanged(wssVersionCombo));
        assertion.setDigestAlgorithmName((String) getSelectedItemOrNullIfUnchanged(signatureDigestCombo));
        assertion.setReferenceDigestAlgorithmName((String)getSelectedItemOrNullIfUnchanged(signatureReferenceDigestCombo));
        KeyReference sigRef = (KeyReference)getSelectedItemOrNullIfUnchanged(signatureKeyReferenceCombo);
        assertion.setKeyReference(sigRef == null ? null : sigRef.getName());
        KeyReference encRef = (KeyReference)getSelectedItemOrNullIfUnchanged(encryptionKeyReferenceCombo);
        assertion.setEncryptionKeyReference(encRef == null ? null : encRef.getName());
        assertion.setEncryptionAlgorithmUri((String) getSelectedItemOrNullIfUnchanged(encryptionAlgCombo));
        assertion.setKeyWrappingAlgorithmUri((String) getSelectedItemOrNullIfUnchanged(keyEncryptionAlgCombo));
        assertion.setUseDerivedKeys(useDerivedKeysCheckBox.isSelected());
        assertion.setSecureConversationNamespace((String)getSelectedItemOrNullIfUnchanged(secureConversationNsComboBox));
        TimestampEnum timestampEnum = (TimestampEnum)getSelectedItemOrNullIfUnchanged(timestampCombo);
        assertion.setTimestampValue( timestampEnum == null ? null : timestampEnum.name());
        TimestampSignatureEnum timestampSignatureEnum = (TimestampSignatureEnum)getSelectedItemOrNullIfUnchanged(timestampSignatureCombo);
        assertion.setSignTimestampValue(timestampSignatureEnum == null ? null : timestampSignatureEnum.name());
        assertion.setProtectTokens(signSecurityTokensCheckBox.isSelected());
        assertion.setEncryptSignature(encryptSignatureCheckBox.isSelected());
        assertion.setSignWsAddressingHeaders(signWSAddressingHeadersCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        wssVersionCombo.setModel(new DefaultComboBoxModel(prependUnchanged(WsSecurityVersion.values())));
        timestampCombo.setModel(new DefaultComboBoxModel(prependUnchanged(TimestampEnum.values())));
        timestampSignatureCombo.setModel(new DefaultComboBoxModel(prependUnchanged(TimestampSignatureEnum.values())));
        signatureDigestCombo.setModel(new DefaultComboBoxModel(prependUnchanged(SupportedDigestMethods.getDigestNames())));
        signatureReferenceDigestCombo.setModel(new DefaultComboBoxModel(prependUnchanged(SupportedDigestMethods.getDigestNames())));
        Set<KeyReference> sigTypes = new HashSet<KeyReference>(KeyReference.getAllTypes());
        sigTypes.remove( KeyReference.KEY_NAME ); // key name references not supported for signatures
        signatureKeyReferenceCombo.setModel(new DefaultComboBoxModel(prependUnchanged(sigTypes.toArray())));

        Set<KeyReference> encTypes = new HashSet<KeyReference>(KeyReference.getAllTypes());
        encryptionKeyReferenceCombo.setModel(new DefaultComboBoxModel(prependUnchanged(encTypes.toArray())));

        encryptionAlgCombo.setModel(new DefaultComboBoxModel(new String[] {
                UNCHANGED,
                XencUtil.TRIPLE_DES_CBC,
                XencUtil.AES_128_CBC,
                XencUtil.AES_192_CBC,
                XencUtil.AES_256_CBC,
                XencUtil.AES_128_GCM,
                XencUtil.AES_256_GCM
        }));

        keyEncryptionAlgCombo.setModel(new DefaultComboBoxModel(new String[] {
            UNCHANGED,
            SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO,
            SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2
        }));

        secureConversationNsComboBox.setModel(new DefaultComboBoxModel(prependUnchanged(SoapConstants.WSSC_NAMESPACE_ARRAY)));

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            protected void run() {
                enableAndDisableComponents();
            }
        };
        useDerivedKeysCheckBox.addActionListener( enableDisableListener );

        return mainPanel;
    }

    private void enableAndDisableComponents() {
        final boolean enableSecureConversationNs = useDerivedKeysCheckBox.isSelected();
        secureConversationNsComboBox.setEnabled( enableSecureConversationNs );
        secureConversationNsLabel.setEnabled( enableSecureConversationNs );
    }

    private static Object[] prependUnchanged(Object[] things) {
        return ArrayUtils.unshift(things, UNCHANGED);
    }

    private static void setSelectedItemOrUnchangedIfNull(JComboBox comboBox, Object objOrUnchanged) {
        if (null == objOrUnchanged) {
            comboBox.setSelectedItem(UNCHANGED);
        } else {
            comboBox.setSelectedItem(objOrUnchanged);
        }
    }

    private static Object getSelectedItemOrNullIfUnchanged(JComboBox comboBox) {
        Object ret = comboBox.getSelectedItem();
        return UNCHANGED == ret || !comboBox.isEnabled() ? null : ret;
    }
}
