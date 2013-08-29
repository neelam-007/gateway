package com.l7tech.gateway.common.siteminder;


import com.l7tech.objectmodel.Goid;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

public class SiteMinderHost implements Serializable {

    private final static String HOST_NAME = "hostname";
    private final static String HOST_CONFIG_OBJECT = "hostconfigobject";
    private final static String POLICY_SERVER = "policyserver";
    private final static String REQUEST_TIMEOUT = "requesttimeout";
    private final static String SHARED_SECRET = "sharedsecret";
    private final static String SHARED_SECRET_TIME = "sharedsecrettime";
    private final static String FIPS_MODE = "fipsmode";

    private String hostname;
    private String hostConfigObject;
    private String policyServer;
    private Integer requestTimeout;
    private String sharedSecret;
    private Long sharedSecretTime;
    private SiteMinderFipsModeOption fipsMode;
    private String userName;
    private Goid passwordGoid;

    public SiteMinderHost(String hostName, String policyServer, String hostConfigObject,
                          SiteMinderFipsModeOption fipsMode, String userName, Goid passwordGoid) {
        this.hostname = hostName;
        this.hostConfigObject = hostConfigObject;
        this.policyServer = policyServer;
        this.requestTimeout = 0;
        this.sharedSecret = "";
        this.sharedSecretTime = 0L;
        this.fipsMode = fipsMode;
        this.userName = userName;
        this.passwordGoid = passwordGoid;
    }


    public SiteMinderHost(String smHostConfPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(smHostConfPath))) {
            Properties prop = new Properties();
            prop.load(fis);
            hostname = getValue(prop, HOST_NAME);
            hostConfigObject = getValue(prop, HOST_CONFIG_OBJECT);
            policyServer = getValue(prop, POLICY_SERVER);
            requestTimeout = getIntValue(prop, REQUEST_TIMEOUT);
            sharedSecret = getValue(prop, SHARED_SECRET);
            sharedSecretTime = getLongValue(prop, SHARED_SECRET_TIME);
            fipsMode = getFipsMode(prop, FIPS_MODE);
        }
    }

    private Integer getIntValue(Properties properties, String attr) {
        String value = getValue(properties, attr);
        if (value != null) {
            return new Integer(value);
        }
        return null;
    }

    private Long getLongValue(Properties properties, String attr) {
        String value = getValue(properties, attr);
        if (value != null) {
            return new Long(value);
        }
        return null;
    }

    private SiteMinderFipsModeOption getFipsMode(Properties properties, String attr) {
        String name = getValue(properties, attr);

        return SiteMinderFipsModeOption.getByName(name);
    }

    private String getValue(Properties properties, String attr) {
        String value = properties.getProperty(attr);
        if (value != null) {
            return value.replaceAll("^\"|\"$", "");
        }
        return null;
    }

    public String getHostname() {
        return hostname;
    }

    public String getHostConfigObject() {
        return hostConfigObject;
    }

    public String getPolicyServer() {
        return policyServer;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public Long getSharedSecretTime() {
        return sharedSecretTime;
    }

    public SiteMinderFipsModeOption getFipsMode() {
        return fipsMode;
    }

    public void setUserName(String userName){
        this.userName = userName;
    }


    public String getUserName() {
        return userName;
    }

    public void setPasswordGoid(Goid passwordGoid){
        this.passwordGoid = passwordGoid;
    }

    public Goid getPasswordGoid() {
        return passwordGoid;
    }
}
