package com.l7tech.external.assertions.jwt.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.external.assertions.jwt.EncodeJsonWebTokenAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;

public class EncodeJsonWebTokenPropertiesDialog extends AssertionPropertiesOkCancelSupport<EncodeJsonWebTokenAssertion> {

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

    private JPanel signaturePrivateKeyPanel;
    private JPanel signatureSecretPanel;
    private JComboBox keyTypeComboBox;
    private JComboBox encryptionKeyType;
    private JTextField encryptionKeyId;
    private JRadioButton fromListRadioButton;
    private JRadioButton fromVariableRadioButton;
    private PrivateKeysComboBox privateKeysComboBox;
    private JLabel encryptionKeyWarningLabel;
    private JLabel signatureAlgorithmWarningLabel;
    private JComboBox headerActionComboBox;

    private InputValidator validators;

    public EncodeJsonWebTokenPropertiesDialog(final Frame parent, final EncodeJsonWebTokenAssertion assertion) {
        super(EncodeJsonWebTokenAssertion.class, parent, String.valueOf(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME)), true);
        initComponents();

        setData(assertion);
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        privateKeysComboBox.setIncludeDefaultSslKey(false);
        privateKeysComboBox.repopulate();

        //headers
        headerActionComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.HEADER_ACTION.toArray(new String[JsonWebTokenConstants.HEADER_ACTION.size()])));

        headerActionComboBox.addActionListener(headerActionListener);

        //signature algo
        signatureAlgorithmComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.SIGNATURE_ALGORITHMS.values().toArray(new String[JsonWebTokenConstants.SIGNATURE_ALGORITHMS.size()])));
        signatureAlgorithmComboBox.addActionListener(signatureAlgorithmActionListener);

        fromVariableRadioButton.addActionListener(privateKeyActionListener);
        fromListRadioButton.addActionListener(privateKeyActionListener);

        PasswordGuiUtils.configureOptionalSecurePasswordField(signaturePasswordField, showSignatureCheckbox, signatureWarningLabel);
        keyTypeComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.SIGNATURE_KEY_TYPES.toArray(new String[JsonWebTokenConstants.SIGNATURE_KEY_TYPES.size()])));
        keyTypeComboBox.addActionListener(keyTypeActionListener);

        //key management
        keyManagementAlgorithmComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.KEY_MANAGEMENT_ALGORITHMS.values().toArray(new String[JsonWebTokenConstants.KEY_MANAGEMENT_ALGORITHMS.size()])));
        keyManagementAlgorithmComboBox.addActionListener(keyManagmentAlgorithmActionListener);
        keyManagementAlgorithmComboBox.addActionListener(encryptionActionListener);

        //cek
        contentEncryptionAlgorithmComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.CONTENT_ENCRYPTION_ALGORITHMS.values().toArray(new String[JsonWebTokenConstants.CONTENT_ENCRYPTION_ALGORITHMS.size()])));
        contentEncryptionAlgorithmComboBox.addActionListener(encryptionActionListener);

        encryptionKeyType.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.ENCRYPTION_KEY_TYPES.toArray(new String[JsonWebTokenConstants.ENCRYPTION_KEY_TYPES.size()])));
        encryptionKeyType.addActionListener(encryptionKeyTypeActionListener);

        encryptionKeyWarningLabel.setVisible(false);

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

    @Override
    public void setData(final EncodeJsonWebTokenAssertion assertion) {
        //assertion to UI
        sourceVariableTextField.setText(assertion.getSourceVariable());
        //headers
        headerActionComboBox.setSelectedItem(assertion.getHeaderAction());
        if (!JsonWebTokenConstants.HEADERS_USE_DEFAULT.equals(headerActionComboBox.getSelectedItem())) {
            headersTextField.setText(assertion.getSourceHeaders());
        }

        //signature
        if (assertion.getSignatureAlgorithm() != null) {
            signatureAlgorithmComboBox.setSelectedItem(JsonWebTokenConstants.SIGNATURE_ALGORITHMS.get(assertion.getSignatureAlgorithm()));
        }

        signaturePasswordField.setText(assertion.getSignatureSecretKey());
        signatureSourceVariable.setText(assertion.getSignatureSourceVariable());

        if (assertion.isPrivateKeyFromVariable()) {
            fromVariableRadioButton.setSelected(true);
            signatureSourceVariable.setEnabled(true);
            keyTypeComboBox.setEnabled(true);
            privateKeysComboBox.setEnabled(false);
            signatureAlgorithmKeyIdTextField.setEnabled(JsonWebTokenConstants.KEY_TYPE_JWKS.equals(assertion.getSignatureKeyType()));
            if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(assertion.getSignatureKeyType())) {
                signatureAlgorithmKeyIdTextField.setText(assertion.getSignatureJwksKeyId());
            }
            if (assertion.getSignatureKeyType() != null) {
                keyTypeComboBox.setSelectedItem(assertion.getSignatureKeyType());
            }
        } else {
            fromListRadioButton.setSelected(true);
            if (assertion.getPrivateKeyGoid() != null && !assertion.getPrivateKeyGoid().trim().isEmpty()) {
                int sel = privateKeysComboBox.select(Goid.parseGoid(assertion.getPrivateKeyGoid()), assertion.getPrivateKeyAlias());
                privateKeysComboBox.setSelectedIndex(sel < 0 ? 0 : sel);
            }
        }

        if (assertion.getKeyManagementAlgorithm() != null) {
            keyManagementAlgorithmComboBox.setSelectedItem(JsonWebTokenConstants.KEY_MANAGEMENT_ALGORITHMS.get(assertion.getKeyManagementAlgorithm()));
        }
        if (assertion.getContentEncryptionAlgorithm() != null) {
            contentEncryptionAlgorithmComboBox.setSelectedItem(JsonWebTokenConstants.CONTENT_ENCRYPTION_ALGORITHMS.get(assertion.getContentEncryptionAlgorithm()));
        }

        encryptionKeyTextField.setText(assertion.getEncryptionKey());
        if (assertion.getEncryptionKeyType() != null) {
            encryptionKeyType.setSelectedItem(assertion.getEncryptionKeyType());
        }
        if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(assertion.getEncryptionKeyType())) {
            encryptionKeyId.setText(assertion.getEncryptionKeyId());
        }

        targetVariable.setVariable(assertion.getTargetVariable());
        targetVariable.setAssertion(assertion, getPreviousAssertion());
    }

    @Override
    public EncodeJsonWebTokenAssertion getData(final EncodeJsonWebTokenAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
        //ui to assertion
        assertion.setSourceVariable(sourceVariableTextField.getText());
        assertion.setHeaderAction(headerActionComboBox.getSelectedItem().toString());
        if (!JsonWebTokenConstants.HEADERS_USE_DEFAULT.equals(headerActionComboBox.getSelectedItem())) {
            assertion.setSourceHeaders(headersTextField.getText().trim());
        }

        final String algo = signatureAlgorithmComboBox.getSelectedItem().toString();
        assertion.setSignatureAlgorithm(JsonWebTokenConstants.SIGNATURE_ALGORITHMS.inverse().get(algo));

        if (!"None".equals(algo) && algo.startsWith("HMAC")) {
            assertion.setSignatureSecretKey(String.valueOf(signaturePasswordField.getPassword()));
            //clear the other fields
            assertion.setSignatureSourceVariable(null);
            assertion.setSignatureKeyType(null);
            assertion.setSignatureJwksKeyId(null);
        } else if (!"None".equals(algo) && !algo.startsWith("HMAC")) {
            if (fromListRadioButton.isSelected()) {
                assertion.setPrivateKeyFromVariable(false);
                assertion.setPrivateKeyGoid(privateKeysComboBox.getSelectedKeystoreId().toHexString());
                assertion.setPrivateKeyAlias(privateKeysComboBox.getSelectedKeyAlias());
            } else {
                assertion.setPrivateKeyFromVariable(true);
                assertion.setSignatureKeyType(keyTypeComboBox.getSelectedItem().toString());
                if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyTypeComboBox.getSelectedItem())) {
                    assertion.setSignatureJwksKeyId(signatureAlgorithmKeyIdTextField.getText().trim());
                }
            }
            assertion.setSignatureSourceVariable(signatureSourceVariable.getText().trim());
            //clear the other fields
            assertion.setSignatureSecretKey(null);
        }

        assertion.setKeyManagementAlgorithm(JsonWebTokenConstants.KEY_MANAGEMENT_ALGORITHMS.inverse().get(keyManagementAlgorithmComboBox.getSelectedItem().toString()));
        assertion.setContentEncryptionAlgorithm(JsonWebTokenConstants.CONTENT_ENCRYPTION_ALGORITHMS.inverse().get(contentEncryptionAlgorithmComboBox.getSelectedItem().toString()));
        if(encryptionKeyTextField.isEnabled()){
            assertion.setEncryptionKey(encryptionKeyTextField.getText().trim());
        }
        if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(encryptionKeyType.getSelectedItem())) {
            assertion.setEncryptionKeyId(encryptionKeyId.getText().trim());
        }
        assertion.setEncryptionKeyType(encryptionKeyType.getSelectedItem().toString());

        assertion.setTargetVariable(targetVariable.getVariable());
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
            signaturePrivateKeyPanel.setVisible(!"None".equals(sa));

            signaturePasswordField.setEnabled(sa.toString().startsWith("HMAC"));
            showSignatureCheckbox.setEnabled(sa.toString().startsWith("HMAC"));
            signatureSecretPanel.setVisible(sa.toString().startsWith("HMAC"));
            signatureWarningLabel.setVisible(sa.toString().startsWith("HMAC"));

            signaturePrivateKeyPanel.setVisible(!sa.toString().startsWith("None") && !sa.toString().startsWith("HMAC"));
            signatureSourceVariable.setEnabled(fromVariableRadioButton.isSelected() && !sa.toString().startsWith("None") && !sa.toString().startsWith("HMAC"));

            final Object kty = keyTypeComboBox.getSelectedItem();
            signatureAlgorithmKeyIdTextField.setEnabled(fromVariableRadioButton.isSelected() && !sa.toString().startsWith("None") && !sa.toString().startsWith("HMAC") && JsonWebTokenConstants.KEY_TYPE_JWKS.equals(kty));

            if ("RSASSA-PKCS-v1_5 using SHA-256".equals(sa)) {
                signatureAlgorithmWarningLabel.setText("<HTML><FONT COLOR=\"RED\">WARNING: Please use 'RSASSA-PSS using SHA-256 and MGF1 with SHA-256'.");
            }
            if ("RSASSA-PKCS-v1_5 using SHA-384".equals(sa)) {
                signatureAlgorithmWarningLabel.setText("<HTML><FONT COLOR=\"RED\">WARNING: Please use 'RSASSA-PSS using SHA-384 and MGF1 with SHA-384'.");
            }
            if ("RSASSA-PKCS-v1_5 using SHA-512".equals(sa)) {
                signatureAlgorithmWarningLabel.setText("<HTML><FONT COLOR=\"RED\">WARNING: Please use 'RSASSA-PSS using SHA-512 and MGF1 with SHA-512'.");
            }
            signatureAlgorithmWarningLabel.setVisible("<HTML><FONT COLOR=\"RED\">WARNING: RSASSA-PKCS-v1_5 using SHA-256".equals(sa) || "RSASSA-PKCS-v1_5 using SHA-384".equals(sa) || "RSASSA-PKCS-v1_5 using SHA-512".equals(sa));

            EncodeJsonWebTokenPropertiesDialog.this.pack();
        }
    });

    private RunOnChangeListener keyManagmentAlgorithmActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            final Object km = keyManagementAlgorithmComboBox.getSelectedItem();
            contentEncryptionAlgorithmComboBox.setEnabled(!"None".equals(km));
            encryptionKeyWarningLabel.setVisible("RSAES-PKCS1-V1_5".equals(km));
            if ("RSAES-PKCS1-V1_5".equals(km)) {
                encryptionKeyWarningLabel.setText("<HTML><FONT COLOR=\"RED\">WARNING: Please use 'RSAES OAEP using SHA-256 and MGF1 with SHA-256'.");
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
            final Object km = keyManagementAlgorithmComboBox.getSelectedItem();
            encryptionKeyTextField.setEnabled(!"None".equals(km));
            encryptionKeyType.setEnabled(!"None".equals(km));
            encryptionKeyId.setEnabled(!"None".equals(km) && encryptionKeyType.getSelectedItem().equals(JsonWebTokenConstants.KEY_TYPE_JWKS));
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
            privateKeysComboBox.setEnabled(fromListRadioButton.isSelected());

            signatureSourceVariable.setEnabled(fromVariableRadioButton.isSelected());
            keyTypeComboBox.setEnabled(fromVariableRadioButton.isSelected());
            signatureAlgorithmKeyIdTextField.setEnabled(fromVariableRadioButton.isSelected() && JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyTypeComboBox.getSelectedItem()));
        }
    });
}
