package com.l7tech.console;

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import com.jgoodies.plaf.plastic.Plastic3DLookAndFeel;
import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.plastic.PlasticXPLookAndFeel;
import com.jgoodies.plaf.plastic.theme.SkyBluerTahoma;
import com.jgoodies.plaf.windows.ExtWindowsLookAndFeel;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.console.util.HeavySsmPreferences;
import com.l7tech.console.util.SplashScreen;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.security.ManagerTrustProvider;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Security;

/**
 * This class is the SSG console Main entry point.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Main {
    Logger log = Logger.getLogger(Main.class.getName());
    /* this class classloader */
    private final ClassLoader cl = Main.class.getClassLoader();

    // splash screen

    /**
     * run the application
     *
     * @param args
     */
    public void run(String[] args) {
        try {
            setInitialEnvironment();
            final SplashScreen screen = new SplashScreen("/com/l7tech/console/resources/splash-screen.gif");
            try {
                screen.splash();
                JdkLoggerConfigurator.configure("com.l7tech.console", "com/l7tech/console/resources/logging.properties");
                Logger log = Logger.getLogger(getClass().getName());

                configureSecurity();

                initializeUIPreferences();
                if (!SyspropUtil.getBoolean("com.l7tech.console.useSheets"))
                    DialogDisplayer.setForceNative(true);

                /* load user preferences and merge them with system props */
                HeavySsmPreferences prefs = HeavySsmPreferences.getPreferences();
                prefs.updateFromProperties(System.getProperties(), false);
                /* so it is visible in help/about */
                prefs.updateSystemProperties();
                // ensure trust store exists
                prefs.initializeSsgCertStorage();
                try {
                    copyResources(new String[]{"com/l7tech/console/resources/logger.dtd"}, prefs.getHomePath());
                } catch (IOException e) {
                    log.log(Level.WARNING, "error on copying resources", e);
                }
                ApplicationContext ctx = createApplicationContext();
                SsmApplication app = (SsmApplication)ctx.getBean("ssmApplication");
                app.run();
            } finally {
                screen.dispose();
            }
        } catch (final Exception e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    throw new RuntimeException("Startup Error", e);
                }
            });
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
        if (!SsmApplication.isSuppressAutoLookAndFeel())
            return; // Will choose auto look-and-feel

        HeavySsmPreferences prefs = HeavySsmPreferences.getPreferences();
        String lfName = prefs.getString(SsmPreferences.LOOK_AND_FEEL);
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
     * Utility routine that removes the security manager if present.
     *
     * This ensures that we disable downloading of classes using the default
     * RMI mechanism.
     *
     * Also adds the security provider for Gateway SSL trust
     */
    private void configureSecurity() {
        if (System.getSecurityManager() != null) {
            System.setSecurityManager(null);
        }

        // Add security provider for HTTP remoting / RMI
        Security.addProvider(new ManagerTrustProvider());
    }

    private void setInitialEnvironment() {
        // AWT event dispatching thread error handler
        System.setProperty("sun.awt.exception.handler", com.l7tech.console.logging.AwtErrorHandler.class.getName());

        // Build information
        System.setProperty("com.l7tech.buildstring", BuildInfo.getBuildString());
        System.setProperty("com.l7tech.builddate", BuildInfo.getBuildDate() + BuildInfo.getBuildTime());

        // apache logging layer to use the jdk logger
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");

        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");

        // Set property for use by Spring HTTP remoting (its not RMI but uses this to load classes)
        System.setProperty("java.rmi.server.RMIClassLoaderSpi", "com.l7tech.console.util.CustomAssertionRMIClassLoaderSpi");

        // Set trust manager algorithm for HTTP remoting / RMI
        System.setProperty("com.l7tech.console.trustMananagerFactoryAlgorithm", "L7TA");

        // Set props for StAX
        System.setProperty("javax.xml.stream.XMLEventFactory", "com.sun.xml.stream.events.ZephyrEventFactory");
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.sun.xml.stream.ZephyrParserFactory");
        System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.stream.ZephyrWriterFactory");
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
                FileUtils.save(in, new File(file));
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

    private static ApplicationContext createApplicationContext() {
        String ctxName = SyspropUtil.getProperty("ssm.application.context");
        if (ctxName == null) {
            ctxName = "com/l7tech/console/resources/beans-context.xml";
        }
        String ctxHeavy = "com/l7tech/console/resources/beans-application.xml";
        return new ClassPathXmlApplicationContext(new String[]{ctxHeavy, ctxName});
    }


    /**
     * Starts the application. The applicaiton is started
     * as a <code>PrivilegedAction</code> to provide the security
     * context.
     *
     * @param args an array of command-line arguments
     */
    public static void main(final String[] args) {
        new Main().run(args);
    }
}
