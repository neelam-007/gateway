/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license.gui;

import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standalone application that makes license files.
 */
public class LicenseGeneratorMain {
    private static final Logger logger = Logger.getLogger(LicenseGeneratorMain.class.getName());
    public static final String PROPERTY_PROPERTIES_PATH = "licenseGenerator.properties";
    public static final String DEFAULT_PROPERTIES_PATH = "licenseGenerator.properties";

    private static File propertiesFile = new File(System.getProperty(PROPERTY_PROPERTIES_PATH, DEFAULT_PROPERTIES_PATH));
    private static Properties properties = new Properties();

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

        Properties props = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propertiesFile);
            props.load(fis);
            logger.info("Loaded properties from " + propertiesFile.getPath());
            Collection keys = props.keySet();
            for (Iterator i = keys.iterator(); i.hasNext();) {
                Object key = i.next();
                if (key instanceof String)
                    System.setProperty((String)key, props.getProperty((String)key));
            }
            properties = props;
        } catch (IOException e) {
            // Oh well, we'll do without
        } finally {
            if (fis != null) //noinspection EmptyCatchBlock
                try { fis.close(); } catch (IOException e) {}
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

    /** @return the properties we loaded on startup, or empty properties if we didn't load any. */
    public static Properties getProperties() {
        return properties;
    }

    /** Save the current properties into the properties file. */
    public static void saveProperties() throws IOException {
        FileOutputStream fos = null;
        try {
            logger.info("Saving properties to " + propertiesFile.getPath());
            fos = new FileOutputStream(propertiesFile);
            properties.store(fos, "License Generator Properties.  This file gets rewritten when the user changes properties in the License Generator GUI.");
        } catch (IOException e) {
            throw (IOException)new IOException("Unable to save properties to " + propertiesFile.getPath() +
                                  ".\nError was: " + ExceptionUtils.getMessage(e)).initCause(e);
        } finally {
            if (fos != null) //noinspection EmptyCatchBlock
                try { fos.close(); } catch (IOException e) {}
        }
    }
}
