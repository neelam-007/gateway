/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import com.l7tech.common.util.JavaVersionChecker;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.swing.*;
import javax.swing.plaf.metal.MetalTheme;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mike
 */
public abstract class SsmApplication extends ApplicationObjectSupport {
    private static final Logger logger = Logger.getLogger(SsmApplication.class.getName());
    private static final String KUNSTSTOFF_CLASSNAME = "com.incors.plaf.kunststoff.KunststoffLookAndFeel";
    private static final String KUNSTSTOFF_THEME_CLASSNAME = "com.incors.plaf.kunststoff.themes.KunststoffDesktopTheme";
    private static final boolean SUPPRESS_AUTO_LNF = Boolean.valueOf("com.l7tech.console.SuppressAutoLookAndFeel");

    private String resourcePath;
    protected MainWindow mainWindow;

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    abstract void run();

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    abstract boolean isApplet();

    /** @return true if a custom look and feel should be honored.  False to do normal automatic look-and-feel selection. */
    public static boolean isSuppressAutoLookAndFeel() {
        return SUPPRESS_AUTO_LNF;
    }

    private static interface LnfSetter {
        void setLnf() throws Exception;
    }

    /**
     * Try to set the Kunststoff look and feel.
     */
    private static LnfSetter kunststoffLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            final Class kunststoffClass = Class.forName( KUNSTSTOFF_CLASSNAME );
            final Object kunststoffLnF = kunststoffClass.newInstance();
            try {
                final Class themeClass = Class.forName( KUNSTSTOFF_THEME_CLASSNAME );
                final Object theme = themeClass.newInstance();
                kunststoffClass.getMethod("setCurrentTheme", MetalTheme.class).invoke(kunststoffLnF, theme);
            } catch ( Exception e ) {
                // eat it, themes not make one great
            }
            UIManager.setLookAndFeel( (LookAndFeel) kunststoffLnF );
        }
    };

    /**
     * Try to set the Windows look and feel.
     */
    private static LnfSetter windowsLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            boolean wasSet = false;
            UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
            for (UIManager.LookAndFeelInfo feel : feels) {
                if (feel.getName().indexOf("Windows") >= 0) {
                    UIManager.setLookAndFeel(feel.getClassName());
                    wasSet = true;
                    break;
                }
            }
            if (!wasSet)
                throw new Exception("No XP LNF");
        }
    };

    /**
     * Otherwise, system look and feel.
     */
    private static LnfSetter systemLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        }
    };

    /**
     * Automatically pick the best look and feel and enable it.
     * This method currently uses Windows, then kunststoff, then system LnF on java 1.4.2 or higher;
     * or kunststoff, windows, then system on earlier javas.
     */
    protected void setAutoLookAndFeel() {
        boolean haveXpLnf = JavaVersionChecker.isJavaVersionAtLeast( new int[]{1, 4, 2} );
        LnfSetter[] order = haveXpLnf ? new LnfSetter[]{windowsLnfSetter, kunststoffLnfSetter, systemLnfSetter}
                            : new LnfSetter[]{kunststoffLnfSetter, windowsLnfSetter, systemLnfSetter};
        for (LnfSetter anOrder : order) {
            try {
                anOrder.setLnf();
            } catch (Exception e) {
                continue;
            }
            break;
        }

        try {
            // incors.org Kunststoff faq says we need the following line if we ever want to use Java Web Start:
            UIManager.getLookAndFeelDefaults().put( "ClassLoader", getClass().getClassLoader() );
        } catch ( Exception e ) {
            logger.log(Level.WARNING, "Unable to update look-and-feel classloader", e);
        }
    }
}
