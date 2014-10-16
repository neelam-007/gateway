package com.l7tech.external.assertions.policybundleexporter.console;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;

import javax.swing.*;
import java.awt.event.*;

/**
 *
 */
public class FolderSelectionDialog extends JDialog {
    private JPanel contentPanel;
    private FolderSelectionPanel folderSelectionPanel;
    private JButton okButton;
    private JButton cancelButton;

    private boolean isConfirmed = false;

    public FolderSelectionDialog(JDialog owner) {
        super(owner, "Select A Folder", true);
        initialize();
    }

    public void setFolder(FolderHeader folder) {
        folderSelectionPanel.setSelectedFolder(folder);
    }

    public FolderHeader getFolder() {
        return folderSelectionPanel.getSelectedFolder();
    }

    public boolean getIsConfirmed() {
        return isConfirmed;
    }

    private void initialize() {
        folderSelectionPanel.populateFolders(Folder.ROOT_FOLDER_ID, false, false);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });
        getRootPane().setDefaultButton(okButton);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        contentPanel.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setContentPane(contentPanel);
        pack();
    }

    private void onOk() {
        String error = folderSelectionPanel.isFolderSelected();

        if (!error.isEmpty()) {
            DialogDisplayer.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE, null);
        } else {
            isConfirmed = true;
            dispose();
        }
    }
}