package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.SsmApplication;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Dialog to enter host key.
 */
public class HostKeyDialog extends JDialog {

    public enum HostKeyValidationType {
        VALIDATE_PEM_PRIVATE_KEY_FORMAT, VALIDATE_SSH_PUBLIC_KEY_FINGERPRINT_FORMAT
    }

    private JPanel mainPanel;
    private JTextArea hostKeyField;
    private JButton loadFromFileButton;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel hostKeyFieldPanel;

    private boolean confirmed = false;
    private static final ResourceBundle resources = ResourceBundle.getBundle(HostKeyDialog.class.getName());

    public HostKeyDialog(Window owner, String hostKey, HostKeyValidationType validationType) {
        super(owner, getResourceString("sshServerHostKeyLabel"), Dialog.ModalityType.APPLICATION_MODAL);
        initComponents(hostKey, validationType);
    }

    public HostKeyDialog(Frame owner, String hostKey, HostKeyValidationType validationType) {
        super(owner, getResourceString("sshServerHostKeyLabel"), true);
        initComponents(hostKey, validationType);
    }

    public HostKeyDialog(Dialog owner, String hostKey, HostKeyValidationType validationType) {
        super(owner, getResourceString("sshServerHostKeyLabel"), true);
        initComponents(hostKey, validationType);
    }

    private void initComponents(String hostKey, final HostKeyValidationType validationType) {
        okButton.setEnabled(hostKey != null);
        hostKeyField.setLineWrap(true);
        hostKeyField.setText(hostKey == null ? "" : hostKey);
        hostKeyField.setEditable(true);

        String borderTitle = "";
        switch (validationType)
        {
            case VALIDATE_PEM_PRIVATE_KEY_FORMAT:
                borderTitle = getResourceString("privateKeyLabel");
                break;
            case VALIDATE_SSH_PUBLIC_KEY_FINGERPRINT_FORMAT:
                borderTitle = getResourceString("sshPublicKeyFingerprintLabel");
                break;
            default:
                break;
        }
        hostKeyFieldPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), borderTitle));


        hostKeyField.getDocument().addDocumentListener(new RunOnChangeListener() {
            @Override
            public void run() {
                if(hostKeyField.getText().trim().length() > 0) {
                    okButton.setEnabled(true);
                } else {
                    okButton.setEnabled(false);
                }
            }
        });

        loadFromFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readFromFile(validationType);
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (validationType)
                {
                    case VALIDATE_PEM_PRIVATE_KEY_FORMAT:
                        if (SshKeyUtil.getPemPrivateKeyAlgorithm(hostKeyField.getText().trim()) == null) {
                            JOptionPane.showMessageDialog(HostKeyDialog.this, MessageFormat.format(
                                    getResourceString("sshHostKeyFormatError"), "The key must be in PEM private key format."));
                            return;
                        }
                        break;
                    case VALIDATE_SSH_PUBLIC_KEY_FINGERPRINT_FORMAT:
                        Pair<Boolean, String> hostKeyFormatIsValid;
                        if (hostKeyField == null || hostKeyField.getText().equalsIgnoreCase("")){
                            hostKeyFormatIsValid = new Pair<Boolean, String>(false, "Server key cannot be blank, please enter a value or cancel");
                        } else {
                            hostKeyFormatIsValid = SshKeyUtil.validateSshPublicKeyFingerprint(hostKeyField.getText());
                        }
                        if(!hostKeyFormatIsValid.left.booleanValue()){
                            JOptionPane.showMessageDialog(HostKeyDialog.this, MessageFormat.format(
                                    getResourceString("sshHostKeyFormatError"), hostKeyFormatIsValid.right));
                            return;
                        }
                        break;
                    default:
                        break;
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

    private void readFromFile(final HostKeyValidationType validationType) {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                if (HostKeyValidationType.VALIDATE_SSH_PUBLIC_KEY_FINGERPRINT_FORMAT.equals(validationType)) {
                    FileFilter pubFilter = FileChooserUtil.buildFilter(".pub", "(*.pub) SSH public key files");
                    fc.setFileFilter(pubFilter);
                }
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
            hostKeyField.setText(new String(IOUtils.slurpFile(new File(filename))));
        } catch(IOException ioe) {
            JOptionPane.showMessageDialog(this, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getHostKey() {
        return hostKeyField.getText().trim();
    }

     private static String getResourceString(String key){
        final String value = resources.getString(key);
        if(value.endsWith(":")){
            return value.substring(0, value.lastIndexOf(":"));
        }
        return value;
    }
}
