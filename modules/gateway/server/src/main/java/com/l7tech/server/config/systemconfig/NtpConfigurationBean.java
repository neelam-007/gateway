package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * User: megery
 * Date: Jun 22, 2006
 * Time: 10:24:14 AM
 */
public class NtpConfigurationBean extends BaseConfigurationBean {
    private List<String > timeServerAddresses;
    private String timeServerName;
    private String timezone;

    public NtpConfigurationBean(String name, String description) {
        super(name, description);
        init();
    }

    private void init() {
        timeServerAddresses = new ArrayList<String>();
    }

    public void reset() {
        timeServerAddresses.clear();
    }

    protected void populateExplanations() {
        List<String> timeServers = getTimeServers();

        if (!timeServers.isEmpty()) {
            explanations.add("Configure NTP on this server");

            for (String tsInfo : timeServers) {
                String s = "\tTime server: " + tsInfo;
                explanations.add(s);
            }
        }
        if (StringUtils.isNotEmpty(timezone)) {
            explanations.add("Configure the timezone on this server");
            explanations.add("\tTimezone: " + timezone + eol);
        }
    }

    public List<String> getTimeServers() {
        return timeServerAddresses;
    }

    public void addTimeServer(String timeServer) {
        timeServerAddresses.add(timeServer);
    }

    public String getTimeServerName() {
        return timeServerName;
    }

    public void setTimeServerName(String timeServerName) {
        this.timeServerName = timeServerName;
    }

    public void setTimeZoneInfo(String tzInfo) {
        this.timezone = tzInfo;
    }


    public String getTimezone() {
        return timezone;
    }
}
