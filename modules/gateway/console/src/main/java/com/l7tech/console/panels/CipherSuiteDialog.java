package com.l7tech.console.panels;

import com.l7tech.console.util.CipherSuiteGuiUtil;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel that can be used to edit the enabled cipher suites.
 */
public class CipherSuiteDialog extends JDialog {
    private JPanel mainPanel;
    private JList cipherSuiteList;
    private JButton moveUpButton;
    private JButton defaultCipherListButton;
    private JButton moveDownButton;
    private JButton cancelButton;
    private JButton okButton;
    private JButton selectNoneButton;

    private final CipherSuiteListModel cipherSuiteListModel;
    private boolean confirmed = false;

    public CipherSuiteDialog(Window owner, String title, ModalityType modalityType, String cipherSuites, boolean readOnly) {
        super(owner, title, modalityType);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel);
        cipherSuiteListModel = CipherSuiteGuiUtil.createCipherSuiteListModel(cipherSuiteList, true, defaultCipherListButton, selectNoneButton, null, moveUpButton, moveDownButton);
        cipherSuiteListModel.setCipherListString(cipherSuites);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!cipherSuiteListModel.isAnyEntryChecked()) {
                    DialogDisplayer.showMessageDialog(cipherSuiteList, "At least one cipher suite must be enabled.", "Error", JOptionPane.ERROR_MESSAGE, null);
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

        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        Utilities.equalizeButtonSizes(new JButton[] { moveUpButton, moveDownButton, defaultCipherListButton });

        okButton.setEnabled(!readOnly);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getCipherListString() {
        return cipherSuiteListModel.asCipherListStringOrNullIfDefault();
    }

    /**
     * Show a dialog that offers to (re)configure a cipher suite list string.
     *
     * @param owner the owner dialog.  required.
     * @param title the title, or null to use a default.
     * @param modalityType the modality type, or null to use the system default for modal dialogs.
     * @param readOnly if true, the OK button will never be enabled.
     * @param cipherSuiteList the initial cipher suite list to show, or null to show the default list.
     * @param confirmCallback a confirmation callback that will be invoked only if the dialog is confirmed.
     *                        Its argument will be the new cipher suite list, with a value of null meaning the new list matches the system default list.
     */
    public static void show(Window owner, String title, ModalityType modalityType, boolean readOnly, String cipherSuiteList, final Functions.UnaryVoid<String> confirmCallback) {
        if (title == null)
            title = "Cipher Suite Configuration";
        if (modalityType == null)
            modalityType = JDialog.DEFAULT_MODALITY_TYPE;
        final CipherSuiteDialog dlg = new CipherSuiteDialog(owner, title, modalityType, cipherSuiteList, readOnly);
        dlg.setModal(true);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed() && confirmCallback != null) {
                    confirmCallback.call(dlg.getCipherListString());
                }
            }
        });
    }
}
