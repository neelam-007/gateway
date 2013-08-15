package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The SiteMinderConfigurationMO object represents a Siteminder configuration.
 */
@XmlRootElement(name="SiteMinderConfiguration")
@XmlType(name="SiteMinderConfigurationType", propOrder={"nameValue", "agentNameValue", "addressValue", "hostnameValue", "hostConfigurationValue", "userNameValue", "passwordIdValue", "enabledValue", "fipsModeValue", "ipCheckValue", "updateSsoTokenValue", "clusterThresholdValue", "nonClusterFailoverValue", "secretValue", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "siteMinderConfigurations")
public class SiteMinderConfigurationMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    public String getName() {
        return get(this.name);
    }

    public void setName(String name) {
        this.name = set(this.name, name);
    }

    public String getAgentName() {
        return get(agentName);
    }

    public void setAgentName(String agentName) {
        this.agentName = set(this.agentName, agentName);
    }

    public String getAddress() {
        return get(this.address);
    }

    public void setAddress(String address) {
        this.address = set(this.address, address);
    }

    public String getSecret() {
        return get(this.secret);
    }

    public void setSecret(String secret) {
        this.secret = set(this.secret, secret);
    }

    public String getHostname() {
        return get(this.hostname);
    }

    public void setHostname(String hostname) {
        this.hostname = set(this.hostname, hostname);
    }

    public String getHostConfiguration() {
        return get(this.hostConfiguration);
    }

    public void setHostConfiguration(String hostConfiguration) {
        this.hostConfiguration = set(this.hostConfiguration, hostConfiguration);
    }

    public String getUserName() {
        return get(this.userName);
    }

    public void setUserName(String userName) {
        this.userName = set(this.userName, userName);
    }

    public String getPasswordId() {
        return get(this.passwordId);
    }

    public void setPasswordId(String passwordId) {
        this.passwordId = set(this.passwordId, passwordId);
    }

    public boolean getIpCheck() {
        return get(this.ipCheck);
    }

    public void setIpCheck(boolean ipCheck) {
        this.ipCheck = set(this.ipCheck, ipCheck);
    }

    public boolean getUpdateSsoToken() {
        return get(this.updateSsoToken);
    }

    public void setUpdateSsoToken(boolean updateSsoToken) {
        this.updateSsoToken = set(this.updateSsoToken, updateSsoToken);
    }

    public boolean getEnabled() {
        return get(this.enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = set(this.enabled, enabled);
    }

    public boolean getNonClusterFailover() {
        return get(this.nonClusterFailover);
    }

    public void setNonClusterFailover(boolean nonClusterFailover) {
        this.nonClusterFailover = set(this.nonClusterFailover, nonClusterFailover);
    }

    public int getFipsMode() {
        return get(this.fipsMode);
    }

    public void setFipsMode(int fipsMode) {
        this.fipsMode = set(this.fipsMode, fipsMode);
    }

    public int getClusterThreshold() {
        return get(this.clusterThreshold);
    }

    public void setClusterThreshold(int clusterThreshold) {
        this.clusterThreshold = set(this.clusterThreshold, clusterThreshold);
    }

    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="Name",required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue(AttributeExtensibleString name) {
        this.name = name;
    }

    @XmlElement(name="AgentName",required=true)
    protected AttributeExtensibleString getAgentNameValue() {
        return agentName;
    }

    protected void setAgentNameValue(AttributeExtensibleString agentName) {
        this.agentName = agentName;
    }

    @XmlElement(name="Address",required=true)
    protected AttributeExtensibleString getAddressValue() {
        return address;
    }

    protected void setAddressValue(AttributeExtensibleString address) {
        this.address = address;
    }

    @XmlElement(name="Secret",required=true)
    protected AttributeExtensibleString getSecretValue() {
        return secret;
    }

    protected void setSecretValue(AttributeExtensibleString secret) {
        this.secret = secret;
    }

    @XmlElement(name="Hostname",required=true)
    protected AttributeExtensibleString getHostnameValue() {
        return hostname;
    }

    protected void setHostnameValue(AttributeExtensibleString hostname) {
        this.hostname = hostname;
    }

    @XmlElement(name="HostConfiguration")
    protected AttributeExtensibleString getHostConfigurationValue() {
        return hostConfiguration;
    }

    protected void setHostConfigurationValue(AttributeExtensibleString hostConfiguration) {
        this.hostConfiguration = hostConfiguration;
    }

    @XmlElement(name="UserName")
    protected AttributeExtensibleString getUserNameValue() {
        return userName;
    }

    protected void setUserNameValue(AttributeExtensibleString userName) {
        this.userName = userName;
    }

    @XmlElement(name="PasswordId")
    protected AttributeExtensibleString getPasswordIdValue() {
        return passwordId;
    }

    protected void setPasswordIdValue(AttributeExtensibleString passwordOid) {
        this.passwordId = passwordOid;
    }

    @XmlElement(name="IpCheck")
    protected AttributeExtensibleBoolean getIpCheckValue() {
        return ipCheck;
    }

    protected void setIpCheckValue(AttributeExtensibleBoolean ipCheck) {
        this.ipCheck = ipCheck;
    }

    @XmlElement(name="UpdateSsoToken")
    protected AttributeExtensibleBoolean getUpdateSsoTokenValue() {
        return updateSsoToken;
    }

    protected void setUpdateSsoTokenValue(AttributeExtensibleBoolean updateSsoToken) {
        this.updateSsoToken = updateSsoToken;
    }

    @XmlElement(name="Enabled",required=true)
    protected AttributeExtensibleBoolean getEnabledValue() {
        return enabled;
    }

    protected void setEnabledValue(AttributeExtensibleBoolean enabled) {
        this.enabled = enabled;
    }

    @XmlElement(name="NonClusterFailover")
    protected AttributeExtensibleBoolean getNonClusterFailoverValue() {
        return nonClusterFailover;
    }

    protected void setNonClusterFailoverValue(AttributeExtensibleBoolean nonClusterFailover) {
        this.nonClusterFailover = nonClusterFailover;
    }

    @XmlElement(name="FipsMode")
    protected AttributeExtensibleInteger getFipsModeValue() {
        return fipsMode;
    }

    protected void setFipsModeValue(AttributeExtensibleInteger fipsMode) {
        this.fipsMode = fipsMode;
    }

    @XmlElement(name="ClusterThreshold")
    protected AttributeExtensibleInteger getClusterThresholdValue() {
        return clusterThreshold;
    }

    protected void setClusterThresholdValue(AttributeExtensibleInteger clusterThreshold) {
        this.clusterThreshold = clusterThreshold;
    }

    //- PACKAGE

    SiteMinderConfigurationMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleString agentName;
    private AttributeExtensibleString address;
    private AttributeExtensibleString secret;
    private AttributeExtensibleString hostname;
    private AttributeExtensibleString hostConfiguration;
    private AttributeExtensibleString userName;
    private AttributeExtensibleString passwordId;

    private AttributeExtensibleBoolean ipCheck;
    private AttributeExtensibleBoolean updateSsoToken;
    private AttributeExtensibleBoolean enabled;
    private AttributeExtensibleBoolean nonClusterFailover;

    private AttributeExtensibleInteger fipsMode;
    private AttributeExtensibleInteger clusterThreshold;

    private Map<String, String> properties;

}
