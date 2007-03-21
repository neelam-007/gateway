package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.beans.BaseConfigurationBean;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 4:16:00 PM
 */
public class PackageUpdateConfigBean extends BaseConfigurationBean {
    private String packageLocation;

    public PackageUpdateConfigBean(String name, String description) {
        super(name, description);
    }

    public void setPackageLocation(String location) {
        this.packageLocation = location;
    }


    public String getPackageLocation() {
        return packageLocation;
    }

    public void reset() {
        packageLocation ="";
    }

    protected void populateExplanations() {
    }
}
