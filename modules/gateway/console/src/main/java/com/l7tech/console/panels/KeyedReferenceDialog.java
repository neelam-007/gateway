package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.uddi.UDDIKeyedReference;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Set;

public class KeyedReferenceDialog extends JDialog {
    
    public KeyedReferenceDialog(final Window owner,
                                final Set<UDDIKeyedReference> existingRefs) {
        super(owner, resources.getString("dialog.title"));
        setContentPane(contentPane);
        this.existingRefs = existingRefs;
        isUpdate = false;
        initialize();
    }

    public KeyedReferenceDialog(final Window owner,
                                final UDDIKeyedReference keyedReference,
                                final Set<UDDIKeyedReference> existingRefs) {
        super(owner, resources.getString("dialog.title.edit"));
        setContentPane(contentPane);
        this.existingRefs = existingRefs;
        isUpdate = true;
        initialize();
        tModelKeyTextField.setText(keyedReference.getTModelKey());
        keyValueTextField.setText(keyedReference.getKeyValue());
        keyNameTextField.setText(keyedReference.getKeyName());
        incomingKeyRef = keyedReference;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public UDDIKeyedReference getKeyedReference() {
        return new UDDIKeyedReference(
                getTModelKey(),
                getKeyName(),
                getKeyValue());
    }

    private String getTModelKey(){
        return tModelKeyTextField.getText().trim();
    }

    private String getKeyName(){
        return keyNameTextField.getText().trim().replaceAll(" ", "");
    }

    private String getKeyValue(){
        return keyValueTextField.getText().trim().replaceAll(" ", "");
    }
    // - PRIVATE

    private void initialize(){
        if (getOwner() == null)
            Utilities.setAlwaysOnTop(this, true);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String error = validateValues();
                if(error != null){
                    DialogDisplayer.showMessageDialog(KeyedReferenceDialog.this, error,
                        "Invalid value", JOptionPane.WARNING_MESSAGE, null);
                } else {
                    confirmed = true;
                    dispose();
                }
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.centerOnScreen(this);
    }

    private String validateValues(){
        final String tModelKey = getTModelKey();

        if(tModelKey.isEmpty()){
            return resources.getString("tmodelkey.is.required");
        }

        if(!ValidationUtils.isValidUri(tModelKey)){
            return resources.getString("tmodelKey.invalid");
        }

        if(tModelKey.length() > 255){
            final String msg = resources.getString("field.maxLength");
            return MessageFormat.format(msg, new Object[]{"tModelKey", tModelKey.length()});
        }

        final String keyValue = getKeyValue();
        if (keyValue.isEmpty()){
            return resources.getString("keyvalue.is.required");
        }

        if(keyValue.length() > 255){
            final String msg = resources.getString("field.maxLength");
            return MessageFormat.format(msg, new Object[]{"keyValue", keyValue.length()});
        }

        final String keyName = getKeyName();
        if(tModelKey.equals(UDDIKeyedReference.GENERAL_KEYWORDS) && (keyName == null || keyName.isEmpty())) {
            return resources.getString("keyName.required");
        }

        if(keyName.length() > 255){
            final String msg = resources.getString("field.maxLength");
            return MessageFormat.format(msg, new Object[]{"keyName", keyName.length()});
        }

        final UDDIKeyedReference keyRef = getKeyedReference();
        final String duplicateErrorMsg = resources.getString("duplicate.error.msg");
        if(isUpdate){
            final boolean refChanged = !keyRef.equals(incomingKeyRef);
            if(refChanged){
                if(existingRefs.contains(keyRef)){
                    return duplicateErrorMsg;
                }
            }
        } else {
            if(existingRefs.contains(keyRef)){
                return duplicateErrorMsg;
            }
        }

        return null;
    }

    private JPanel contentPane;
    private JTextField tModelKeyTextField;
    private JTextField keyValueTextField;
    private JTextField keyNameTextField;
    private JButton buttonOK;
    private JButton buttonCancel;
    private boolean confirmed;
    private final Set<UDDIKeyedReference> existingRefs;
    private UDDIKeyedReference incomingKeyRef = null;
    private final boolean isUpdate;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.KeyedReferenceDialog");
}
