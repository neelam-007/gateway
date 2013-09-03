package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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

    /**
     * Get the name for the Siteminder configuration (case insensitive, required)
     *
     * @return  The name of the Siteminder configuration (may be null)
     */
    public String getName() {
        return get(this.name);
    }

    /**
     * Set the name for the Siteminder configuration
     *
     * @param name The name to use.
     */
    public void setName(String name) {
        this.name = set(this.name, name);
    }

    /**
     * Get the agent name for the Siteminder configuration (case insensitive, required)
     *
     * @return  The agent name of the Siteminder configuration (may be null)
     */
    public String getAgentName() {
        return get(agentName);
    }

    /**
     * Set the agent name for the Siteminder configuration. (required)
     *
     * @param agentName The agent name to use.
     */
    public void setAgentName(String agentName) {
        this.agentName = set(this.agentName, agentName);
    }

    /**
     * Get the address for the Siteminder configuration.
     *
     * @return The address or null
     */
    public String getAddress() {
        return get(this.address);
    }

    /**
     * Set the address for the Siteminder configuration.
     *
     * @param address address
     */
    public void setAddress(String address) {
        this.address = set(this.address, address);
    }

    /**
     * Get the secret for the Siteminder configuration. (required)
     *
     * @return The secret
     */
    public String getSecret() {
        return get(this.secret);
    }


    /**
     * Set the secret for the Siteminder configuration.
     *
     * @param secret The secret
     */
    public void setSecret(String secret) {
        this.secret = set(this.secret, secret);
    }

    /**
     * Get the hostname for the Siteminder configuration (required)
     *
     * @return the hostname
     */
    public String getHostname() {
        return get(this.hostname);
    }

    /**
     * Set the hostname for the Siteminder configuration
     *
     * @param hostname the hostname to use
     */
    public void setHostname(String hostname) {
        this.hostname = set(this.hostname, hostname);
    }

    /**
     * Get the host configuration for the Siteminder configuration
     *
     * @return the host configuration or null
     */
    public String getHostConfiguration() {
        return get(this.hostConfiguration);
    }

    /**
     * Set the host configuration for the Siteminder configuration.
     *
     * @param hostConfiguration the host configuration
     */
    public void setHostConfiguration(String hostConfiguration) {
        this.hostConfiguration = set(this.hostConfiguration, hostConfiguration);
    }

    /**
     * Get the user name for the Siteminder configuration.
     *
     * return the usename or null
     */
    public String getUserName() {
        return get(this.userName);
    }

    /**
     * Set the user name for the Siteminder configuration.
     *
     * @param userName the user name
     */
    public void setUserName(String userName) {
        this.userName = set(this.userName, userName);
    }

    /**
     * Get the identifier of the related secure password for the Siteminder configuration.
     *
     * @return the identifier of the related secure password or null
     */
    public String getPasswordId() {
        return get(this.passwordId);
    }

    /**
     * Set the identifier of the related secure password for the Siteminder configuration.
     *
     * @param passwordId the identifier of the related secure password
     */
    public void setPasswordId(String passwordId) {
        this.passwordId = set(this.passwordId, passwordId);
    }

    /**
     * Get the Check IP flag
     *
     * @return true if check IP
     */
    public boolean getIpCheck() {
        return get(this.ipCheck, false);
    }

    /**
     * Set the Check IP flag
     *
     * @param ipCheck True to check IP
     */
    public void setIpCheck(boolean ipCheck) {
        this.ipCheck = set(this.ipCheck, ipCheck);
    }

    /**
     * The Update SSO Token flag
     *
     * @return true to update the SSO token
     */
    public boolean getUpdateSsoToken() {
        return get(this.updateSsoToken, false);
    }

    /**
     * Set the Update SSO Token flag
     *
     * @param updateSsoToken True to update SSO token
     */
    public void setUpdateSsoToken(boolean updateSsoToken) {
        this.updateSsoToken = set(this.updateSsoToken, updateSsoToken);
    }

    /**
     * The siteminder configuration enabled flag
     *
     * @return True if the siteminder configuration is enabled.
     */
    public boolean getEnabled() {
        return get(this.enabled, false);
    }

    /**
     * Set the siteminder configuration enabled flag
     *
     * @param enabled True if the siteminder configuration is enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = set(this.enabled, enabled);
    }

    /**
     * Get if failover is enabled for non-clustered servers.
     *
     * @return True if failover is enabled
     */
    public boolean getNonClusterFailover() {
        return get(this.nonClusterFailover, false);
    }

    /**
     * Set if failover is enabled for non-clustered servers.
     *
     * @param nonClusterFailover True if failover is enabled
     */
    public void setNonClusterFailover(boolean nonClusterFailover) {
        this.nonClusterFailover = set(this.nonClusterFailover, nonClusterFailover);
    }

    /**
     * Get the FIPs mode
     *
     * <p>Fips mode values:
     * <ul>
     *   <li>0 = Unset</li>
     *   <li>1 = COMPAT</li>
     *   <li>2 = MIGRATE</li>
     *   <li>3 = ONLY</li>
     * </ul>
     * </p>
     *
     * @return  The FIPs mode value, default is 0
     */
    public int getFipsMode() {
        return get(this.fipsMode, 0);
    }

    /**
     * Set the FIPs mode
     *
     * @param fipsMode  The FIPs mode value
     */
    public void setFipsMode(int fipsMode) {
        this.fipsMode = set(this.fipsMode, fipsMode);
    }

    /**
     * Gets the cluster threshold
     *
     * @return  The cluster threshold, default is 50
     */
    public int getClusterThreshold() {
        return get(this.clusterThreshold, 50);
    }

    /**
     * Sets the cluster threshold
     *
     * @param clusterThreshold The cluster threshold
     */
    public void setClusterThreshold(int clusterThreshold) {
        this.clusterThreshold = set(this.clusterThreshold, clusterThreshold);
    }

    /**
     * Get the properties for this siteminder configuration
     *
     * <p>The following cluster settings properties are supported:
     * <ul>
     *   <li>&lt;agent_id&gt;.server.&lt;cluster_seq&gt;.&lt;server_number&gt;.address = 123.101.1.222 </li>
     *   <li>&lt;agent_id&gt;.server.&lt;cluster_seq&gt;.&lt;server_number&gt;.authentication.port = 4442 </li>
     *   <li>&lt;agent_id&gt;.server.&lt;cluster_seq&gt;.&lt;server_number&gt;.authorization.port = 4443  </li>
     *   <li>&lt;agent_id&gt;.server.&lt;cluster_seq&gt;.&lt;server_number&gt;.accounting.port = 4441  </li>
     *   <li>&lt;agent_id&gt;.server.&lt;cluster_seq&gt;.&lt;server_number&gt;.connection.min = 1  </li>
     *   <li>&lt;agent_id&gt;.server.&lt;cluster_seq&gt;.&lt;server_number&gt;.connection.max = 3 </li>
     *   <li>&lt;agent_id&gt;.server.&lt;cluster_seq&gt;.&lt;server_number&gt;.connection.step = 1 </li>
     *   <li>&lt;agent_id&gt;.server.&lt;cluster_seq&gt;.&lt;server_number&gt;.timeout = 75  </li>
     * </ul>
     * </p>
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this siteminder configuration
     *
     * @return The properties to use
     */
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

    @XmlElement(name="Secret")
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

    @XmlElement(name="UserName",required=true)
    protected AttributeExtensibleString getUserNameValue() {
        return userName;
    }

    protected void setUserNameValue(AttributeExtensibleString userName) {
        this.userName = userName;
    }

    @XmlElement(name="PasswordId",required=true)
    protected AttributeExtensibleString getPasswordIdValue() {
        return passwordId;
    }

    protected void setPasswordIdValue(AttributeExtensibleString passwordOid) {
        this.passwordId = passwordOid;
    }

    @XmlElement(name="IpCheck",required=true)
    protected AttributeExtensibleBoolean getIpCheckValue() {
        return ipCheck;
    }

    protected void setIpCheckValue(AttributeExtensibleBoolean ipCheck) {
        this.ipCheck = ipCheck;
    }

    @XmlElement(name="UpdateSsoToken",required=true)
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

    @XmlElement(name="NonClusterFailover",required=true)
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
