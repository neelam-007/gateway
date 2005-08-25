package com.l7tech.server.config.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 23, 2005
 * Time: 1:08:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class LunaKeystorePanel extends KeystorePanel {
    private JPanel mainPanel;

    private JCheckBox overwriteExisting;
    private JTextField lunaInstallPath;
    private JButton browseLunaPath;
    private JTextField lunaJSPPath;
    private JButton browseLunaJSPPath;

    public LunaKeystorePanel() {
        super();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public boolean validateInput() {
        return true;
    }

    public boolean isOverwriteExisting() {
        return overwriteExisting.isSelected();
    }

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
}
