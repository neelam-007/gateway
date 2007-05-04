package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.beans.BaseConfigurationBean;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 4:16:00 PM
 */
public class PackageUpdateConfigBean extends BaseConfigurationBean {
    public static final String PACKAGE_UPDATE_EXTENSION = "sup";
    public static final String DESCRIPTION_FILENAME = "description.txt";
    public static final String INSTALLER_JAR_FILENAME = "UpdateInstaller.jar";
    public static final String UPDATE_FILE_LIST = "updatefiles.txt";

    private UpdatePackageInfo packageInfo;

    public PackageUpdateConfigBean(String name, String description) {
        super(name, description);
    }

    public void reset() {
        packageInfo = null;
    }

    protected void populateExplanations() {
        explanations.add("Install the following update package:");
        explanations.add(insertTab + "Package: " + packageInfo.getOriginalLocation());
        explanations.add(insertTab + "Description: " + packageInfo.getDescription());
    }

    public void setPackageInformation(UpdatePackageInfo updatePackageInfo) {
        this.packageInfo = updatePackageInfo;
    }

    public UpdatePackageInfo getPackageInfo() {
        return packageInfo;
    }

    public static class UpdatePackageInfo {
        private String description;
        private String originalLocation;
        private String expandedLocation;


        public UpdatePackageInfo() {
        }

        public String getDescription() {
            return description == null?"":description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getOriginalLocation() {
            return originalLocation;
        }

        public void setOriginalLocation(String originalLocation) {
            this.originalLocation = originalLocation;
        }

        public String getExpandedLocation() {
            return expandedLocation;
        }

        public void setExpandedLocation(String expandedLocation) {
            this.expandedLocation = expandedLocation;
        }
    }
}
