package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.uddi.UDDIKeyedReference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
                tModelKeyTextField.getText(),
                keyNameTextField.getText(),
                keyValueTextField.getText());
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
        final String duplicateErrorMsg = resources.getString("duplicate.error.msg");
        String error = null;
        final String tModelKey = tModelKeyTextField.getText();
        final String keyName = keyNameTextField.getText();

        if(tModelKey.trim().isEmpty()){
            error = resources.getString("tmodelkey.is.required");
        } else if (keyValueTextField.getText().trim().isEmpty()){
            error = resources.getString("keyvalue.is.required");
        } else if(tModelKey.equals(UDDIKeyedReference.GENERAL_KEYWORDS) && (keyName == null || keyName.trim().isEmpty())) {
            error = "A value for keyName is required when the tModelKey represents a general keywords reference.";
        } else {
            final UDDIKeyedReference keyRef = getKeyedReference();
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
        }

        return error;
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
