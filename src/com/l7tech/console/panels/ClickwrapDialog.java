package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.WrappingLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Click wrap dialog for SSM.
 */
public class ClickwrapDialog extends JDialog {
    private JPanel rootPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JScrollPane licenseScrollPane;
    private JPanel licensePanel;

    private boolean confirmed = false;

    public ClickwrapDialog(Frame owner, String agreementText) throws HeadlessException {
        super(owner);
        initialize(agreementText);
    }

    public ClickwrapDialog(Dialog owner, String agreementText) throws HeadlessException {
        super(owner);
        initialize(agreementText);
    }

    private void initialize(String agreementText) {
        setContentPane(rootPanel);
        setModal(true);
        setTitle("License Agreement");

        WrappingLabel licenseText = new WrappingLabel(agreementText);
        licenseText.setContextMenuAutoSelectAll(false);
        licenseText.setContextMenuEnabled(true);
        licensePanel.add(licenseText, BorderLayout.CENTER);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                ClickwrapDialog.this.dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ClickwrapDialog.this.dispose();
            }
        });
        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        //Utilities.attachDefaultContextMenu(licenseText);
    }

    /** @return true only if the dialog has been displayed and dismissed via the "I Agree" button. */
    public boolean isConfirmed() {
        return confirmed;
    }
}
