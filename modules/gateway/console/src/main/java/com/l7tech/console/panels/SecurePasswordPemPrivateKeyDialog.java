package com.l7tech.console.panels;

import com.l7tech.console.SsmApplication;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dialog to enter PEM private key.
 */
public class SecurePasswordPemPrivateKeyDialog extends JDialog {
    private JPanel mainPanel;
    private JTextArea pemPrivateKeyField;
    private JButton loadFromFileButton;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel pemPrivateKeyPanel;

    private boolean confirmed = false;

    public SecurePasswordPemPrivateKeyDialog(Window owner, String title, int maxPasswordLength) {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        setContentPane(mainPanel);
        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.centerOnParentWindow(this);
        pack();
        pemPrivateKeyField.requestFocusInWindow();
        Utilities.setMaxLength(pemPrivateKeyField.getDocument(), maxPasswordLength);

        okButton.setEnabled(false);
        pemPrivateKeyField.setLineWrap(true);
        pemPrivateKeyField.setText("");
        pemPrivateKeyField.setEditable(true);
        pemPrivateKeyPanel.setBorder(BorderFactory.createEtchedBorder());

        pemPrivateKeyField.getDocument().addDocumentListener(new RunOnChangeListener() {
            @Override
            public void run() {
                if(pemPrivateKeyField.getText().trim().length() > 0) {
                    okButton.setEnabled(true);
                } else {
                    okButton.setEnabled(false);
                }
            }
        });

        loadFromFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readFromFile();
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!simplePemPrivateKeyValidation(getPemPrivateKey())) {
                    JOptionPane.showMessageDialog(SecurePasswordPemPrivateKeyDialog.this, "The key must be in PEM private key format.");
                    return;
                }
                confirmed = true;
                dispose();

            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(okButton, cancelButton);

        setContentPane(mainPanel);
        pack();
    }

    private void readFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                doRead(fc);
            }
        });
    }
    
    private void doRead(JFileChooser dlg) {
        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }

        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            pemPrivateKeyField.setText(new String(IOUtils.slurpFile(new File(filename))));
        } catch(IOException ioe) {
            JOptionPane.showMessageDialog(this, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Do simple PEM private key validation.
     * @param pemPrivateKey a private key string in PEM format
     * @return whether PEM private key has BEGIN and END markers
     */
    public static boolean simplePemPrivateKeyValidation(String pemPrivateKey) {
        final Pattern pattern = Pattern.compile("^[ \n\r]*-----BEGIN.*PRIVATE KEY-----[ \n\r]*$", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(pemPrivateKey);
        return matcher.matches();
    }

    public String getPemPrivateKey() {
        return pemPrivateKeyField != null ? pemPrivateKeyField.getText() : null;
    }

    /**
     * Prompt the user for a PEM private key.
     * @param parent
     * @param title
     * @return The password the user typed, or null if the dialog was canceled.
     */
    public static char[] getPemPrivateKey(Window parent, String title, int maxPasswordLength) {
        SecurePasswordPemPrivateKeyDialog dialog = new SecurePasswordPemPrivateKeyDialog(parent, title, maxPasswordLength);
        String word = dialog.runPemPrivateKeyPrompt();
        dialog.dispose();
        return word != null ? word.toCharArray() : null;
    }

    private String runPemPrivateKeyPrompt() {
        pack();
        setResizable(true);
        Utilities.centerOnScreen(this);
        setVisible(true);
        return confirmed ? getPemPrivateKey() : null;
    }
}
