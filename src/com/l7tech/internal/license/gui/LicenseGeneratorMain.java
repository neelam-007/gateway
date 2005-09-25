/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license.gui;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Standalone application that makes license files.
 */
public class LicenseGeneratorMain {
    private static final Logger logger = Logger.getLogger(LicenseGeneratorMain.class.getName());

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Unable to set system look and feel", e);
        } catch (InstantiationException e) {
            logger.log(Level.WARNING, "Unable to set system look and feel", e);
        } catch (IllegalAccessException e) {
            logger.log(Level.WARNING, "Unable to set system look and feel", e);
        } catch (UnsupportedLookAndFeelException e) {
            logger.log(Level.WARNING, "Unable to set system look and feel", e);
        }
        LicenseGeneratorTopWindow window = new LicenseGeneratorTopWindow();
        window.addWindowListener( new WindowAdapter() {
            public void windowClosing( final WindowEvent e ) {
                System.exit(0);
            }
        } );
        Utilities.centerOnScreen(window);
        window.setVisible(true);
        // Startup done
    }
}
