package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * This is the generate CSR dialog. It allows a user to enter a DN and select a hash function for the CSR
 */
public class GenerateCSRDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField dnTextField;
    private JComboBox<SigHash> signatureHashComboBox;
    private boolean okD = false;
    private String selectedHash;
    private String csrSubjectDN;

    public GenerateCSRDialog(Dialog owner, String defaultDN) {
        super(owner, "Generate CSR", true);

        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        final SigHash defaultSignatureHash;
        Collection<SigHash> hashes = new ArrayList<>(Arrays.asList(
                defaultSignatureHash = new SigHash("Auto", null),
                new SigHash("SHA-1", "SHA1"),
                new SigHash("SHA-256", "SHA256"),
                new SigHash("SHA-384", "SHA384"),
                new SigHash("SHA-512", "SHA512")
        ));
        signatureHashComboBox.setModel(new DefaultComboBoxModel<>(hashes.toArray(new SigHash[hashes.size()])));
        signatureHashComboBox.setSelectedItem(defaultSignatureHash);

        dnTextField.setText(defaultDN);
        dnTextField.setCaretPosition(0);
    }

    private void onOK() {
        final String dnres = dnTextField.getText();
        //checks if the subject dn is valid.
        try {
            new X500Principal(dnres);
        } catch (IllegalArgumentException e) {
            DialogDisplayer.showMessageDialog(this, dnres + " is not a valid DN",
                    "Invalid Subject", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        selectedHash = ((SigHash) signatureHashComboBox.getSelectedItem()).algorithm;
        csrSubjectDN = dnres;
        okD = true;

        dispose();
    }

    private void onCancel() {
        okD = false;
        dispose();
    }

    /**
     * Returned true if the operation was ok'd.
     *
     * @return true if the operation was ok'd.
     */
    public boolean isOkD() {
        return okD;
    }

    /**
     * Returns the selected signature hash algorithm to use
     *
     * @return The signature hash to use
     */
    public String getSelectedHash() {
        return selectedHash;
    }

    /**
     * Returns the subject DN
     * @return The selected subject DN
     */
    public String getCsrSubjectDN() {
        return csrSubjectDN;
    }

    private static class SigHash {
        private final String label;
        private final String algorithm;

        private SigHash(String label, String algorithm) {
            this.label = label;
            this.algorithm = algorithm;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
