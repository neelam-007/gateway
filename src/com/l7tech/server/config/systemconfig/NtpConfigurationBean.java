package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;
import com.l7tech.common.util.Pair;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * User: megery
 * Date: Jun 22, 2006
 * Time: 10:24:14 AM
 */
public class NtpConfigurationBean extends BaseConfigurationBean {
    private Map< String, String > timeServerAddresses;
    private String timeServerName;
    private String timezone;

    public NtpConfigurationBean(String name, String description) {
        super(name, description);
        init();
    }

    private void init() {
        timeServerAddresses = new LinkedHashMap<String, String>();
    }

    public void reset() {
        timeServerAddresses.clear();
    }

    protected void populateExplanations() {
        Map<String, String> tsInfos = getTimeServers();

        if (!tsInfos.isEmpty()) {
            explanations.add("Configure NTP on this server");

            for (Map.Entry<String, String> tsInfo : tsInfos.entrySet()) {
                String address = tsInfo.getKey();
                String name = tsInfo.getValue();
                String s = "\tTime server: " + address;
                if (!StringUtils.equals(address, name)) {
                    s += " (" + name + ")";
                }
                explanations.add(s);
            }
        }
        if (StringUtils.isNotEmpty(timezone)) {
            explanations.add("Configure the timezone on this server");
            explanations.add("\tTimezone: " + timezone + eol);
        }
    }

    public Map<String, String> getTimeServers() {
        return timeServerAddresses;
    }

    public void addTimeServer(String timeServerAddress, String timeServerName) {
        timeServerAddresses.put(timeServerAddress, timeServerName);
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
