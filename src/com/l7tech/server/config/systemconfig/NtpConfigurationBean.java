package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;
import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: Jun 22, 2006
 * Time: 10:24:14 AM
 */
public class NtpConfigurationBean extends BaseConfigurationBean {
    private String timeServerAddress;
    private String timeServerName;

    public NtpConfigurationBean(String name, String description) {
        super(name, description);
        init();
    }

    private void init() {
    }

    public void reset() {
    }

    protected void populateExplanations() {
        String tsAddress = getTimeServerAddress();
        String tsName = getTimeServerName();

        if (StringUtils.isNotEmpty(tsAddress)) {
            explanations.add("Configure NTP on this server");
            String s = "\tTime server: " + getTimeServerAddress();

            if (!StringUtils.equals(tsName, tsAddress)) {
               s += " (" + tsName + ")";
            }
            s += eol;
            explanations.add(s);
        }
    }

    public String getTimeServerAddress() {
        return timeServerAddress;
    }

    public String getTimeServerName() {
        return timeServerName;
    }

    public void setTimeServerName(String timeServerName) {
        this.timeServerName = timeServerName;
    }

    public void setTimeServerAddress(String timeServerAddress) {
        this.timeServerAddress = timeServerAddress;
    }
}
