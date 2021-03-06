package com.l7tech.console;

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import com.jgoodies.plaf.plastic.Plastic3DLookAndFeel;
import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.plastic.PlasticXPLookAndFeel;
import com.jgoodies.plaf.plastic.theme.SkyBluerTahoma;
import com.jgoodies.plaf.windows.ExtWindowsLookAndFeel;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.security.ManagerTrustProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SplashScreen;
import com.l7tech.console.util.SsmPreferences;

import com.l7tech.console.security.PolicyManagerBuildInfo;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.FileUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.SyspropUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.jnlp.DownloadService;
import javax.jnlp.DownloadService2;
import javax.jnlp.ServiceManager;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Security;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.l7tech.console.security.PolicyManagerBuildInfo;
import javax.jnlp.DownloadService2.ResourceSpec;

/**
 * This class is the SSG console Main entry point.
 *
 * @author Emil Marceta
 */
public class Main {
    Logger log = Logger.getLogger(Main.class.getName());
    /* this class classloader */
    private final ClassLoader cl = Main.class.getClassLoader();

    // splash screen

    /**
     * run the application
     */
    public void run() {
        try {
            if (isWebStart()) {
                //Delete the old versions of webstart jar from cache, otherwise it contains multiple versions of same jar file
                deleteOldVersionsOfJar();
            }
            setInitialEnvironment();
            final SplashScreen screen = new SplashScreen("/com/l7tech/console/resources/CA_Policy_Manager_Splash.jpg");
            try {
                screen.splash();
                JdkLoggerConfigurator.configure("com.l7tech.console", "com/l7tech/console/resources/logging.properties");
                // create logger after logging is configured
                Logger.getLogger( Main.class.getName() ).info("Starting " + PolicyManagerBuildInfo.getInstance().getLongBuildString());

                configureSecurity();

                if ( !SyspropUtil.getBoolean( "com.l7tech.console.useSheets", false ) )
                    DialogDisplayer.setForceNative(true);

                ApplicationContext ctx = createApplicationContext();

                SsmPreferences prefs = ctx.getBean("preferences", SsmPreferences.class);
                try {
                    copyResources(new String[]{"com/l7tech/console/resources/logger.dtd"}, prefs.getHomePath());
                } catch (IOException e) {
                    log.log(Level.WARNING, "error on copying resources", e);
                }
                initializeUIPreferences(prefs);
                ErrorManager.installUncaughtExceptionHandler();

                SsmApplication app = ctx.getBean("ssmApplication", SsmApplication.class);
                app.run();
            } finally {
                screen.dispose();
            }
        } catch (final Exception e) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Startup Error" );
        }
    }

    /*
        Deletes the old versions of webstart jar from the java cache
     */
    private void deleteOldVersionsOfJar() {
        try {
            DownloadService2 service2 = (DownloadService2)
                    ServiceManager.lookup("javax.jnlp.DownloadService2");
            DownloadService service = (DownloadService) ServiceManager.lookup("javax.jnlp.DownloadService");
            ResourceSpec spec = new ResourceSpec(".*ssg/webstart.*", ".*", DownloadService2.JAR);
            ResourceSpec[] results = service2.getCachedResources(spec);

            Hashtable<String,Integer> duplicateEntry = new Hashtable();
            int index = 0;
            for (ResourceSpec result : results) {
                String strUrl = result.getUrl();
                Integer returnedindex = duplicateEntry.get(strUrl);

                if (returnedindex == null) {
                    duplicateEntry.put(strUrl, new Integer(index));
                }
                else {
                    String strVersion = results[returnedindex].getVersion();
                    String strcurrentIndexversion = result.getVersion();

                    if (strVersion == null && strcurrentIndexversion == null) {
                        break;
                    }
                    else {
                        int firstDotIndex = strVersion.indexOf(".");
                        int majorversion = Integer.parseInt(strVersion.substring(0, firstDotIndex));
                        int currentfirstDotIndex = strcurrentIndexversion.indexOf(".");
                        int majorcurrentIndexversion = Integer.parseInt(strcurrentIndexversion.substring(0,currentfirstDotIndex));

                        if (majorcurrentIndexversion > majorversion) {
                            duplicateEntry.put(strUrl, new Integer(index));
                            service.removeResource(new URL(strUrl), strVersion);
                        }
                        else if (majorcurrentIndexversion == majorversion) {
                            float minorversion = Float.parseFloat(strVersion.substring(firstDotIndex + 1,strVersion.length()));
                            float minorcurrentIndexversion = Float.parseFloat(strcurrentIndexversion.substring(currentfirstDotIndex + 1,strcurrentIndexversion.length()));

                            if (minorcurrentIndexversion > minorversion) {
                                duplicateEntry.put(strUrl, new Integer(index));
                                service.removeResource(new URL(strUrl), strVersion);
                            }
                            else {
                                service.removeResource(new URL(strUrl), strcurrentIndexversion);
                            }
                        }
                        else {
                            service.removeResource(new URL(strUrl), strcurrentIndexversion);
                        }
                    }
                }
                index++;
            }


        }
        catch (Exception e)
        {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error while trying to remove earlier versions of jar from Java cache" );
        }
    }
    private void installLookAndFeel(final SsmPreferences prefs) {
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
    private void initializeUIPreferences(SsmPreferences prefs) {
        UIManager.put("ClassLoader", cl);
        installLookAndFeel(prefs);
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
        System.setProperty("com.l7tech.buildstring", PolicyManagerBuildInfo.getInstance().getBuildString());
        System.setProperty("com.l7tech.builddate", PolicyManagerBuildInfo.getInstance().getBuildDate() + PolicyManagerBuildInfo.getInstance().getBuildTime());

        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");

        // Set property for use by Spring HTTP remoting (its not RMI but uses this to load classes)
        System.setProperty("java.rmi.server.RMIClassLoaderSpi", "com.l7tech.console.util.CustomAssertionRMIClassLoaderSpi");

        // Set trust manager algorithm for HTTP remoting / RMI
        System.setProperty("com.l7tech.console.trustMananagerFactoryAlgorithm", "L7TA");

        // Disable ClassTailor optimization for JAXB (SSG-4654)
        if ( null == System.getProperty( "com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize" ) ) {
            System.setProperty( "com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize", "true" );
        }
    }

    /**
     * copy resources from jar to the directory.
     * storage
     */
    private void copyResources(String res[], String directory) throws IOException {
        InputStream in = null;
        try {
            ClassLoader cl = getClass().getClassLoader();
            for( String re : res ) {
                in = cl.getResourceAsStream( re );
                if( in == null ) {
                    System.err.println( "Couldn't load " + re );
                    continue;
                }
                URL url = cl.getResource( re );
                String file = directory + File.separator + new File( url.getFile() ).getName();
                FileUtils.save( in, new File( file ) );
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
        String ctxHeavy = null;

        if (isWebStart()) {
            ctxHeavy = "com/l7tech/console/resources/beans-webstart.xml";
        }
        else {
            ctxHeavy = "com/l7tech/console/resources/beans-application.xml";
        }
        ApplicationContext context = new ClassPathXmlApplicationContext(new String[]{ctxHeavy, ctxName});

        Registry.setDefault( context.getBean("registry", Registry.class) );

        return context;
    }


    private static boolean isWebStart() {

        return System.getProperty("javawebstart.version", null) != null;
    }
    /**
     * Starts the application. The applicaiton is started
     * as a <code>PrivilegedAction</code> to provide the security
     * context.
     *
     * @param args an array of command-line arguments
     */
    public static void main(final String[] args) {
        new Main().run();
    }
}
