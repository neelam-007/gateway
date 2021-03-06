/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.uddi;

import com.l7tech.objectmodel.Goid;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.policy.UsesPrivateKeys;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependency;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Proxy(lazy=false)
@Table(name="uddi_registries")
public class UDDIRegistry extends ZoneableNamedEntityImp implements UsesPrivateKeys {

    public enum UDDIRegistryType{
        CENTRASITE_ACTIVE_SOA("CentraSite ActiveSOA"),
        CENTRASITE_GOVERNANCE_EDITION("CentraSite Governance Edition"),
        CENTRASITE("CentraSite"),
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
         * @param type String representating the enum
         * @return the UDDIRegistryType which matches the type, or null if not found (for now throwing an exception)
         */
        public static UDDIRegistryType findType(final String type){
            for(UDDIRegistryType regType: values()){
                if(regType.toString().equals(type)) return regType;
            }
            throw new IllegalStateException("Unknown registry type requested: " + type);
        }
    }

    private boolean isEnabled = true;
    private String uddiRegistryType;//todo persist this as an enum to avoid string equals compares everywhere its used
    private String baseUrl;
    private String inquiryUrl;
    private String publishUrl;
    private String securityUrl;
    private String subscriptionUrl;

    /**
     * When using SSL, we should use client auth. Optionally used with a private key
     */
    private boolean clientAuth;
    private Goid keystoreGoid;
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

    @RbacAttribute
    @Column(name="enabled")
    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @RbacAttribute
    @Column(name="registry_type")
    public String getUddiRegistryType() {
        return uddiRegistryType;
    }

    public void setUddiRegistryType(String uddiRegistryType) {
        this.uddiRegistryType = uddiRegistryType;
    }

    @RbacAttribute
    @Column(name="base_url")
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @RbacAttribute
    @Column(name="inquiry_url")
    public String getInquiryUrl() {
        return inquiryUrl;
    }

    public void setInquiryUrl(String inquiryUrl) {
        this.inquiryUrl = inquiryUrl;
    }

    @RbacAttribute
    @Column(name="publish_url")
    public String getPublishUrl() {
        return publishUrl;
    }

    public void setPublishUrl(String publishUrl) {
        this.publishUrl = publishUrl;
    }

    @RbacAttribute
    @Column(name="security_url")
    public String getSecurityUrl() {
        return securityUrl;
    }

    public void setSecurityUrl(String securityUrl) {
        this.securityUrl = securityUrl;
    }

    @RbacAttribute
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

    @Column(name="keystore_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getKeystoreGoid() {
        return keystoreGoid;
    }

    public void setKeystoreGoid(Goid keystoreGoid) {
        this.keystoreGoid = keystoreGoid;
    }

    @Column(name="key_alias")
    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    @RbacAttribute
    @Column(name="user_name")
    public String getRegistryAccountUserName() {
        return registryAccountUserName;
    }

    public void setRegistryAccountUserName(String registryAccountUserName) {
        this.registryAccountUserName = registryAccountUserName;
    }

    @Column(name="password")
    @WspSensitive
    @Dependency(methodReturnType = Dependency.MethodReturnType.VARIABLE, type = Dependency.DependencyType.SECURE_PASSWORD)
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

    /**
     * Get the metrics publish frequency in milliseconds.
     *
     * @return The metrics publish interval
     */
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

    /**
     * Get the monitoring frequency in milliseconds.
     *
     * @return The monitoring interval
     */
    @Column(name="monitor_frequency")
    public long getMonitoringFrequency() {
        return monitoringFrequency;
    }

    public void setMonitoringFrequency(long monitoringFrequency) {
        this.monitoringFrequency = monitoringFrequency;
    }

    /**
     * Should subscription notifications be asynchronous.
     *
     * @return True for asynchronous notification, false for synchronous change tracking
     */
    @Column(name="subscribe_for_notifications")
    public boolean isSubscribeForNotifications() {
        return isSubscribeForNotifications;
    }

    public void setSubscribeForNotifications(boolean subscribeForNotifications) {
        isSubscribeForNotifications = subscribeForNotifications;
    }

    @Override
    @Transient
    public SsgKeyHeader[] getPrivateKeysUsed() {
        if(getKeyAlias() != null) {
            return new SsgKeyHeader[]{new SsgKeyHeader(getKeystoreGoid() + ":" + getKeyAlias(), getKeystoreGoid(), getKeyAlias(), getKeyAlias())};
        }
        return null;
    }

    @Override
    public void replacePrivateKeyUsed(@org.jetbrains.annotations.NotNull final SsgKeyHeader oldSSGKeyHeader, @org.jetbrains.annotations.NotNull final SsgKeyHeader newSSGKeyHeader) {
        if(getKeyAlias() != null) {
            if(Goid.equals(getKeystoreGoid(), oldSSGKeyHeader.getKeystoreId()) && getKeyAlias().equals(oldSSGKeyHeader.getAlias())){
                setKeystoreGoid(newSSGKeyHeader.getKeystoreId());
                setKeyAlias(newSSGKeyHeader.getAlias());
            }
        }
    }

    public void copyFrom (UDDIRegistry copyFrom){
        this.setGoid(copyFrom.getGoid());
        this.setName(copyFrom.getName());
        this.setEnabled(copyFrom.isEnabled());
        this.setUddiRegistryType(copyFrom.getUddiRegistryType());
        this.setBaseUrl(copyFrom.getBaseUrl());
        this.setInquiryUrl(copyFrom.getInquiryUrl());
        this.setPublishUrl(copyFrom.getPublishUrl());
        this.setSecurityUrl(copyFrom.getSecurityUrl());
        this.setSubscriptionUrl(copyFrom.getSubscriptionUrl());
        this.setClientAuth(copyFrom.isClientAuth());
        this.setKeystoreGoid(copyFrom.getKeystoreGoid());
        this.setKeyAlias(copyFrom.getKeyAlias());
        this.setRegistryAccountUserName(copyFrom.getRegistryAccountUserName());
        this.setRegistryAccountPassword(copyFrom.getRegistryAccountPassword());
        this.setMetricsEnabled(copyFrom.isMetricsEnabled());
        this.setMetricPublishFrequency(copyFrom.getMetricPublishFrequency());
        this.setMonitoringEnabled(copyFrom.isMonitoringEnabled());
        this.setMonitoringFrequency(copyFrom.getMonitoringFrequency());
        this.setSubscribeForNotifications(copyFrom.isSubscribeForNotifications());
        this.setSecurityZone(copyFrom.getSecurityZone());
    }
}
