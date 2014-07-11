package com.l7tech.external.assertions.jsonwebtoken.console;

import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.GoidUpgradeMapper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.*;

/**
 * User: rseminoff
 * Date: 28/11/12
 */
public class AlgorithmSecretPanel extends JPanel {
    private JRadioButton privateKeyRadioButton;
    private JRadioButton variableRadioButton;
    private PrivateKeysComboBox privateKeyCombo;
    private SecurePasswordComboBox passwordCombo;
    private JRadioButton passwordRadioButton;
    private JPanel algPanel;
    private TargetVariablePanel variableField;
    private JRadioButton noSecretRadioButton;
    private JCheckBox variableBase64EncodedCheckbox;
    private int secretSelection;

    private boolean keyHasTitleItem;
    private boolean passwordHasTitleItem;

    private Assertion myAssertion;

    public AlgorithmSecretPanel() {
        super();
        initComponents();

        privateKeyRadioButton.setSelected(false);
        variableRadioButton.setSelected(false);
        passwordRadioButton.setSelected(false);

        privateKeyCombo.setSelectedItem(-1);
        privateKeyCombo.setIncludeDefaultSslKey(false);
        privateKeyCombo.setIncludeRestrictedAccessKeys(false);
        privateKeyCombo.setIncludeHardwareKeystore(false);
        privateKeyCombo.repopulate();
        keyHasTitleItem = false;

        passwordCombo.setSelectedItem(-1);
        passwordHasTitleItem = false;

        variableField.setVariable("");
        variableField.setValueWillBeRead(true);
        variableField.setValueWillBeWritten(false);

        variableBase64EncodedCheckbox.setSelected(false);

        secretSelection = SELECTED_SECRET_NONE;
    }

