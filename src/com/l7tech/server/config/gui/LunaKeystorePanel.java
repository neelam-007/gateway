package com.l7tech.server.config.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 23, 2005
 * Time: 1:08:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class LunaKeystorePanel extends KeystorePanel {
    private JPanel mainPanel;

    //private JCheckBox overwriteExisting;
    private JTextField lunaInstallPath;
    private JButton browseLunaPath;
    private JTextField lunaJSPPath;
    private JButton browseLunaJSPPath;

    private String chosenLunaInstallPath;
    private String chosenLunaJspPath;

    public LunaKeystorePanel() {
        super();
        init();
    }

    private void init() {
        browseLunaPath.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseLunaInstallPath();
            }
        });

        browseLunaJSPPath.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseLunaJspPath();
            }
        });
        
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public boolean validateInput() {
        return true;
    }

//    public boolean isOverwriteExisting() {
//        return overwriteExisting.isSelected();
//    }

    public void setDefaultLunaInstallPath(String path) {
        lunaInstallPath.setText(path);
    }

    public void setDefaultLunaJSPPath(String path) {
        lunaJSPPath.setText(path);
    }

    public String getLunaInstallPath() {
        return lunaInstallPath.getText();
    }

    public String getLunaJSPPath() {
        return lunaJSPPath.getText();
    }

    private void chooseLunaJspPath() {
        String origPath = lunaJSPPath.getText();
        JFileChooser fc = new JFileChooser(origPath);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int retval = fc.showOpenDialog(this);
        File selectedFile = null;
        if (retval == JFileChooser.CANCEL_OPTION) {
            lunaJSPPath.setText(origPath);
        } else if (retval == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
        }
        if (selectedFile != null) {
            lunaJSPPath.setText(selectedFile.getAbsolutePath());
        }
    }

    private void chooseLunaInstallPath() {
        String origPath = lunaInstallPath.getText();
        JFileChooser fc = new JFileChooser(origPath);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int retval = fc.showOpenDialog(this);
        File selectedFile = null;
        if (retval == JFileChooser.CANCEL_OPTION) {
            lunaInstallPath.setText(origPath);
        } else if (retval == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
        }
        if (selectedFile != null) {
            lunaInstallPath.setText(selectedFile.getAbsolutePath());
        }
    }
}
