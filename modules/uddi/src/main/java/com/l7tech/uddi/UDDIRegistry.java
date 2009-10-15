/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

import org.hibernate.annotations.Proxy;

import javax.persistence.*;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.BeanUtils;

import java.lang.reflect.InvocationTargetException;

@Entity
@Proxy(lazy=false)
@Table(name="uddi_registries")
public class UDDIRegistry extends NamedEntityImp {

    public enum UDDIRegistryType{
        CENTRASITE_ACTIVE_SOA("CentraSite Active SOA"),
        CENTRASITE_GOVERNANCE_EDITION("CentraSite Governance Edition"),
        CENTRASITE("CentraSite"),
        ORACLE_REGISTRY("Oracle Registry"),
        SYSTINET("Systinet"),
        GENERIC("Generic");

        private String typeName;

        private UDDIRegistryType(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            return typeName;
        }

        /**
         * Convert the string into a UDDIRegistryType.
         * @param type the String representation of the enum
         * @return the UDDIRegistryType which matches the type, or null if not found
         */
        public static UDDIRegistryType findType(final String type){
            for(UDDIRegistryType regType: values()){
                if(regType.toString().equals(type)) return regType;
            }
            return null;
        }
    }

    private boolean isEnabled = true;
    private String uddiRegistryType;
    private String baseUrl;
    private String inquiryUrl;
    private String publishUrl;
    private String securityUrl;
    private String subscriptionUrl;

    /**
     * When using SSL, we should use client auth. Optionally used with a private key
     */
    private boolean clientAuth;
    private Long keystoreOid;
    private String keyAlias;


    private String registryAccountUserName;
    private String registryAccountPassword;

    /**
     * Global setting for entire UDDI Registry
     */
    private boolean isMetricsEnabled;

    private long metricPublishFrequency;

    private boolean isMonitoringEnabled;

    private long monitoringFrequency;

    /**
     * If false, then we are using a subscription to retrieve notifications
     */
    private boolean isSubscribeForNotifications;

    public UDDIRegistry() {
    }

    public UDDIRegistry(long oid, String name, String uddiRegistryType, String baseUrl, String inquiryUrl, String publishUrl, String securityUrl, String subscriptionUrl, boolean clientAuth, long keystoreOid, String keyAlias, String registryAccountUserName, String registryAccountPassword, boolean metricsEnabled, long metricPublishFrequency, boolean monitoringEnabled, long monitoringFrequency, boolean subscribeForNotifications) {
        super();
        setOid(oid);
        setName(name);
        this.uddiRegistryType = uddiRegistryType;
        this.baseUrl = baseUrl;
        this.inquiryUrl = inquiryUrl;
        this.publishUrl = publishUrl;
        this.securityUrl = securityUrl;
        this.subscriptionUrl = subscriptionUrl;
        this.clientAuth = clientAuth;
        this.keystoreOid = keystoreOid;
        this.keyAlias = keyAlias;
        this.registryAccountUserName = registryAccountUserName;
        this.registryAccountPassword = registryAccountPassword;
        isMetricsEnabled = metricsEnabled;
        this.metricPublishFrequency = metricPublishFrequency;
        isMonitoringEnabled = monitoringEnabled;
        this.monitoringFrequency = monitoringFrequency;
        isSubscribeForNotifications = subscribeForNotifications;
    }

    @Column(name="enabled")
    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Column(name="registry_type")
    public String getUddiRegistryType() {
        return uddiRegistryType;
    }

    public void setUddiRegistryType(String uddiRegistryType) {
        this.uddiRegistryType = uddiRegistryType;
    }

    @Column(name="base_url")
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Column(name="inquiry_url")
    public String getInquiryUrl() {
        return inquiryUrl;
    }

    public void setInquiryUrl(String inquiryUrl) {
        this.inquiryUrl = inquiryUrl;
    }

    @Column(name="publish_url")
    public String getPublishUrl() {
        return publishUrl;
    }

    public void setPublishUrl(String publishUrl) {
        this.publishUrl = publishUrl;
    }

    @Column(name="security_url")
    public String getSecurityUrl() {
        return securityUrl;
    }

    public void setSecurityUrl(String securityUrl) {
        this.securityUrl = securityUrl;
    }

    @Column(name="subscription_url")
    public String getSubscriptionUrl() {
        return subscriptionUrl;
    }

    public void setSubscriptionUrl(String subscriptionUrl) {
        this.subscriptionUrl = subscriptionUrl;
    }

    @Column(name="client_auth")
    public boolean isClientAuth() {
        return clientAuth;
    }

    public void setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    @Column(name="keystore_oid")
    public Long getKeystoreOid() {
        return keystoreOid;
    }

    public void setKeystoreOid(Long keystoreOid) {
        this.keystoreOid = keystoreOid;
    }

    @Column(name="key_alias")
    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    @Column(name="user_name")
    public String getRegistryAccountUserName() {
        return registryAccountUserName;
    }

    public void setRegistryAccountUserName(String registryAccountUserName) {
        this.registryAccountUserName = registryAccountUserName;
    }

    @Column(name="password")
    public String getRegistryAccountPassword() {
        return registryAccountPassword;
    }

    public void setRegistryAccountPassword(String registryAccountPassword) {
        this.registryAccountPassword = registryAccountPassword;
    }

    @Column(name="metrics_enabled")
    public boolean isMetricsEnabled() {
        return isMetricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        isMetricsEnabled = metricsEnabled;
    }

    @Column(name="metrics_publish_frequency")
    public long getMetricPublishFrequency() {
        return metricPublishFrequency;
    }

    public void setMetricPublishFrequency(long metricPublishFrequency) {
        this.metricPublishFrequency = metricPublishFrequency;
    }

    @Column(name="monitoring_enabled")
    public boolean isMonitoringEnabled() {
        return isMonitoringEnabled;
    }

    public void setMonitoringEnabled(boolean monitoringEnabled) {
        isMonitoringEnabled = monitoringEnabled;
    }

    @Column(name="monitor_frequency")
    public long getMonitoringFrequency() {
        return monitoringFrequency;
    }

    public void setMonitoringFrequency(long monitoringFrequency) {
        this.monitoringFrequency = monitoringFrequency;
    }

    @Column(name="subscribe_for_notifications")
    public boolean isSubscribeForNotifications() {
        return isSubscribeForNotifications;
    }

    public void setSubscribeForNotifications(boolean subscribeForNotifications) {
        isSubscribeForNotifications = subscribeForNotifications;
    }
}
