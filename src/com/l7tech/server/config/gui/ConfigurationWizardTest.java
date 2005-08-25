package com.l7tech.server.config.gui;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 10, 2005
 * Time: 9:37:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigurationWizardTest {
    public static void main(String[] args)
      throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        ConfigurationWizard wizard = ConfigurationWizard.getInstance(new JFrame());

        wizard.setSize(780, 560);
        Utilities.centerOnScreen(wizard);
        wizard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        wizard.setVisible(true);
    }
}
