package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.beans.BaseConfigurationBean;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 4:16:00 PM
 */
public class PackageUpdateConfigBean extends BaseConfigurationBean {
    public static final String PACKAGE_UPDATE_EXTENSION = "sup";
    public static String DESCRIPTION_FILENAME = "description.txt";
    public static String INSTALLER_JAR_FILENAME = "UpdateInstaller.jar";

    private UpdatePackageInfo packageInfo;

    public PackageUpdateConfigBean(String name, String description) {
        super(name, description);
    }

    public void reset() {
        packageInfo = null;
    }

    protected void populateExplanations() {
    }

    public void setPackageInformation(UpdatePackageInfo updatePackageInfo) {
        this.packageInfo = updatePackageInfo;
    }

    public UpdatePackageInfo getPackageInfo() {
        return packageInfo;
    }

    public static class UpdatePackageInfo {
        private String description;
        private String pathLocation;


        public UpdatePackageInfo() {
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPathLocation() {
            return pathLocation;
        }

        public void setPathLocation(String pathLocation) {
            this.pathLocation = pathLocation;
        }
    }
}
