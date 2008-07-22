package com.l7tech.server.config.packageupdater;

import com.l7tech.util.FileUtils;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.BaseConfigurationCommand;
import com.l7tech.server.config.packageupdater.installer.UpdatePackageInstaller;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 4:16:10 PM
 */
public class PackageUpdateConfigCommand extends BaseConfigurationCommand {
    private static final Logger logger = Logger.getLogger(PackageUpdateConfigCommand.class.getName());
    private static final String INSTALLER_CLASSNAME = "com.l7tech.server.config.packageupdater.installer.UpdatePackageInstaller";

    PackageUpdateConfigBean packageBean;

    protected PackageUpdateConfigCommand(ConfigurationBean bean) {
        super(bean);
        packageBean = (PackageUpdateConfigBean) bean;
    }

    public boolean execute() {
        boolean success = false;
        PackageUpdateConfigBean.UpdatePackageInfo info = packageBean.getPackageInfo();
        success = loadAndExecuteJar(info);
        if (success)
            FileUtils.deleteDir(new File(info.getExpandedLocation()));

        return success;
    }

    private boolean loadAndExecuteJar(PackageUpdateConfigBean.UpdatePackageInfo info) {
        boolean success = true;
        File f = new File(info.getExpandedLocation(), PackageUpdateConfigBean.INSTALLER_JAR_FILENAME);
        String errorMsg = "Could not run the installer at " + info.getExpandedLocation() + ". ({0} - {1})";
        ClassLoader currentCl = this.getClass().getClassLoader();
        URL installerUrl = null;
        try {
            installerUrl = f.toURL();
            URL[] urls = new URL[]{installerUrl};
            URLClassLoader urlCl = new URLClassLoader(urls, currentCl);
            Class clazz = urlCl.loadClass(INSTALLER_CLASSNAME);
            Constructor ctor = clazz.getConstructor(File.class);
            UpdatePackageInstaller installClass = (UpdatePackageInstaller) ctor.newInstance(f.getParentFile());
            installClass.setLogger(logger);
            int returnVal = installClass.doInstall();
            success = (returnVal == 0);
        } catch (MalformedURLException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage(), e.getClass().getName()));
            success = false;
        } catch (ClassNotFoundException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage(), e.getClass().getName()));
            success = false;
        } catch (IllegalAccessException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage(), e.getClass().getName()));
            success = false;
        } catch (InstantiationException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage(), e.getClass().getName()));
            success = false;
        } catch (NoSuchMethodException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage(), e.getClass().getName()));
            success = false;
        } catch (InvocationTargetException e) {
            logger.severe(MessageFormat.format(errorMsg, e.getMessage(), e.getClass().getName()));
            success = false;
        }
        return success;
    }
}