    public void initComponents() {

        // Only enable password and private key selection if there are any available to select from.
        privateKeyEnabled(ssgHasPrivateKeysAvailable());
        passwordEnabled(ssgHasPasswordsAvailable());

        privateKeyRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // From Variable Radio Button was selected.
                privateKeySelected(true);
                passwordSelected(false);
                variableSelected(false);
            }
        });

        passwordRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // From Variable Radio Button was selected.
                passwordSelected(true);
                privateKeySelected(false);
                variableSelected(false);
            }
        });

        variableRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // From Variable Radio Button was selected.
                variableSelected(true);
                privateKeySelected(false);
                passwordSelected(false);
            }
        });

        noSecretRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // No Secret Button was selected.
                variableSelected(false);
                privateKeySelected(false);
                passwordSelected(false);
            }
        });

    }

    protected void setPanelWithValue(String value, int selection) {
        if (value != null) {
            value = value.trim();
            secretSelection = selection;
            switch (selection) {
                case SELECTED_SECRET_KEY: {
                    // Must be a private key.
                    privateKeySelected(true);
                    String[] keyParts = value.split("[.]");
                    Goid keystoreGOID = null;
                    long keystoreID = Long.MIN_VALUE;
                    String alias = "";
                    if (keyParts.length == 2) {
                        // This is a valid PK variable
                        // The Key is PK:<keystoreID>.<alias>
                        try {
                            keystoreGOID = Goid.parseGoid(keyParts[0]);
                        } catch (IllegalArgumentException iae) {
                            try {
                                keystoreID = Long.parseLong(keyParts[0]);
                            } catch (NumberFormatException nfe) {
                                // Nothing parsed.  Create a default GOID and move on.
                                keystoreGOID = PersistentEntity.DEFAULT_GOID;
                            }
                        }
                        alias = keyParts[1];
                    }

                    if (keystoreGOID == null) {
                        // There is no GOID, just a long to map.
                        keystoreGOID = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, keystoreID);
                    }

                    // Does the private key exist in the gateway right now?
                    int index = privateKeyCombo.select(keystoreGOID, alias);
                    if (index > -1) {
                        privateKeyCombo.setSelectedIndex(privateKeyCombo.select(keystoreGOID, alias));
                    } else {
                        privateKeyCombo.insertItemAt("Select a Key...", 0);
                        privateKeyCombo.setSelectedIndex(0);
                        keyHasTitleItem = true;
                    }

                    passwordCombo.reloadPasswordList();
                    passwordSelected(false);
                    variableSelected(false);
                    return;
                }
                case SELECTED_SECRET_PASSWORD: {
                    passwordSelected(true);
                    Goid goidValue;
                    try {
                        goidValue = Goid.parseGoid(value);
                    } catch (IllegalArgumentException e) {
                        try {
                            goidValue = GoidUpgradeMapper.mapOid(EntityType.SECURE_PASSWORD, Long.parseLong(value));    // Goid didn't parse properly, hopefully this helps.
                        } catch (NumberFormatException nfe) {
                            goidValue = PersistentEntity.DEFAULT_GOID;   // Reset GOID, password must be reselected.
                        }
                    }
                    if (passwordCombo.containsItem(goidValue)) {
                        passwordCombo.setSelectedSecurePassword(goidValue);
                    } else {
                        passwordCombo.setSelectedIndex(-1);
                        passwordHasTitleItem = true;
                    }
                    privateKeySelected(false);
                    variableSelected(false);
                    return;
                }
                case SELECTED_SECRET_VARIABLE_BASE64: {
                    variableBase64EncodedCheckbox.setSelected(true);
                }
                case SELECTED_SECRET_VARIABLE: {
                    variableSelected(true);
                    variableField.setVariable(value);
                    privateKeySelected(false);
                    passwordSelected(false);
                    return;
                }
                default: {
                }  // Includes SELECTED_SECRET_NONE .. falls through.
            }
        }

        value = ""; // Create an empty string.
        // Therefore, we set the UI to default selections and select 'Private Key' to start.
        privateKeyCombo.setSelectedItem(0);
        passwordCombo.setSelectedItem(0);
        variableField.setVariable("");
        privateKeySelected(false);
        passwordSelected(false);
        variableSelected(false);
        variableBase64EncodedCheckbox.setSelected(false);   // Off by default.
        noneSelected(true); // Works regardless if its visible or not.

    }

    protected String getValueFromPanel() {
        // Which radio button is selected?
        if (privateKeyRadioButton.isSelected()) {
            // A private key is selected, so we get the value from the panel.
            if (keyHasTitleItem && (privateKeyCombo.getSelectedItem() instanceof String)) {
                // It's the inserted item "Select A Key...", and is invalid
                return "";  // No value to return.
            }
            secretSelection = SELECTED_SECRET_KEY;
            Goid keyStoreID = privateKeyCombo.getSelectedKeystoreId();
            String keyAlias = privateKeyCombo.getSelectedKeyAlias();
            return (Goid.isDefault(keyStoreID) ? "" : keyStoreID + "." + keyAlias);
        }

        if (passwordRadioButton.isSelected()) {
            if (passwordHasTitleItem && (passwordCombo.getSelectedItem() instanceof String)) {
                // It's the inserted item "Select a Password...", and is invalid.
                return "-1";    // String indicating invalid Selection.
            }
            // It's a password
            secretSelection = SELECTED_SECRET_PASSWORD;
            SecurePassword selectedItem = passwordCombo.getSelectedSecurePassword();
            if (selectedItem != null) {
                Goid selectedGoid = (Goid.isDefault(selectedItem.getGoid()) ? PersistentEntity.DEFAULT_GOID : selectedItem.getGoid());
                return Goid.toString(selectedGoid);
            }
        }

        if (variableRadioButton.isSelected()) {
            secretSelection = (variableBase64EncodedCheckbox.isSelected() ? SELECTED_SECRET_VARIABLE_BASE64 : SELECTED_SECRET_VARIABLE);
            String returnVar = variableField.getVariable();
            if (returnVar.startsWith(Syntax.SYNTAX_PREFIX)) {
                String[] names = Syntax.getReferencedNames(variableField.getVariable().trim());
                if (names.length >= 1) returnVar = names[0];    // Only the first variable is used.
            }
            return returnVar;
        }

        secretSelection = SELECTED_SECRET_NONE;
        return "";  // Default if location isn't known or is NONE.
    }

    protected int getSecretSelection() {
        return secretSelection;
    }

    protected void updateAvailableSecrets(int availableSecrets, boolean signatureIsVariable) {

        // Start with AVAILABLE_SECRET_NONE
        privateKeyEnabled(false);
        variableEnabled(false);
        passwordEnabled(false);

        if (availableSecrets == AVAILABLE_SECRET_NONE) {
            noneSelected(true);
            return;
        }

        // Update the UI from bottom to top, with good reason:
        // This method is only called when the signature type changes.
        // Therefore, in order to always ensure the "top" selection is selected when the change is made,
        // we update the UI from bottom to top.
        // This always ensures the top entry is always selected.
        if ((availableSecrets & JwtUtilities.AVAILABLE_SECRET_VARIABLE) == AVAILABLE_SECRET_VARIABLE) {
            variableEnabled(true);
        }
        if ((availableSecrets & JwtUtilities.AVAILABLE_SECRET_PASSWORD) == AVAILABLE_SECRET_PASSWORD) {
            passwordEnabled(true);
        }
        if ((availableSecrets & JwtUtilities.AVAILABLE_SECRET_KEY) == AVAILABLE_SECRET_KEY) {
            privateKeyEnabled(true);
        }


        // Change the selection to an available secret based on availableSecrets.  If the existing selection is
        // supported by the new signature algorithm, leave it alone.
        // ie: For HMAC signatures, if Private Key or NONE is selected, change it to Variable as a default.
        //     If it's already Variable or Password, leave it alone.
        int currentSecretSelection = SELECTED_SECRET_NONE;

        if (privateKeyRadioButton.isSelected()) {
            currentSecretSelection = SELECTED_SECRET_KEY;
        } else if (passwordRadioButton.isSelected()) {
            currentSecretSelection = SELECTED_SECRET_PASSWORD;
        } else if (variableRadioButton.isSelected()) {
            currentSecretSelection = SELECTED_SECRET_VARIABLE;
        }
        if (signatureIsVariable) {
            // The default is variable unless a current Secret is set.
            if (currentSecretSelection == SELECTED_SECRET_NONE) {
                variableSelected(true);
            }
            return;
        }

        // We have the selection.  Does the current selection match available secrets?
        if ((availableSecrets & AVAILABLE_SECRET_KEY) == AVAILABLE_SECRET_KEY) {
            if (currentSecretSelection != SELECTED_SECRET_KEY) {
                privateKeySelected(true);
            }
        } else { //if ((currentSecretSelection & SELECTED_SECRET_VARIABLE) != SELECTED_SECRET_VARIABLE) {
            if ((currentSecretSelection != SELECTED_SECRET_VARIABLE) && (currentSecretSelection != SELECTED_SECRET_PASSWORD)) {
                variableSelected(true);
            }
        }
    }

    public void setAssertion(Assertion assertion, Assertion previousAssertion) {
        this.myAssertion = assertion;

        if (assertion != null) {
            variableField.setAssertion(this.myAssertion, previousAssertion);
        }
    }

    public Assertion getAssertion() {
        return myAssertion;
    }

    public void setPanelForDecode() {
        privateKeyRadioButton.setText("Public Key From Private Key");
        noSecretRadioButton.setVisible(true);
    }

    /**
     * Allows the private key controls to be enabled or disabled if needed.
     * (like if there are no private keys to select from)
     */
    private void privateKeyEnabled(boolean enable) {
        privateKeyRadioButton.setEnabled((ssgHasPrivateKeysAvailable() && enable));
        privateKeyCombo.setEnabled(privateKeyRadioButton.isSelected() && ssgHasPrivateKeysAvailable() && enable);
    }

    /**
     * Select/Deselect the private key controls, only if they're enabled because there's
     * private keys to select from.
     */
    private void privateKeySelected(boolean enable) {
        privateKeyRadioButton.setSelected(enable);
        privateKeyCombo.setEnabled(privateKeyRadioButton.isSelected() && enable);
        privateKeyCombo.setSelectedIndex(0);    // Tries to force the first key to always be selected as a default.
    }

    /**
     * Allows the password controls to be enabled or disabled if needed.
     * (like if there are no passwords to select from)
     */
    private void passwordEnabled(boolean enable) {
        passwordRadioButton.setEnabled((ssgHasPasswordsAvailable() && enable));
        passwordCombo.setEnabled(passwordRadioButton.isSelected() && ssgHasPasswordsAvailable() && enable);
    }

    /**
     * Select/Deselect the password controls, but only if they're enabled.
     */
    private void passwordSelected(boolean enable) {

        if (passwordRadioButton.isSelected() && (!ssgHasPasswordsAvailable())) {
            variableSelected(true); // There are no passwords any longer, reset to variable selected instead.
        }

        passwordRadioButton.setSelected(enable && ssgHasPasswordsAvailable());
        passwordCombo.setEnabled((passwordRadioButton.isEnabled() && enable && ssgHasPasswordsAvailable()));
    }

    private void variableSelected(boolean enable) {
        variableRadioButton.setSelected(enable);  // Needed when building the panel the first time.
        variableField.setEnabled(variableRadioButton.isSelected() && enable);
        variableBase64EncodedCheckbox.setEnabled(variableRadioButton.isSelected() && enable);
    }

    private void variableEnabled(boolean enable) {
        variableRadioButton.setEnabled(enable);
        variableField.setEnabled(variableRadioButton.isSelected() && enable);
        variableBase64EncodedCheckbox.setEnabled(variableRadioButton.isSelected() && enable);
    }

    private void noneSelected(boolean enable) {
        noSecretRadioButton.setSelected(enable);
    }

    private boolean ssgHasPasswordsAvailable() {
        return (passwordCombo.getItemCount() > (passwordHasTitleItem ? 1 : 0));
    }

    private boolean ssgHasPrivateKeysAvailable() {
        return (privateKeyCombo.getItemCount() > (keyHasTitleItem ? 1 : 0));
    }

}
