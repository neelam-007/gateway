package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion.KeySource;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server.BlockAsymmetricAlgorithm;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AsymmetricKeyEncryptionDecryptionAssertionDialog extends AssertionPropertiesOkCancelSupport<AsymmetricKeyEncryptionDecryptionAssertion> {

    private static Logger logger = Logger.getLogger(AsymmetricKeyEncryptionDecryptionAssertionDialog.class.getName());

    private static final String CERTIFICATE_LABEL = "User Certificate";
    private static final String PRIVATE_KEY_LABEL = "User Private Key";

    private AsymmetricKeyEncryptionDecryptionAssertion assertion;
    private JPanel contentPane;
    private JRadioButton encryptRadioButton;
    private JRadioButton decryptRadioButton;
    private JComboBox keyComboBox;
    private TargetVariablePanel inputVariableField;
    private TargetVariablePanel outputVariableField;
    private JComboBox<String> nameComboBox;
    private JComboBox<String> modeComboBox;
    private JComboBox<String> paddingComboBox;
    private JRadioButton fromStoreRadioButton;
    private JRadioButton fromValueRadioButton;
    private JTextField keyValueField;
    private JPanel certificatePanel;

    private List<KeyEntry> trustedCertificates = new ArrayList<>();
    private List<KeyEntry> privateKeys = new ArrayList<>();

    private class KeyEntry {

        private String name;
        private Goid goid;

        private KeyEntry(String name, Goid goid) {
            this.name = name;
            this.goid = goid;
        }

        public String toString() {
            return name;
        }

        private String getName() {
            return name;
        }

        private Goid getGoid() {
            return goid;
        }
    }

    public AsymmetricKeyEncryptionDecryptionAssertionDialog(Window owner, AsymmetricKeyEncryptionDecryptionAssertion assertion) {
        super(AsymmetricKeyEncryptionDecryptionAssertion.class, owner, "Asymmetric Key Encryption / Decryption Assertion Properties", true);
        this.assertion = assertion;
        initComponents();
    }

    @Override  //set fields in assertion from data in dialog
    public AsymmetricKeyEncryptionDecryptionAssertion getData(AsymmetricKeyEncryptionDecryptionAssertion assertion) throws ValidationException {

        assertion.setInputVariable(inputVariableField.getVariable());
        assertion.setOutputVariable(outputVariableField.getVariable());

        if (encryptRadioButton.isSelected())
            assertion.setMode(Cipher.ENCRYPT_MODE);
        else if (decryptRadioButton.isSelected())
            assertion.setMode(Cipher.DECRYPT_MODE);

        if (fromStoreRadioButton.isSelected()) {
            assertion.setKeySource(KeySource.FROM_STORE);
            assertion.setKeyName(((KeyEntry) keyComboBox.getSelectedItem()).getName());
            assertion.setKeyGoid(((KeyEntry) keyComboBox.getSelectedItem()).getGoid());
            assertion.setRsaKeyValue("");
        } else if (fromValueRadioButton.isSelected()) {
            assertion.setKeySource(KeySource.FROM_VALUE);
            assertion.setRsaKeyValue(keyValueField.getText());
            assertion.setKeyName("");
            assertion.setKeyGoid(Goid.DEFAULT_GOID);
        }
        assertion.setAlgorithm(BlockAsymmetricAlgorithm.getAlgorithm(
                (String) nameComboBox.getSelectedItem(), (String) modeComboBox.getSelectedItem(), (String) paddingComboBox.getSelectedItem()));

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return this.contentPane;
    }

    @Override    //set fields in dialog from data in assertion
    public void setData(AsymmetricKeyEncryptionDecryptionAssertion assertion) {
        int mode = assertion.getMode();

        if (mode == Cipher.ENCRYPT_MODE) {
            encryptRadioButton.setSelected(true);
        } else if (mode == Cipher.DECRYPT_MODE) {
            decryptRadioButton.setSelected(true);
        }

        if (assertion.getKeySource() == KeySource.FROM_STORE) {
            fromStoreRadioButton.setSelected(true);
        } else {
            fromValueRadioButton.setSelected(true);
        }

        populateKeyComboBoxOrRsaVariable();

        inputVariableField.setVariable(assertion.getInputVariable());
        outputVariableField.setVariable(assertion.getOutputVariable());
        outputVariableField.setAssertion(assertion, getPreviousAssertion());

        String algorithm = assertion.getAlgorithm();
        if (algorithm == null) {
            nameComboBox.setSelectedItem(BlockAsymmetricAlgorithm.NAME_RSA);
            modeComboBox.setSelectedItem(BlockAsymmetricAlgorithm.MODE_ECB);
            paddingComboBox.setSelectedItem(BlockAsymmetricAlgorithm.PADDING_NO_PADDING);
        } else {
            String[] algorithmParts = BlockAsymmetricAlgorithm.parseAlgorithm(algorithm);
            nameComboBox.setSelectedItem(algorithmParts[0]);
            modeComboBox.setSelectedItem(algorithmParts[1]);
            paddingComboBox.setSelectedItem(algorithmParts[2]);
        }
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        ActionListener radioButterListener = e -> {
            populateKeyComboBoxOrRsaVariable();
            toggleOkButton();
        };

        //setup radio button
        encryptRadioButton.addActionListener(radioButterListener);
        decryptRadioButton.addActionListener(radioButterListener);

        fromStoreRadioButton.addActionListener(radioButterListener);
        fromValueRadioButton.addActionListener(radioButterListener);

        encryptRadioButton.setSelected(true);
        fromStoreRadioButton.setSelected(true);

        ItemListener comboBoxItemListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    toggleOkButton();
                }
            }
        };

        //initialize the key selection box.  Since encryptRadioButton is selected
        //it will be populated with certificate data.
        populateKeyComboBoxOrRsaVariable();
        keyComboBox.addItemListener(comboBoxItemListener);

        //initialize input/output text fields
        ChangeListener variableFieldChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                toggleOkButton();
            }
        };

        inputVariableField.addChangeListener(variableFieldChangeListener);
        outputVariableField.addChangeListener(variableFieldChangeListener);
        keyValueField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) { toggleOkButton(); }
            @Override
            public void insertUpdate(DocumentEvent e) { toggleOkButton(); }
            @Override
            public void removeUpdate(DocumentEvent e) { toggleOkButton(); }
        });
        
        nameComboBox.setModel(new DefaultComboBoxModel(BlockAsymmetricAlgorithm.getKnownNames().toArray()));
        nameComboBox.addItemListener(comboBoxItemListener);

        modeComboBox.setModel(new DefaultComboBoxModel(BlockAsymmetricAlgorithm.getKnownModes().toArray()));
        modeComboBox.addItemListener(comboBoxItemListener);

        paddingComboBox.setModel(new DefaultComboBoxModel(BlockAsymmetricAlgorithm.getKnownPaddings().toArray()));
        paddingComboBox.addItemListener(comboBoxItemListener);
    }

    private void populateKeyComboBoxOrRsaVariable() {
        if (fromStoreRadioButton.isSelected()) {
            keyValueField.setText(null);
            keyValueField.setEnabled(false);

            List<KeyEntry> keyEntries = new ArrayList<>();
            keyComboBox.setEnabled(true);

            if (encryptRadioButton.isSelected())
                keyEntries = getTrustedCertificateList();
            else if (decryptRadioButton.isSelected())
                keyEntries = getPrivateKeyList();

            keyComboBox.setModel(new DefaultComboBoxModel(keyEntries.toArray()));

            //update combo box with current selection
            setSelectedCertificateEntry(assertion.getKeyName(), assertion.getKeyGoid());
        } else if (fromValueRadioButton.isSelected()) {
            fromValueRadioButton.setSelected(true);
            keyValueField.setEnabled(true);
            keyValueField.setText(assertion.getRsaKeyValue());
            keyComboBox.setSelectedItem(-1);
            keyComboBox.setEnabled(false);
        }

        if (encryptRadioButton.isSelected()) {
            certificatePanel.setBorder(BorderFactory.createTitledBorder(CERTIFICATE_LABEL));
        } else if (decryptRadioButton.isSelected()) {
            certificatePanel.setBorder(BorderFactory.createTitledBorder(PRIVATE_KEY_LABEL));
        }
    }

    /**
     * @return a list of the name and oid of trusted certificates from the certificate manager
     */
    private List<KeyEntry> getTrustedCertificateList() {

        if (Registry.getDefault().isAdminContextPresent() && trustedCertificates.isEmpty()) {

            try {
                List<TrustedCert> allCertsFromStore = Registry.getDefault().getTrustedCertManager().findAllCerts();
                for (TrustedCert certificate : allCertsFromStore) {
                    KeyEntry keyEntry = new KeyEntry(certificate.getCertificate().getSubjectDN().getName(),
                                            certificate.getGoid());
                    trustedCertificates.add(keyEntry);
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to read trusted certs: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return trustedCertificates;
    }

    /**
     * @return a list of the alias and keystore id for private keys in the gateway
     */
    private List<KeyEntry> getPrivateKeyList() {

        KeyEntry keyEntry = null;
        List<KeystoreFileEntityHeader> keyStores = null;
        List<SsgKeyEntry> keyList = null;

        if (Registry.getDefault().isAdminContextPresent() && privateKeys.isEmpty()) {

            try {
                TrustedCertAdmin certAdmin = Registry.getDefault().getTrustedCertManager();
                keyStores = certAdmin.findAllKeystores(true); //not sure if we should use true here

                for (KeystoreFileEntityHeader keyStore : keyStores) {

                    keyList = certAdmin.findAllKeys(keyStore.getGoid(), true); //not sure if we should use true here

                    for (SsgKeyEntry ssgKeyEntry : keyList) {
                        keyEntry = new KeyEntry(ssgKeyEntry.getName(), ssgKeyEntry.getKeystoreId());
                        privateKeys.add(keyEntry);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to read private keys: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return privateKeys;
    }

    private void toggleOkButton() {
        if (inputVariableField.isEntryValid() &&
            outputVariableField.isEntryValid() &&
            isCertOrKeyValid() &&
            nameComboBox.getSelectedIndex() != -1 &&
            modeComboBox.getSelectedIndex() != -1 &&
            StringUtils.isNotBlank((String) paddingComboBox.getSelectedItem())) {
            this.getOkButton().setEnabled(true);
        } else {
            this.getOkButton().setEnabled(false);
        }
    }

    private boolean isCertOrKeyValid(){
        return ((fromStoreRadioButton.isSelected() && keyComboBox.getSelectedIndex() != -1)
                || (fromValueRadioButton.isSelected() && !keyValueField.getText().trim().isEmpty()));
    }

    private void setSelectedCertificateEntry(String name, Goid goid) {

        KeyEntry tempKeyEntry = null;
        int numEntries = keyComboBox.getItemCount();

        for (int i = 0; i < numEntries; i++) {
            tempKeyEntry = (KeyEntry) keyComboBox.getItemAt(i);
            if (tempKeyEntry.getName().equals(name) &&
                    tempKeyEntry.getGoid().equals(goid))
                keyComboBox.setSelectedIndex(i);
        }

    }
}
