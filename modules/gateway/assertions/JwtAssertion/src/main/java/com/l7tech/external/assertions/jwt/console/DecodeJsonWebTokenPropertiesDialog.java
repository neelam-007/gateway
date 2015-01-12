package com.l7tech.external.assertions.jwt.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.external.assertions.jwt.DecodeJsonWebTokenAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class DecodeJsonWebTokenPropertiesDialog extends AssertionPropertiesOkCancelSupport<DecodeJsonWebTokenAssertion> {

    private JPanel contentPane;
    private JTextField sourcePayloadTextField;
    private TargetVariablePanel targetVariable;

    private JTextField sourceVariableTextField;
    private JComboBox keyType;
    private JTextField keyIdTextField;
    private PrivateKeysComboBox privateKeysComboBox;
    private JPasswordField secretPasswordField;
    private JCheckBox showPasswordCheckBox;
    private JLabel secretWarningLabel;

    private JComboBox validationType;



    public DecodeJsonWebTokenPropertiesDialog(final Frame parent, final DecodeJsonWebTokenAssertion assertion) {
        super(DecodeJsonWebTokenAssertion.class, parent, String.valueOf(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME)), true);
        initComponents();
        setData(assertion);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        getOkButton().setEnabled(false);
        validationType.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.VALIDATION_TYPE.toArray(new String[JsonWebTokenConstants.VALIDATION_TYPE.size()])));
        validationType.addActionListener(validationTypeActionListener);

        keyType.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.ENCRYPTION_KEY_TYPES.toArray(new String[JsonWebTokenConstants.ENCRYPTION_KEY_TYPES.size()])));
        keyType.addActionListener(keyTypeActionListener);

        PasswordGuiUtils.configureOptionalSecurePasswordField(secretPasswordField, showPasswordCheckBox, secretWarningLabel);

        sourcePayloadTextField.getDocument().addDocumentListener(documentListener);
        secretPasswordField.getDocument().addDocumentListener(documentListener);
        sourceVariableTextField.getDocument().addDocumentListener(documentListener);
        keyIdTextField.getDocument().addDocumentListener(documentListener);
        targetVariable.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateOkButtonState();
            }
        });

    }

    @Override
    public void setData(final DecodeJsonWebTokenAssertion assertion) {
        //assertion to UI
        sourcePayloadTextField.setText(assertion.getSourcePayload());

        if (assertion.getValidationType() == null || assertion.getValidationType().trim().isEmpty()) {
            validationType.setSelectedIndex(0);
        } else {
            validationType.setSelectedItem(assertion.getValidationType());
        }

        if (JsonWebTokenConstants.VALIDATION_USING_SECRET.equals(assertion.getValidationType())) {
            secretPasswordField.setText(assertion.getSignatureSecret());
        } else if (JsonWebTokenConstants.VALIDATION_USING_PK.equals(assertion.getValidationType())) {
            if (assertion.getPrivateKeyGoid() == null || assertion.getPrivateKeyGoid().trim().isEmpty()) {
                privateKeysComboBox.setSelectedIndex(0);
            } else {
                final int sel = privateKeysComboBox.select(Goid.parseGoid(assertion.getPrivateKeyGoid()), assertion.getPrivateKeyAlias());
                privateKeysComboBox.setSelectedIndex(sel < 0 ? 0 : sel);
            }
        } else if (JsonWebTokenConstants.VALIDATION_USING_CV.equals(assertion.getValidationType())) {
            sourceVariableTextField.setText(assertion.getPrivateKeySource());
            if (assertion.getKeyType() == null || assertion.getKeyType().trim().isEmpty()) {
                keyType.setSelectedIndex(0);
            } else {
                keyType.setSelectedItem(assertion.getKeyType());
            }
            if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(assertion.getKeyType())) {
                keyIdTextField.setText(assertion.getKeyId());
            }
        }

        targetVariable.setVariable(assertion.getTargetVariablePrefix());
        targetVariable.setAssertion(assertion, getPreviousAssertion());
    }

    @Override
    public DecodeJsonWebTokenAssertion getData(final DecodeJsonWebTokenAssertion assertion) throws ValidationException {
        assertion.setSourcePayload(sourcePayloadTextField.getText().trim());

        final String vt = validationType.getSelectedItem().toString();
        assertion.setValidationType(vt);
        if (JsonWebTokenConstants.VALIDATION_USING_SECRET.equals(vt)) {
            assertion.setSignatureSecret(String.valueOf(secretPasswordField.getPassword()));
        } else if (JsonWebTokenConstants.VALIDATION_USING_PK.equals(vt)) {
            assertion.setPrivateKeyGoid(privateKeysComboBox.getSelectedKeystoreId().toHexString());
            assertion.setPrivateKeyAlias(privateKeysComboBox.getSelectedKeyAlias());
        } else if (JsonWebTokenConstants.VALIDATION_USING_CV.equals(vt)) {
            assertion.setPrivateKeySource(sourceVariableTextField.getText().trim());
            assertion.setKeyType(keyType.getSelectedItem().toString());
            if (JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyType.getSelectedItem())) {
                assertion.setKeyId(keyIdTextField.getText().trim());
            }
        }
        assertion.setTargetVariablePrefix(targetVariable.getVariable());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private RunOnChangeListener validationTypeActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            final String sel = validationType.getSelectedItem().toString();

            secretPasswordField.setEnabled(!JsonWebTokenConstants.VALIDATION_NONE.equals(sel) && JsonWebTokenConstants.VALIDATION_USING_SECRET.equals(sel));
            showPasswordCheckBox.setEnabled(!JsonWebTokenConstants.VALIDATION_NONE.equals(sel) && JsonWebTokenConstants.VALIDATION_USING_SECRET.equals(sel));

            privateKeysComboBox.setEnabled(!JsonWebTokenConstants.VALIDATION_NONE.equals(sel) && JsonWebTokenConstants.VALIDATION_USING_PK.equals(sel));

            sourceVariableTextField.setEnabled(!JsonWebTokenConstants.VALIDATION_NONE.equals(sel) && JsonWebTokenConstants.VALIDATION_USING_CV.equals(sel));
            keyType.setEnabled(!JsonWebTokenConstants.VALIDATION_NONE.equals(sel) && JsonWebTokenConstants.VALIDATION_USING_CV.equals(sel));
            keyIdTextField.setEnabled(!JsonWebTokenConstants.VALIDATION_NONE.equals(sel) && (JsonWebTokenConstants.VALIDATION_USING_CV.equals(sel)) && JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyType.getSelectedItem()));

            secretWarningLabel.setVisible(JsonWebTokenConstants.VALIDATION_USING_SECRET.equals(sel));
            updateOkButtonState();
        }
    });

    private RunOnChangeListener keyTypeActionListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            keyIdTextField.setEnabled(JsonWebTokenConstants.KEY_TYPE_JWKS.equals(keyType.getSelectedItem()));
        }
    });

    private DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            updateOkButtonState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateOkButtonState();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateOkButtonState();
        }
    };

    private void updateOkButtonState(){
        if(!targetVariable.isEntryValid()){
            getOkButton().setEnabled(false);
            return;
        }
        if(sourcePayloadTextField.getText().trim().isEmpty()){
            getOkButton().setEnabled(false);
            return;
        }
        if(secretPasswordField.isEnabled() && secretPasswordField.getPassword().length == 0){
            getOkButton().setEnabled(false);
            return;
        }
        if(sourceVariableTextField.isEnabled() && sourceVariableTextField.getText().trim().isEmpty()){
            getOkButton().setEnabled(false);
            return;
        }
        if(keyIdTextField.isEnabled() && keyIdTextField.getText().trim().isEmpty()){
            getOkButton().setEnabled(false);
            return;
        }
        getOkButton().setEnabled(true);
    }

    protected void updateOkButtonEnableState() {
        //do nothing - so i can disable the ok button initially
    }
}
