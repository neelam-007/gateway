package com.l7tech.server.config.packageupdater;

import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.BaseConfigurationCommand;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.jar.Attributes;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 4:16:10 PM
 */
public class PackageUpdateConfigCommand extends BaseConfigurationCommand {
    private static final Logger logger = Logger.getLogger(PackageUpdateConfigCommand.class.getName());

    PackageUpdateConfigBean packageBean;

    protected PackageUpdateConfigCommand(ConfigurationBean bean) {
        super(bean);
        packageBean = (PackageUpdateConfigBean) bean;
    }

    public boolean execute() {
        boolean success = false;
        PackageUpdateConfigBean.UpdatePackageInfo info = packageBean.getPackageInfo();
//        success = loadAndExecuteJar(info);
        success = executeJar(info);
        if (success)
            FileUtils.deleteDir(new File(info.getExpandedLocation()));

        return success;
    }

    private boolean executeJar(PackageUpdateConfigBean.UpdatePackageInfo info) {
        boolean success = false;
        if (info != null) {
            String expandedLocation = info.getExpandedLocation();
            File f = new File(expandedLocation, PackageUpdateConfigBean.INSTALLER_JAR_FILENAME);
            if (f.exists()) {
                String javaExe = System.getProperty("java.home") + "/bin/java";
                ProcessBuilder pb = new ProcessBuilder(javaExe, "-jar", f.getAbsolutePath());
                pb.redirectErrorStream(true);
                pb.directory(f.getParentFile());
                try {
                    logger.info("Launching the Update Installer [" + pb.command() + "]");
                    Process p = pb.start();
                    success = true;
                    int retCode = p.waitFor();
                    byte[] buff = HexUtils.slurpStream(p.getInputStream());
                    System.out.println("OUTPUT OF INSTALLER (return code=" + String.valueOf(retCode) + ")");
                    System.out.println(new String(buff));
                } catch (IOException e) {
                    logger.warning("Exception while running the installer: " + e.getMessage());
                    success = false;
                } catch (InterruptedException e) {
                    logger.warning("Exception while running the installer: " + e.getMessage());
                    success = false;
                }
            }
        }
        return success;
    }

    private boolean loadAndExecuteJar(PackageUpdateConfigBean.UpdatePackageInfo info) {
        boolean success = true;
        File f = new File(info.getExpandedLocation(), PackageUpdateConfigBean.INSTALLER_JAR_FILENAME);
        String errorMsg = "Could not run the installer at " + info.getExpandedLocation() + ". ({0})";
        try {
            URL url = f.toURL();
            JarClassLoader jcl = new JarClassLoader(url);
            String mainClass = jcl.getMainClassName();
            jcl.invokeClass(mainClass, new String[0]);
            success = true;
        } catch (MalformedURLException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage()));
            success = false;
        } catch (IOException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage()));
            success = false;
        } catch (NoSuchMethodException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage()));
            success = false;
        } catch (InvocationTargetException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage()));
            success = false;
        } catch (ClassNotFoundException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage()));
            success = false;           
        }
        return success;
    }

    class JarClassLoader extends URLClassLoader {
        private URL url;

        /**
         * Creates a new JarClassLoader for the specified url.
         *
         * @param url the url of the jar file
         */
        public JarClassLoader(URL url) {
            super(new URL[] { url });
            this.url = url;
        }

        /**
         * Returns the name of the jar file main class, or null if
         * no "Main-Class" manifest attributes was defined.
         */
        public String getMainClassName() throws IOException {
            URL u = new URL("jar", "", url + "!/");
            JarURLConnection uc = (JarURLConnection)u.openConnection();
            Attributes attr = uc.getMainAttributes();
            return attr != null ? attr.getValue(Attributes.Name.MAIN_CLASS) : null;
        }

        /**
         * Invokes the application in this jar file given the name of the
         * main class and an array of arguments. The class must define a
         * static method "main" which takes an array of String arguemtns
         * and is of return type "void".
         *
         * @param name the name of the main class
         * @param args the arguments for the application
         * @exception ClassNotFoundException if the specified class could not
         *            be found
         * @exception NoSuchMethodException if the specified class does not
         *            contain a "main" method
         * @exception java.lang.reflect.InvocationTargetException if the application raised an
         *            exception
         */
        public void invokeClass(String name, String[] args)
            throws ClassNotFoundException,
                   NoSuchMethodException,
                InvocationTargetException {
            Class c = loadClass(name);
            Method m = c.getMethod("main", new Class[] { args.getClass() });
            m.setAccessible(true);
            int mods = m.getModifiers();
            if (m.getReturnType() != void.class || !Modifier.isStatic(mods) ||
                !Modifier.isPublic(mods)) {
                throw new NoSuchMethodException("main");
            }
            try {
                m.invoke(null, new Object[] { args });
            } catch (IllegalAccessException e) {
                // This should not happen, as we have disabled access checks
            }
        }
    }
}
