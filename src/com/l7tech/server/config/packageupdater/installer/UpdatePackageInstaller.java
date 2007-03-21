package com.l7tech.server.config.packageupdater.installer;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 3:16:28 PM
 */
public class UpdatePackageInstaller {
    public UpdatePackageInstaller() {
    }

    public static void main(String[] args) {
        UpdatePackageInstaller installer = new UpdatePackageInstaller();
        installer.doInstall();
    }

    public void doInstall() {
        System.out.println("performing the package update installation");
    }
}
