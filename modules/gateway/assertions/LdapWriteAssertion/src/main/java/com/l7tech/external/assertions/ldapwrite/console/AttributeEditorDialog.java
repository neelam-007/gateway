package com.l7tech.external.assertions.ldapwrite.console;

import com.l7tech.external.assertions.ldapwrite.LdifAttribute;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

public class AttributeEditorDialog extends JDialog {

    private static String DIALOG_TITLE_PROP = "dialog.title";
    private static String DIALOG_ERROR_UNABLE_TO_SAVE_TITLE_PROP = "dialog.save.error.title";
    private static String DIALOG_ERROR_MSG_UNABLE_TO_SAVE_KEY_PROP = "dialog.save.key.error";
    private static String DIALOG_ERROR_MSG_UNABLE_TO_SAVE_VALUE_PROP = "dialog.save.value.error";

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField keyTextField;
    private JTextField valueTextField;
    private LdifAttribute ldifAttribute;
    private boolean wasOKed;
    private ResourceBundle resourceBundle;

    public AttributeEditorDialog(final Frame owner, final LdifAttribute ldifAttribute) {

        super(owner, true);
        this.ldifAttribute = ldifAttribute;
        initialize();
    }

    private void initialize() {

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        resourceBundle = ResourceBundle.getBundle( getClass().getName());
        setTitle(resourceBundle.getString( DIALOG_TITLE_PROP));

        keyTextField.setText(this.ldifAttribute.getKey());

        //Listen to the keyTextField to enable/disable controls.
        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(keyTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                enableDisableControls();
            }
        }, 300);

        addWindowListener( new WindowAdapter() {
            public void windowOpened( WindowEvent e ){
                keyTextField.requestFocus();
            }
        });

        valueTextField.setText(this.ldifAttribute.getValue());
        enableDisableControls();

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onCancel();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                onCancel();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

    }


    private void onOK() {

        if (!validateInput()) {
            return;
        }

        ldifAttribute = new LdifAttribute(keyTextField.getText().trim(), valueTextField.getText().trim());

        wasOKed = true;
        dispose();
    }

    private boolean validateInput() {

        String dialogUnableToSaveTitle = resourceBundle.getString(DIALOG_ERROR_UNABLE_TO_SAVE_TITLE_PROP);

        String dialogUnableToSaveKeyMsg = resourceBundle.getString(DIALOG_ERROR_MSG_UNABLE_TO_SAVE_KEY_PROP);

        if (keyTextField.getText().trim().isEmpty()) {
            DialogDisplayer.showMessageDialog(this, dialogUnableToSaveKeyMsg, dialogUnableToSaveTitle, JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        String dialogUnableToSaveValueMsg = resourceBundle.getString(DIALOG_ERROR_MSG_UNABLE_TO_SAVE_VALUE_PROP);

        if ((!keyTextField.getText().trim().equalsIgnoreCase("-")) && valueTextField.getText().trim().isEmpty()) {
            DialogDisplayer.showMessageDialog(this, dialogUnableToSaveValueMsg, dialogUnableToSaveTitle, JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        return true;
    }

    private void onCancel() {

        dispose();
    }

    public LdifAttribute getReturnItem() {

        return ldifAttribute;
    }


    public boolean isWasOKed() {

        return wasOKed;
    }

    private void enableDisableControls() {

        if (keyTextField.getText().equals("-")) {
            valueTextField.setEnabled(false);
        } else {
            valueTextField.setEnabled(true);
        }
    }

}