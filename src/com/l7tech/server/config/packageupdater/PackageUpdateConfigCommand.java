package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.BaseConfigurationCommand;
import com.l7tech.common.util.HexUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.logging.Logger;

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
        if (info != null) {
            String expandedLocation = info.getExpandedLocation();
            File f = new File(expandedLocation, PackageUpdateConfigBean.INSTALLER_JAR_FILENAME);
            if (f.exists()) {
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", f.getAbsolutePath());
                pb.redirectErrorStream(true);
                pb.directory(f.getParentFile());
                try {
                    Process p = pb.start();
                    success = true;
                    int retCode = p.waitFor();
                    byte[] buff = HexUtils.slurpStream(p.getInputStream());
                    logger.info("OUTPUT OF INSTALLER (return code=" + String.valueOf(retCode) + ")");
                    logger.info(new String(buff));
                } catch (IOException e) {
                    logger.warning("Exception while running the installer: " + e.getMessage());
                    success = false;
                } catch (InterruptedException e) {
                    logger.warning("Exception while running the installer: " + e.getMessage());
                    success = false;
                }
//                try {
//                    URL jarUrl = f.toURL();
//                    JarClassLoader jarcl = new JarClassLoader(jarUrl);
//                    String mainClass = jarcl.getMainClassName();
//                    jarcl.invokeClass(mainClass, new String[0]);
//                    success = true;
//                } catch (MalformedURLException e) {
//                    logger.severe(MessageFormat.format("Error while loading the Update Installer. Cannot proceed.({0})\n{1}", e.getClass().getName(), e.getMessage()));
//                } catch (IOException e) {
//                    logger.severe(MessageFormat.format("Error while loading the Update Installer. Cannot proceed.({0})\n{1}", e.getClass().getName(), e.getMessage()));
//                } catch (NoSuchMethodException e) {
//                    logger.severe(MessageFormat.format("Error while loading the Update Installer. Cannot proceed.({0})\n{1}", e.getClass().getName(), e.getMessage()));
//                } catch (InvocationTargetException e) {
//                    logger.severe(MessageFormat.format("Error while loading the Update Installer. Cannot proceed.({0})\n{1}", e.getClass().getName(), e.getMessage()));
//                } catch (ClassNotFoundException e) {
//                    logger.severe(MessageFormat.format("Error while loading the Update Installer. Cannot proceed.({0})\n{1}", e.getClass().getName(), e.getMessage()));
//                }
            }
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
