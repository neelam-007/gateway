package com.l7tech.console;

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import com.jgoodies.plaf.plastic.Plastic3DLookAndFeel;
import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.plastic.PlasticXPLookAndFeel;
import com.jgoodies.plaf.plastic.theme.SkyBluerTahoma;
import com.jgoodies.plaf.windows.ExtWindowsLookAndFeel;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.console.util.Preferences;
import net.jini.security.policy.DynamicPolicyProvider;
import net.jini.security.policy.PolicyInitializationException;

import javax.security.auth.Subject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.rmi.RMISecurityManager;
import java.security.AccessController;
import java.security.Permission;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the SSG console Main entry point.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Main {
    Logger log = Logger.getLogger(Main.class.getName());
    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();

    // splash screen
    private SplashScreen mainSplashScreen = null;

    /**
     * run the application
     *
     * @param args
     */
    public void run(String[] args) {
        try {
            setInitialEnvironment();
            JdkLoggerConfigurator.configure("com.l7tech.console", "com/l7tech/console/resources/logging.properties");
            Logger log = Logger.getLogger(getClass().getName());

            ensureSecurityManager();
            installEventQueue();

            Policy.setPolicy(new DynamicPolicyProvider());
            final MainWindow main;
            initializeUIPreferences();
            /* invoke the splash screen */
            showSplashScreen();

            /* load user preferences and merge them with system props */
            Preferences prefs = Preferences.getPreferences();
            prefs.updateFromProperties(System.getProperties(), false);
            /* so it is visible in help/about */
            prefs.updateSystemProperties();
            try {
                copyResources(new String[]{"com/l7tech/console/resources/logger.dtd"}, prefs.getHomePath());
            } catch (IOException e) {
                log.log(Level.WARNING, "error on copying resources", e);
            }

            main = new MainWindow();
            // Window listener
            main.addWindowListener(new WindowAdapter() {
                /**
                 * Invoked when a window has been opened.
                 */
                public void windowOpened(WindowEvent e) {
                    if (mainSplashScreen != null) {
                        mainSplashScreen.dispose();
                        mainSplashScreen = null;
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            main.activateLogonDialog();
                        }
                    });
                }

                public void windowClosed(WindowEvent e) {
                    saveWindowPosition(main);
                    System.exit(0);
                }
            });
            main.setVisible(true);
            main.toFront();
        } catch (HeadlessException e) {
            log.log(Level.SEVERE, "SSM Error", e);
        } catch (PolicyInitializationException e) {
            log.log(Level.SEVERE, "SSM Error", e);
        }
    }

    /**
     * the "Splash Screen"
     */
    void showSplashScreen() {
        /* Create the splash screen */
        mainSplashScreen = new SplashScreen();
        mainSplashScreen.pack();
        Utilities.centerOnScreen(mainSplashScreen);
        mainSplashScreen.setVisible(true);
    }

    /**
     * install custom event queue as the default one does not pass the
     * security info (Subject, AccessController.getJndiContext()...)
     */
    void installEventQueue() {
        EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();

        // our event queue
        eq.push(new EventQueue() {
            final Subject current =
              Subject.getSubject(AccessController.getContext());
            EventPrivilegedAction privilegedAction = new EventPrivilegedAction();

            protected void dispatchEvent(final AWTEvent event) {
                privilegedAction.setEvent(event);
                Subject.doAs(current, privilegedAction);
            }

            private void dispatchEventToSuper(final AWTEvent event) {
                super.dispatchEvent(event);
            }

            /**
             * the event holder, mainly exists so
             */
            class EventPrivilegedAction implements PrivilegedAction {
                AWTEvent event;

                void setEvent(AWTEvent event) {
                    this.event = event;
                }

                /**
                 * This method will be called by <code>AccessController.doPrivileged</code>
                 * after enabling privileges.
                 *
                 * @see PrivilegedAction for more detials
                 */
                public Object run() {
                    dispatchEventToSuper(event);
                    return null;
                }
            }
        });
    }

    /**
     * Save the window position preference.  Called when the app is closed.
     */
    private void saveWindowPosition(Window w) {
        Point curWindowLocation = w.getLocation();
        Dimension curWindowSize = w.getSize();
        try {
            Preferences prefs = Preferences.getPreferences();
            prefs.setLastWindowLocation(curWindowLocation);
            prefs.setLastWindowSize(curWindowSize);
            prefs.store();
        } catch (IOException e) {
            log.log(Level.WARNING, "unable to save window position prefs: ", e);
        }
    }


    private void installLookAndFeel() {
        // register L&F
        new KunststoffLookAndFeel();
        LookAndFeel addlf = new ExtWindowsLookAndFeel();
        UIManager.installLookAndFeel(addlf.getName(), addlf.getClass().getName());
        addlf = new PlasticLookAndFeel();
        UIManager.installLookAndFeel(addlf.getName(), addlf.getClass().getName());
        PlasticLookAndFeel.setMyCurrentTheme(new SkyBluerTahoma());
        addlf = new Plastic3DLookAndFeel();
        UIManager.installLookAndFeel(addlf.getName(), addlf.getClass().getName());
        addlf = new PlasticXPLookAndFeel();
        UIManager.installLookAndFeel(addlf.getName(), addlf.getClass().getName());

        // ClearLookManager.setMode(ClearLookMode.DEBUG);

        String lfName = null;
        Preferences prefs = Preferences.getPreferences();
        lfName = prefs.getString(Preferences.LOOK_AND_FEEL);
        LookAndFeel lf = null;
        if (lfName == null) {
            lf = new Plastic3DLookAndFeel();
        } else {
            try {
                lf = (LookAndFeel)Class.forName(lfName).newInstance();
            } catch (Exception e) {
                log.log(Level.WARNING, "Unable to instantiate L&F from properties. Will attempt system L&F", e);
                try {
                    lf = (LookAndFeel)Class.forName(UIManager.getSystemLookAndFeelClassName()).newInstance();
                } catch (Exception e1) {
                    log.log(Level.WARNING, "Unable to instantiate L&F", e1);
                }
            }
        }
        try {
            UIManager.setLookAndFeel(lf);
        } catch (UnsupportedLookAndFeelException e) {
            log.log(Level.WARNING, "Unable to set L&F", e);
        }
    }

    /**
     * Tweak any global preferences.
     */
    private void initializeUIPreferences() {
        UIManager.put("ClassLoader", cl);
        installLookAndFeel();
    }

    /**
     * Utility routine that sets a security manager if one isn't already
     * present.
     */
    private void ensureSecurityManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager() {
                public void checkPermission(Permission perm) {
                }

                public void checkPermission(Permission perm, Object context) {
                }

            });
        }
    }

    private void setInitialEnvironment() {
        // AWT event dispatching thread error handler
        System.setProperty("sun.awt.exception.handler", "com.l7tech.console.logging.AwtErrorHandler");
        //System.setProperty("java.security.policy", "etc/jini/policy.all");
        System.setProperty("java.protocol.handler.pkgs", "com.l7tech.remote.jini");

        // where locator looks for implementaitons
        System.setProperty("com.l7tech.common.locator.properties", "/com/l7tech/console/resources/services.properties");
        // Build information
        System.setProperty("com.l7tech.buildstring", BuildInfo.getBuildString());
        System.setProperty("com.l7tech.builddate", BuildInfo.getBuildDate() + BuildInfo.getBuildTime());

        // apache logging layer to use the jdk logger
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");

    }

    /**
     * copy resources from jar to the directory.
     * storage
     */
    private void copyResources(String res[], String directory) throws IOException {
        InputStream in = null;
        try {
            ClassLoader cl = getClass().getClassLoader();
            for (int i = 0; i < res.length; i++) {
                in = cl.getResourceAsStream(res[i]);
                if (in == null) {
                    System.err.println("Couldn't load " + res[i]);
                    continue;
                }
                URL url = cl.getResource(res[i]);
                String file = directory + File.separator + new File(url.getFile()).getName();
                final InputStream input = in;
                FileUtils.saveFileSafely(file, new FileUtils.Saver() {
                    public void doSave(FileOutputStream fos) throws IOException {
                        byte[] bytearray = new byte[512];
                        int len = 0;
                        try {
                            while ((len = input.read(bytearray)) != -1) {
                                fos.write(bytearray, 0, len);
                            }
                        } finally {
                            if (fos != null) fos.close();
                        }
                    }
                });
                in.close();
                in = null;
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Starts the application. The applicaiton is started
     * as a <code>PrivilegedAction</code> to provide the security
     * context.
     *
     * @param args an array of command-line arguments
     */
    public static void main(final String[] args) {
        Subject.doAsPrivileged(new Subject(), new PrivilegedAction() {
            public Object run() {
                new Main().run(args);
                return null;
            }
        }, null);
    }
}
