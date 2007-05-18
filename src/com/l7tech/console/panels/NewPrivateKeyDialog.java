package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog that offers ways of creating a new key pair and associated metadata.
 */
public class NewPrivateKeyDialog extends JDialog {
    private static final String TITLE = "Create Private Key";

    private JPanel rootPanel;
    private JRadioButton rbSelfSigned;
    private JRadioButton rbCsr;
    private JTextField dnField;
    private JButton editDnButton;
    private JComboBox cbKeyType;
    private JTextField aliasField;
    private JButton createButton;
    private JButton cancelButton;
    private JTextField expiryDaysField;

    private boolean confirmed;

    public NewPrivateKeyDialog(Frame owner) throws HeadlessException {
        super(owner, TITLE, true);
        initialize();
    }

    public NewPrivateKeyDialog(Dialog owner) throws HeadlessException {
        super(owner, TITLE, true);
        initialize();
    }

    private void initialize() {
        final Container contentPane = getContentPane();
        contentPane.removeAll();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(rootPanel, BorderLayout.CENTER);

        confirmed = false;

        editDnButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO DN editor
                JOptionPane.showMessageDialog(NewPrivateKeyDialog.this, "DN editor dialog goes here");
            }
        });

        createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createKey();
                
                confirmed = true;
                setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                setVisible(false);
            }
        });
    }

    /**
     * Do the actual work and attempt to create the new key.
     */
    private void createKey() {
        // TODO do the actual work
        JOptionPane.showMessageDialog(NewPrivateKeyDialog.this, "Doing the actual work goes here");
    }

    private TrustedCertAdmin getCertAdmin() {
        return Registry.getDefault().getTrustedCertManager();
    }

    /** @return true if this dialog has been dismissed with the Create button. */
    public boolean isConfirmed() {
        return confirmed;
    }
}
