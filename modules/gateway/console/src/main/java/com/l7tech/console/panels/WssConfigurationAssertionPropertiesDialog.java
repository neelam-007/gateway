package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.xml.SupportedDigestMethods;
import com.l7tech.security.xml.WsSecurityVersion;
import com.l7tech.security.xml.XencUtil;
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
    private JComboBox encryptionAlgCombo;
    private JComboBox keyEncryptionAlgCombo;
    private JComboBox signatureKeyReferenceCombo;
    private JComboBox encryptionKeyReferenceCombo;
    private JCheckBox useDerivedKeysCheckBox;
    private JCheckBox signTimestampCheckBox;
    private JCheckBox signSecurityTokensCheckBox;
    private JCheckBox addTimestampCheckBox;

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
        final String sigRef = assertion.getKeyReference();
        setSelectedItemOrUnchangedIfNull(signatureKeyReferenceCombo, sigRef == null ? null : KeyReference.valueOf(sigRef));
        final String encRef = assertion.getEncryptionKeyReference();
        setSelectedItemOrUnchangedIfNull(encryptionKeyReferenceCombo, encRef == null ? null : KeyReference.valueOf(encRef));
        setSelectedItemOrUnchangedIfNull(encryptionAlgCombo, assertion.getEncryptionAlgorithmUri());
        setSelectedItemOrUnchangedIfNull(keyEncryptionAlgCombo, assertion.getKeyWrappingAlgorithmUri());
        useDerivedKeysCheckBox.setSelected(assertion.isUseDerivedKeys());
        addTimestampCheckBox.setSelected(assertion.isAddTimestamp());
        signTimestampCheckBox.setSelected(assertion.isSignTimestamp());
        signSecurityTokensCheckBox.setSelected(assertion.isProtectTokens());
    }

    @Override
    public WssConfigurationAssertion getData(WssConfigurationAssertion assertion) throws ValidationException {
        assertion.setWssVersion((WsSecurityVersion)getSelectedItemOrNullIfUnchanged(wssVersionCombo));
        assertion.setDigestAlgorithmName((String)getSelectedItemOrNullIfUnchanged(signatureDigestCombo));
        KeyReference sigRef = (KeyReference)getSelectedItemOrNullIfUnchanged(signatureKeyReferenceCombo);
        assertion.setKeyReference(sigRef == null ? null : sigRef.getName());
        KeyReference encRef = (KeyReference)getSelectedItemOrNullIfUnchanged(encryptionKeyReferenceCombo);
        assertion.setEncryptionKeyReference(encRef == null ? null : encRef.getName());
        assertion.setEncryptionAlgorithmUri((String) getSelectedItemOrNullIfUnchanged(encryptionAlgCombo));
        assertion.setKeyWrappingAlgorithmUri((String) getSelectedItemOrNullIfUnchanged(keyEncryptionAlgCombo));
        assertion.setUseDerivedKeys(useDerivedKeysCheckBox.isSelected());
        assertion.setAddTimestamp(addTimestampCheckBox.isSelected());
        assertion.setSignTimestamp(signTimestampCheckBox.isSelected());
        assertion.setProtectTokens(signSecurityTokensCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        wssVersionCombo.setModel(new DefaultComboBoxModel(prependUnchanged(WsSecurityVersion.values())));
        signatureDigestCombo.setModel(new DefaultComboBoxModel(prependUnchanged(SupportedDigestMethods.getDigestNames())));
        signatureKeyReferenceCombo.setModel(new DefaultComboBoxModel(prependUnchanged(KeyReference.getAllTypes().toArray())));

        // X.509 BinarySecurityToken isn't supported for identifying recipient for encryption
        Set<KeyReference> encTypes = new HashSet<KeyReference>(KeyReference.getAllTypes());
        encTypes.remove(KeyReference.BST);
        encryptionKeyReferenceCombo.setModel(new DefaultComboBoxModel(prependUnchanged(encTypes.toArray())));

        encryptionAlgCombo.setModel(new DefaultComboBoxModel(new String[] {
                UNCHANGED,
                XencUtil.TRIPLE_DES_CBC,
                XencUtil.AES_128_CBC,
                XencUtil.AES_192_CBC,
                XencUtil.AES_256_CBC
        }));

        keyEncryptionAlgCombo.setModel(new DefaultComboBoxModel(new String[] {
            UNCHANGED,
            SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO,
            SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2
        }));

        return mainPanel;
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
        return UNCHANGED == ret ? null : ret;
    }
}
