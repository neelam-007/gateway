package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server.RsaModePaddingOption;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ExceptionUtils;

import javax.crypto.Cipher;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AsymmetricKeyEncryptionDecryptionAssertionDialog extends AssertionPropertiesOkCancelSupport<AsymmetricKeyEncryptionDecryptionAssertion> {

    private static Logger logger = Logger.getLogger(AsymmetricKeyEncryptionDecryptionAssertionDialog.class.getName());

    private static final String CERTIFICATE_LABEL = "User Certificate";
    private static final String PRIVATE_KEY_LABEL = "User Private Key";

    private JPanel contentPane;
    private JRadioButton encryptRadioButton;
    private JRadioButton decryptRadioButton;
    private JComboBox keyComboBox;
    private JLabel outputLabel;
    private JLabel keyLabel;
    private JLabel inputLabel;
    private TargetVariablePanel inputVariableField;
    private TargetVariablePanel outputVariableField;
    private JComboBox modePaddingComboBox;
    private JLabel modePaddingLabel;

    private class KeyEntry {

        private String name;
        private Goid goid = Goid.DEFAULT_GOID;

        public KeyEntry(String _name, Goid _goid) {
            name = _name;
            goid = _goid;
        }

        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Goid getGoid() {
            return goid;
        }

        public void setGoid(Goid _goid) {
            this.goid = _goid;
        }
    }

    public AsymmetricKeyEncryptionDecryptionAssertionDialog(Window owner, AsymmetricKeyEncryptionDecryptionAssertion assertion) {
        super(AsymmetricKeyEncryptionDecryptionAssertion.class, owner, "Asymmetric Key Encryption / Decryption Assertion Properties", true);
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

        assertion.setKeyName(((KeyEntry)keyComboBox.getSelectedItem()).getName());
        assertion.setKeyGoid(((KeyEntry)keyComboBox.getSelectedItem()).getGoid());

        assertion.setModePaddingOption((RsaModePaddingOption)modePaddingComboBox.getSelectedItem());

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
            keyLabel.setText(CERTIFICATE_LABEL);
        } else if (mode == Cipher.DECRYPT_MODE) {
            decryptRadioButton.setSelected(true);
            keyLabel.setText(PRIVATE_KEY_LABEL);
        }

        populateKeyComboBox();

        //update combo box with current selection
        setSelectedCertificateEntry(assertion.getKeyName(), assertion.getKeyGoid());

        inputVariableField.setVariable(assertion.getInputVariable());
        outputVariableField.setVariable(assertion.getOutputVariable());
        outputVariableField.setAssertion(assertion, getPreviousAssertion());

        //update modePadding combo box populateKeyComboBox();
        RsaModePaddingOption selectedModePaddingOption = assertion.getModePaddingOption();
        if (selectedModePaddingOption == null)
            modePaddingComboBox.setSelectedItem(RsaModePaddingOption.NO_MODE_NO_PADDING);
        else
            modePaddingComboBox.setSelectedItem(assertion.getModePaddingOption());
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        //setup radio buttson
        encryptRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keyLabel.setText(CERTIFICATE_LABEL);
                populateKeyComboBox();
                toggleOkButton();
            }
        });

        decryptRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keyLabel.setText(PRIVATE_KEY_LABEL);
                populateKeyComboBox();
            }
        });

        encryptRadioButton.setSelected(true);
        keyLabel.setText(CERTIFICATE_LABEL);

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
        populateKeyComboBox();
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

        //initialize mode padding combo box
        RsaModePaddingOption[] modePaddingArray = RsaModePaddingOption.values();
        modePaddingComboBox.setModel(new DefaultComboBoxModel(modePaddingArray));
        //modePaddingComboBox.setSelectedIndex(1);
        modePaddingComboBox.addItemListener(comboBoxItemListener);
    }

    private void populateKeyComboBox() {

        List<KeyEntry> keyEntries = null;

        if (encryptRadioButton.isSelected())
            keyEntries = getTrustedCertificateList();
        else if (decryptRadioButton.isSelected())
            keyEntries = getPrivateKeyList();

        keyComboBox.setModel(new DefaultComboBoxModel(keyEntries.toArray()));
    }

    /**
     * @return a list of the name and oid of trusted certificates from the certificate manager
     */
    private List<KeyEntry> getTrustedCertificateList() {

        KeyEntry keyEntry = null;
        List<KeyEntry> listOfTrustedCertificates = new ArrayList<KeyEntry>();

        if (Registry.getDefault().isAdminContextPresent()) {

            try {
                java.util.List<TrustedCert> trustedCertsList = Registry.getDefault().getTrustedCertManager().findAllCerts();
                for (TrustedCert certificate : trustedCertsList) {
                    keyEntry = new KeyEntry(certificate.getCertificate().getSubjectDN().getName(),
                                            certificate.getGoid());
                    listOfTrustedCertificates.add(keyEntry);
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to read trusted certs: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return listOfTrustedCertificates;
    }

    /**
     * @return a list of the alias and keystore id for private keys in the gateway
     */
    private List<KeyEntry> getPrivateKeyList() {

        KeyEntry keyEntry = null;
        List<KeyEntry> listOfPrivateKeys = new ArrayList<KeyEntry>();
        List<KeystoreFileEntityHeader> keyStores = null;
        List<SsgKeyEntry> keyList = null;

        if (Registry.getDefault().isAdminContextPresent()) {

            try {
                TrustedCertAdmin certAdmin = Registry.getDefault().getTrustedCertManager();
                keyStores = certAdmin.findAllKeystores(true); //not sure if we should use true here

                for (KeystoreFileEntityHeader keyStore : keyStores) {

                    keyList = certAdmin.findAllKeys(keyStore.getGoid(), true); //not sure if we should use true here

                    for (SsgKeyEntry ssgKeyEntry : keyList) {
                        keyEntry = new KeyEntry(ssgKeyEntry.getName(), ssgKeyEntry.getKeystoreId());
                        listOfPrivateKeys.add(keyEntry);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to read private keys: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return listOfPrivateKeys;
    }

    private void toggleOkButton() {

        if (inputVariableField.isEntryValid() &&
            outputVariableField.isEntryValid() &&
            keyComboBox.getSelectedIndex() != -1 &&
            modePaddingComboBox.getSelectedIndex() != -1) {
            this.getOkButton().setEnabled(true);
        } else {
            this.getOkButton().setEnabled(false);
        }
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
