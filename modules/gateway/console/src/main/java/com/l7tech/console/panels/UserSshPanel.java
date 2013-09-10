package com.l7tech.console.panels;

import com.l7tech.console.SsmApplication;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.IOUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Panel for user SSH settings.
 */
public class UserSshPanel extends JPanel {
    private JPanel mainPanel;
    private JScrollPane privateKeyScrollPane;
    private JTextArea publicKeyField;
    private JButton loadPrivateKeyFromFileButton;
    private InternalUser user;
    private boolean canUpdate;
    private boolean isWritable;
    static Logger log = Logger.getLogger(UserSshPanel.class.getName());

    public UserSshPanel(InternalUser whichUser, boolean isWritable, boolean canUpdate) throws FindException {
        this.user = whichUser;
        this.canUpdate = canUpdate;
        this.isWritable = isWritable;

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        initComponents();
    }

    private void initComponents() {
        publicKeyField.setEnabled(isWritable && canUpdate);
        publicKeyField.setText(user.getProperty(InternalUser.PROPERTIES_KEY_SSH_USER_PUBLIC_KEY));

        loadPrivateKeyFromFileButton.setEnabled(isWritable && canUpdate);
        loadPrivateKeyFromFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readFromFile();
            }
        });
    }

    private void readFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                FileFilter pubFilter = FileChooserUtil.buildFilter(".pub", "(*.pub) SSH public key files");
                fc.setFileFilter(pubFilter);
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
            publicKeyField.setText(new String(IOUtils.slurpFile(new File(filename))));
        } catch(IOException ioe) {
            JOptionPane.showMessageDialog(this, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getInternalUserPublicKey() {
        return publicKeyField != null ? publicKeyField.getText() : null;
    }
}
