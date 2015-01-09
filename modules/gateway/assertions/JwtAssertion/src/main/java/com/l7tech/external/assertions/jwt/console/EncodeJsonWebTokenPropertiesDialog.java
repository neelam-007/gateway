package com.l7tech.external.assertions.jwt.console;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.jwt.EncodeJsonWebTokenAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionMetadata;


import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class EncodeJsonWebTokenPropertiesDialog extends AssertionPropertiesOkCancelSupport<EncodeJsonWebTokenAssertion> {
    private static final Logger logger = Logger.getLogger(EncodeJsonWebTokenPropertiesDialog.class.getName());

    private JPanel contentPane;
    private JComboBox signatureAlgorithmComboBox;

    private JComboBox keyManagementAlgorithmComboBox;
    private JComboBox contentEncryptionAlgorithmComboBox;
    private JTextField headersTextField;
    private JCheckBox showSignatureCheckbox;
    private JLabel signatureWarningLabel;
    private JTextField sourceVariableTextField;
    private JPasswordField signaturePasswordField;

    private JTextField encryptionKeyTextField;

    private JTextField signatureSourceVariable;
    private JTextField signatureAlgorithmKeyIdTextField;

    private TargetVariablePanel targetVariable;


    private JComboBox keyTypeComboBox;
    private JComboBox encryptionKeyType;
    private JTextField encryptionKeyId;
    private JRadioButton fromListRadioButton;
    private JRadioButton fromVariableRadioButton;
    private PrivateKeysComboBox privateKeysComboBox;
    private JLabel encryptionKeyWarningLabel;
    private JLabel signatureAlgorithmWarningLabel;
    private JComboBox headerActionComboBox;
    private JTabbedPane tabbedPane1;
    private JCheckBox signPayloadCheckBox;
    private JCheckBox encryptPayloadCheckBox;
    private JRadioButton secretRadioButton;
    private JPasswordField jweSharedSecret;
    private JCheckBox jweShowSecret;
    private JLabel jweSecretWarningLabel;
    private JRadioButton jweUseSecret;
    private JRadioButton jweUseVariable;

    private InputValidator validators;

    public EncodeJsonWebTokenPropertiesDialog(final Frame parent, final EncodeJsonWebTokenAssertion assertion) {
        super(EncodeJsonWebTokenAssertion.class, parent, String.valueOf(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME)), true);
        initComponents();

        setData(assertion);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        boolean showAll = false;
        if(Registry.getDefault().isAdminContextPresent()){
            try {
                ClusterProperty cp = Registry.getDefault().getClusterStatusAdmin().findPropertyByName(EncodeJsonWebTokenAssertion.CLUSTER_PROPERTY_SHOW_ALL);
                if(cp != null){
                    showAll = Boolean.valueOf(cp.getValue());
                }
            } catch (FindException e) {
                logger.warning("Could not find cluster property.");
            }
        }
        //headers
        headerActionComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.HEADER_ACTION.toArray(new String[JsonWebTokenConstants.HEADER_ACTION.size()])));

        headerActionComboBox.addActionListener(headerActionListener);

        //JWS
        //signature algo
        signPayloadCheckBox.addActionListener(signingActionListener);

        if(showAll){
            signatureAlgorithmComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.SIGNATURE_ALGORITHMS.values().toArray(new String[JsonWebTokenConstants.SIGNATURE_ALGORITHMS.size()])));
            keyManagementAlgorithmComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.KEY_MANAGEMENT_ALGORITHMS.values().toArray(new String[JsonWebTokenConstants.KEY_MANAGEMENT_ALGORITHMS.size()])));
            contentEncryptionAlgorithmComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.CONTENT_ENCRYPTION_ALGORITHMS.values().toArray(new String[JsonWebTokenConstants.CONTENT_ENCRYPTION_ALGORITHMS.size()])));
        } else {
            signatureAlgorithmComboBox.setModel(new DefaultComboBoxModel(getFilteredList(JsonWebTokenConstants.SIGNATURE_ALGORITHMS)));
            keyManagementAlgorithmComboBox.setModel(new DefaultComboBoxModel(getFilteredList(JsonWebTokenConstants.KEY_MANAGEMENT_ALGORITHMS)));
            contentEncryptionAlgorithmComboBox.setModel(new DefaultComboBoxModel(getFilteredList(JsonWebTokenConstants.CONTENT_ENCRYPTION_ALGORITHMS)));
        }


        signatureAlgorithmComboBox.addActionListener(signatureAlgorithmActionListener);

        secretRadioButton.addActionListener(privateKeyActionListener);
        fromVariableRadioButton.addActionListener(privateKeyActionListener);
        fromListRadioButton.addActionListener(privateKeyActionListener);

        PasswordGuiUtils.configureOptionalSecurePasswordField(signaturePasswordField, showSignatureCheckbox, signatureWarningLabel);
        keyTypeComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.SIGNATURE_KEY_TYPES.toArray(new String[JsonWebTokenConstants.SIGNATURE_KEY_TYPES.size()])));
        keyTypeComboBox.addActionListener(keyTypeActionListener);

        signatureAlgorithmWarningLabel.setVisible(false);

        //JWE
        encryptPayloadCheckBox.addActionListener(encryptionActionListener);
        keyManagementAlgorithmComboBox.addActionListener(keyManagmentAlgorithmActionListener);

        jweUseSecret.addActionListener(encryptionActionListener);
        jweUseVariable.addActionListener(encryptionActionListener);
        //cek
        contentEncryptionAlgorithmComboBox.addActionListener(encryptionActionListener);

        encryptionKeyType.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.ENCRYPTION_KEY_TYPES.toArray(new String[JsonWebTokenConstants.ENCRYPTION_KEY_TYPES.size()])));
        encryptionKeyType.addActionListener(encryptionKeyTypeActionListener);

        encryptionKeyWarningLabel.setVisible(false);

        PasswordGuiUtils.configureOptionalSecurePasswordField(jweSharedSecret, jweShowSecret, jweSecretWarningLabel);
        jweSecretWarningLabel.setVisible(false);

        //validators
        validators = new InputValidator(this, getTitle());

        //source payload
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (sourceVariableTextField.getText().trim().isEmpty()) {
                    return "Source Payload is required and can not be empty.";
                }
                return null;
            }
        });
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (headersTextField.isEnabled() && headersTextField.getText().trim().isEmpty()) {
                    return "Please specify where to obtain the headers value.";
                }
                return null;
            }
        });
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (signaturePasswordField.isEnabled() && signaturePasswordField.getPassword().length < 1) {
                    return "Key is required";
                }
                return null;
            }
        });
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (signatureSourceVariable.isEnabled() && signatureSourceVariable.getText().trim().isEmpty()) {
                    return "Key is required";
                }
                return null;
            }
        });
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (signatureAlgorithmKeyIdTextField.isEnabled() && signatureAlgorithmKeyIdTextField.getText().trim().isEmpty()) {
                    return "Key ID is required";
                }
                return null;
            }
        });
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (encryptionKeyTextField.isEnabled() && encryptionKeyTextField.getText().trim().isEmpty()) {
                    return "Certificate is required";
                }
                return null;
            }
        });
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!targetVariable.getVariableStatus().isOk()) {
                    return "Invalid Destination Variable Prefix value";
                }
                return null;
            }
        });
    }

    private String[] getFilteredList(Map<String, String> map) {
        java.util.List<String> filtered = Lists.newArrayList();
        for(Map.Entry<String, String> m : map.entrySet()){
            if(!JsonWebTokenConstants.UNSUPPORTED_ALGORITHMS.contains(m.getKey())){
                filtered.add(m.getValue());
            }
        }
        return filtered.toArray(new String[filtered.size()]);
    }

    @Override
    public void setData(final EncodeJsonWebTokenAssertion assertion) {
        //assertion to UI
        //general tab
        sourceVariableTextField.setText(assertion.getSourceVariable());
        headerActionComboBox.setSelectedItem(assertion.getHeaderAction());
        targetVariable.setVariable(assertion.getTargetVariable());
        targetVariable.setAssertion(assertion, getPreviousAssertion());

        if (!JsonWebTokenConstants.HEADERS_USE_DEFAULT.equals(headerActionComboBox.getSelectedItem())) {
            headersTextField.setText(assertion.getSourceHeaders());
        }
        //signature
        if(assertion.isSignPayload()){
            signPayloadCheckBox.setSelected(true);
            if(assertion.getSignatureAlgorithm() != null){
                signatureAlgorithmComboBox.setSelectedItem(JsonWebTokenConstants.SIGNATURE_ALGORITHMS.get(assertion.getSignatureAlgorithm()));
                signatureAlgorithmComboBox.setEnabled(true);
            }
            if(assertion.getSignatureSourceType() == JsonWebTokenConstants.SOURCE_SECRET){
                secretRadioButton.setSelected(true);
                signaturePasswordField.setText(assertion.getSignatureSecretKey());
            }
            else if(assertion.getSignatureSourceType() == JsonWebTokenConstants.SOURCE_PK){
                fromListRadioButton.setSelected(true);
                if (assertion.getPrivateKeyGoid() != null && !assertion.getPrivateKeyGoid().trim().isEmpty()) {
                    int sel = privateKeysComboBox.select(Goid.parseGoid(assertion.getPrivateKeyGoid()), assertion.getPrivateKeyAlias());
                    privateKeysComboBox.setSelectedIndex(sel < 0 ? 0 : sel);
                }
            }
            else if(assertion.getSignatureSourceType() == JsonWebTokenConstants.SOURCE_CV){
                fromVariableRadioButton.setEnabled(true);
                fromVariableRadioButton.setSelected(true);
                signatureSourceVariable.setEnabled(true);
                signatureSourceVariable.setText(assertion.getSignatureSourceVariable());
                keyTypeComboBox.setEnabled(true);
                if (assertion.getSignatureKeyType() != null) {
                    keyTypeComboBox.setSelectedItem(assertion.getSignatureKeyType());
                }
                signatureAlgorithmKeyIdTextField.setEnabled(JsonWebTokenConstants.KEY_TYPE_JWKS.equals(assertion.getSignatureKeyType()));
                if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(assertion.getSignatureKeyType())) {
                    signatureAlgorithmKeyIdTextField.setText(assertion.getSignatureJwksKeyId());
                }
            }
        }
        //encryption
        if(assertion.isEncryptPayload()){
            encryptPayloadCheckBox.setSelected(true);
            keyManagementAlgorithmComboBox.setEnabled(true);
            contentEncryptionAlgorithmComboBox.setEnabled(true);
            jweUseVariable.setEnabled(true);
            if (assertion.getKeyManagementAlgorithm() != null) {
                keyManagementAlgorithmComboBox.setSelectedItem(JsonWebTokenConstants.KEY_MANAGEMENT_ALGORITHMS.get(assertion.getKeyManagementAlgorithm()));
            }
            if(assertion.getEncryptionSourceType() == JsonWebTokenConstants.SOURCE_CV){
                jweUseVariable.setSelected(true);
                encryptionKeyTextField.setText(assertion.getEncryptionKey());
                if (assertion.getEncryptionKeyType() != null) {
                    encryptionKeyType.setSelectedItem(assertion.getEncryptionKeyType());
                }
                if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(assertion.getEncryptionKeyType())) {
                    encryptionKeyId.setText(assertion.getEncryptionKeyId());
                }
                if (assertion.getContentEncryptionAlgorithm() != null) {
                    contentEncryptionAlgorithmComboBox.setSelectedItem(JsonWebTokenConstants.CONTENT_ENCRYPTION_ALGORITHMS.get(assertion.getContentEncryptionAlgorithm()));
                }
            } else if(assertion.getEncryptionSourceType() == JsonWebTokenConstants.SOURCE_SECRET){
                jweUseSecret.setSelected(true);
                jweSharedSecret.setText(assertion.getEncryptionSecret());
            }
        }
    }

    @Override
    public EncodeJsonWebTokenAssertion getData(final EncodeJsonWebTokenAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
        //ui to assertion
        //general tab
        assertion.setSourceVariable(sourceVariableTextField.getText());
        assertion.setHeaderAction(headerActionComboBox.getSelectedItem().toString());
        if (!JsonWebTokenConstants.HEADERS_USE_DEFAULT.equals(headerActionComboBox.getSelectedItem())) {
            assertion.setSourceHeaders(headersTextField.getText().trim());
        }
        assertion.setTargetVariable(targetVariable.getVariable());

        //signing
        if(signPayloadCheckBox.isSelected()){
            assertion.setSignatureAlgorithm(JsonWebTokenConstants.SIGNATURE_ALGORITHMS.inverse().get(signatureAlgorithmComboBox.getSelectedItem().toString()));
            if(secretRadioButton.isSelected()){
                assertion.setSignatureSourceType(JsonWebTokenConstants.SOURCE_SECRET);
                assertion.setSignatureSecretKey(String.valueOf(signaturePasswordField.getPassword()));
                assertion.setSignatureSourceVariable(null);
                assertion.setSignatureKeyType(null);
                assertion.setSignatureJwksKeyId(null);
            }
            else if (fromListRadioButton.isSelected()) {
                assertion.setSignatureSourceType(JsonWebTokenConstants.SOURCE_PK);
                assertion.setPrivateKeyGoid(privateKeysComboBox.getSelectedKeystoreId().toHexString());
                assertion.setPrivateKeyAlias(privateKeysComboBox.getSelectedKeyAlias());
            } else if(fromVariableRadioButton.isSelected()) {
                assertion.setSignatureSourceType(JsonWebTokenConstants.SOURCE_CV);
                assertion.setSignatureKeyType(keyTypeComboBox.getSelectedItem().toString());
                if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyTypeComboBox.getSelectedItem())) {
                    assertion.setSignatureJwksKeyId(signatureAlgorithmKeyIdTextField.getText().trim());
                }
                assertion.setSignatureSourceVariable(signatureSourceVariable.getText().trim());
                //clear the other fields
                assertion.setSignatureSecretKey(null);
            }
        } else {
            assertion.setSignPayload(false);
            assertion.setSignatureAlgorithm(null);
            assertion.setSignatureSecretKey(null);
            assertion.setSignatureSourceVariable(null);
            assertion.setSignatureKeyType(null);
            assertion.setSignatureJwksKeyId(null);
            assertion.setPrivateKeyAlias(null);
            assertion.setPrivateKeyGoid(null);
        }
        //encrypting
        if (encryptPayloadCheckBox.isSelected()) {
            assertion.setKeyManagementAlgorithm(JsonWebTokenConstants.KEY_MANAGEMENT_ALGORITHMS.inverse().get(keyManagementAlgorithmComboBox.getSelectedItem().toString()));
            assertion.setContentEncryptionAlgorithm(JsonWebTokenConstants.CONTENT_ENCRYPTION_ALGORITHMS.inverse().get(contentEncryptionAlgorithmComboBox.getSelectedItem().toString()));

            if(jweUseSecret.isSelected()){
                assertion.setEncryptionSourceType(JsonWebTokenConstants.SOURCE_SECRET);
                assertion.setEncryptionSecret(String.valueOf(jweSharedSecret.getPassword()));
            } else if(jweUseVariable.isSelected()){
                assertion.setEncryptionSourceType(JsonWebTokenConstants.SOURCE_CV);
                if (encryptionKeyTextField.isEnabled()) {
                    assertion.setEncryptionKey(encryptionKeyTextField.getText().trim());
                }
                if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(encryptionKeyType.getSelectedItem())) {
                    assertion.setEncryptionKeyId(encryptionKeyId.getText().trim());
                }
                assertion.setEncryptionKeyType(encryptionKeyType.getSelectedItem().toString());
            }

        } else {
            //clear fields
            assertion.setEncryptPayload(false);
            assertion.setKeyManagementAlgorithm(null);
            assertion.setContentEncryptionAlgorithm(null);
            assertion.setEncryptionKey(null);
            assertion.setEncryptionKeyType(null);
            assertion.setEncryptionSecret(null);
        }
        assertion.setSignPayload(signPayloadCheckBox.isSelected());
        assertion.setEncryptPayload(encryptPayloadCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private RunOnChangeListener signatureAlgorithmActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            final Object sa = signatureAlgorithmComboBox.getSelectedItem();
            if(sa == null) return;

            //disable all fields and do nothing else
            if(sa.equals("None")){
                secretRadioButton.setEnabled(false);
                signaturePasswordField.setEnabled(false);
                showSignatureCheckbox.setEnabled(false);
                fromListRadioButton.setEnabled(false);
                privateKeysComboBox.setEnabled(false);
                fromVariableRadioButton.setEnabled(false);
                signatureSourceVariable.setEnabled(false);
                keyTypeComboBox.setEnabled(false);
                signatureAlgorithmKeyIdTextField.setEnabled(false);
            }
            //hmac type where a use of secret is allowed
            else if(sa.toString().startsWith("HMAC")){
                //select secret to fall back as we have from list selected
                secretRadioButton.setSelected(true && fromListRadioButton.isSelected());
                secretRadioButton.setEnabled(true);
                signaturePasswordField.setEnabled(secretRadioButton.isSelected());
                showSignatureCheckbox.setEnabled(secretRadioButton.isSelected());
                signatureWarningLabel.setVisible(true);

                //disable the private key fields
                fromListRadioButton.setEnabled(false);
                privateKeysComboBox.setEnabled(false);

                signatureSourceVariable.setEnabled(fromVariableRadioButton.isSelected());
            }
            //digital signature
            else {
                //disable secrets
                secretRadioButton.setEnabled(false);
                signaturePasswordField.setEnabled(false);
                showSignatureCheckbox.setEnabled(false);

                //if secret was selected previous, default to the list
                fromListRadioButton.setSelected(true && secretRadioButton.isSelected());
                fromListRadioButton.setEnabled(true);
                privateKeysComboBox.setEnabled(fromListRadioButton.isSelected());

                fromVariableRadioButton.setEnabled(true);
                signatureSourceVariable.setEnabled(fromVariableRadioButton.isSelected());

                final Object kty = keyTypeComboBox.getSelectedItem();
                signatureAlgorithmKeyIdTextField.setEnabled(fromVariableRadioButton.isSelected() && !sa.toString().startsWith("None") && !sa.toString().startsWith("HMAC") && JsonWebTokenConstants.KEY_TYPE_JWKS.equals(kty));

                boolean showWarning = false;
                if ("RSASSA-PKCS-v1_5 using SHA-256".equals(sa)) {
                    signatureAlgorithmWarningLabel.setText("<HTML><FONT COLOR=\"RED\">WARNING: The use of 'RSASSA-PKCS-v1_5 using SHA-256' is not recommended.");
                    showWarning = true;
                }
                if ("RSASSA-PKCS-v1_5 using SHA-384".equals(sa)) {
                    signatureAlgorithmWarningLabel.setText("<HTML><FONT COLOR=\"RED\">WARNING: The use of 'RSASSA-PKCS-v1_5 using SHA-384' is not recommended.");
                    showWarning = true;
                }
                if ("RSASSA-PKCS-v1_5 using SHA-512".equals(sa)) {
                    signatureAlgorithmWarningLabel.setText("<HTML><FONT COLOR=\"RED\">WARNING: The use of 'RSASSA-PKCS-v1_5 using SHA-512' is not recommended.");
                    showWarning = true;
                }
                signatureAlgorithmWarningLabel.setVisible(showWarning);

                //disable secret fields
                signaturePasswordField.setEnabled(false);
                showSignatureCheckbox.setEnabled(false);
                signatureWarningLabel.setVisible(false);

                EncodeJsonWebTokenPropertiesDialog.this.pack();
            }
        }
    });

    private RunOnChangeListener keyManagmentAlgorithmActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            final Object km = keyManagementAlgorithmComboBox.getSelectedItem();
            jweUseSecret.setEnabled(encryptPayloadCheckBox.isSelected() && km.toString().startsWith("Direct use"));

            jweSharedSecret.setEnabled(jweUseSecret.isSelected() && km.toString().startsWith("Direct use"));
            jweShowSecret.setEnabled(jweUseSecret.isSelected() && km.toString().startsWith("Direct use"));
            jweSecretWarningLabel.setVisible(jweUseSecret.isSelected() && km.toString().startsWith("Direct use"));

            jweUseVariable.setSelected(!km.toString().startsWith("Direct use"));

            final java.util.List<String> values = Lists.newArrayList(JsonWebTokenConstants.ENCRYPTION_KEY_TYPES);
            if(km.toString().startsWith("Direct use")){
                values.remove("Certificate");
            }
            encryptionKeyType.setModel(new DefaultComboBoxModel(values.toArray(new String[values.size()])));

            encryptionKeyWarningLabel.setVisible("RSAES-PKCS1-V1_5".equals(km));
            if ("RSAES-PKCS1-V1_5".equals(km)) {
                encryptionKeyWarningLabel.setText("<HTML><FONT COLOR=\"RED\">WARNING: The use of 'RSAES-PKCS1-V1_5' is not recommended.");
            }
            EncodeJsonWebTokenPropertiesDialog.this.pack();
        }
    });

    private RunOnChangeListener headerActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            headersTextField.setEnabled(!headerActionComboBox.getSelectedItem().equals(JsonWebTokenConstants.HEADERS_USE_DEFAULT));
        }
    });

    private RunOnChangeListener encryptionActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            jweUseVariable.setEnabled(encryptPayloadCheckBox.isSelected());

            jweSharedSecret.setEnabled(encryptPayloadCheckBox.isSelected() && jweUseSecret.isSelected());
            jweShowSecret.setEnabled(encryptPayloadCheckBox.isSelected() && jweUseSecret.isSelected());
            jweSecretWarningLabel.setVisible(encryptPayloadCheckBox.isSelected() && jweUseSecret.isSelected());


            keyManagementAlgorithmComboBox.setEnabled(encryptPayloadCheckBox.isSelected());

            encryptionKeyTextField.setEnabled(encryptPayloadCheckBox.isSelected() && jweUseVariable.isSelected());
            encryptionKeyType.setEnabled(encryptPayloadCheckBox.isSelected() && jweUseVariable.isSelected());
            encryptionKeyId.setEnabled(encryptPayloadCheckBox.isSelected()  && jweUseVariable.isSelected() && encryptionKeyType.getSelectedItem().equals(JsonWebTokenConstants.KEY_TYPE_JWKS));

            contentEncryptionAlgorithmComboBox.setEnabled(encryptPayloadCheckBox.isSelected());
        }
    });

    private RunOnChangeListener keyTypeActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            final Object s = keyTypeComboBox.getSelectedItem();
            signatureAlgorithmKeyIdTextField.setEnabled(JsonWebTokenConstants.KEY_TYPE_JWKS.equals(s));
        }
    });

    private RunOnChangeListener encryptionKeyTypeActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            final Object s = encryptionKeyType.getSelectedItem();
            encryptionKeyId.setEnabled(JsonWebTokenConstants.KEY_TYPE_JWKS.equals(s));
        }
    });

    private RunOnChangeListener privateKeyActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            //secret
            signaturePasswordField.setEnabled(secretRadioButton.isSelected());
            showSignatureCheckbox.setEnabled(secretRadioButton.isSelected());
            //pk from list
            privateKeysComboBox.setEnabled(fromListRadioButton.isSelected());
            //from context var
            signatureSourceVariable.setEnabled(fromVariableRadioButton.isSelected());
            keyTypeComboBox.setEnabled(fromVariableRadioButton.isSelected());
            signatureAlgorithmKeyIdTextField.setEnabled(fromVariableRadioButton.isSelected() && JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyTypeComboBox.getSelectedItem()));
        }
    });

    private RunOnChangeListener signingActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            signatureAlgorithmComboBox.setEnabled(signPayloadCheckBox.isSelected());

            final String sa = signatureAlgorithmComboBox.getSelectedItem().toString();

            //secret
            secretRadioButton.setEnabled(signPayloadCheckBox.isSelected() && sa.startsWith("HMAC"));
            secretRadioButton.setSelected(signPayloadCheckBox.isSelected() && sa.startsWith("HMAC"));

            signaturePasswordField.setEnabled(signPayloadCheckBox.isSelected() && secretRadioButton.isSelected());
            showSignatureCheckbox.setEnabled(signPayloadCheckBox.isSelected() && secretRadioButton.isSelected());

            //private keys combobox
            fromListRadioButton.setEnabled(signPayloadCheckBox.isSelected() && !sa.startsWith("HMAC"));
            fromListRadioButton.setSelected(signPayloadCheckBox.isSelected() && !sa.startsWith("HMAC"));
            privateKeysComboBox.setEnabled(signPayloadCheckBox.isSelected() && fromListRadioButton.isSelected() && !sa.startsWith("HMAC"));

            //jwk/jwks fields
            fromVariableRadioButton.setEnabled(signPayloadCheckBox.isSelected());
            signatureSourceVariable.setEnabled(signPayloadCheckBox.isSelected() && fromVariableRadioButton.isSelected());
            keyTypeComboBox.setEnabled(signPayloadCheckBox.isSelected() && fromVariableRadioButton.isSelected());
            signatureAlgorithmKeyIdTextField.setEnabled(signPayloadCheckBox.isSelected() && fromVariableRadioButton.isSelected() && keyTypeComboBox.getSelectedItem().equals(JsonWebTokenConstants.KEY_TYPE_JWKS));
        }
    });
}
