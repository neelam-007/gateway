package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.util.CollectionUtils;
import java.util.List;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import org.jetbrains.annotations.Nullable;

/**
 * The AuditFtpConfig managed object represents an FTP configuration.
 *
 * @see AuditConfigurationMO
 * @see AuditConfigurationMO#getFtpConfig()
 * @see AuditConfigurationMO#setFtpConfig(AuditFtpConfig)
 * @see ManagedObjectFactory#createAuditFtpConfig()
 */
@XmlRootElement(name="FtpConfig")
@XmlType(name="AuditFtpConfigType", propOrder={"host", "port", "timeout", "user", "password", "directory", "verifyServerCert", "security", "enabled", "extensions"})
public class AuditFtpConfig extends ManagedObject {

    //- PUBLIC

    /**
     * Get the host (required)
     *
     * @return The host
     */
    @XmlElement(name="Host", required=true)
    public String getHost() {
        return host;
    }

    /**
     * Set the host.
     *
     * @param host The host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get the port (required)
     *
     * @return The port
     */
    @XmlElement(name="Port", required=true)
    public int getPort() {
        return port;
    }

    /**
     * Set the port.
     *
     * @param port The port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Get the timeout (required)
     *
     * @return The timeout
     */
    @XmlElement(name="Timeout", required=true)
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Set the timeout.
     *
     * @param timeout The timeout
     */
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    /**
     * Get the user
     *
     * @return The user
     */
    @XmlElement(name="User")
    @Nullable
    public String getUser() {
        return user;
    }

    /**
     * Set the user.
     *
     * @param user Tbe user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Get the password
     *
     * @return The password
     */
    @XmlElement(name="Password")
    @Nullable
    public AttributeExtensibleType.AttributeExtensibleString getPassword(){
        return password;
    }

    /**
     * Set the password.
     *
     * @param password The password
     */
    public void setPassword(final AttributeExtensibleType.AttributeExtensibleString password) {
        this.password = password;
    }

    @Nullable
    public String getPasswordValue() {
        return password == null ? null : password.getValue();
    }

    public void setPasswordValue(String password) {
        this.password = new AttributeExtensibleType.AttributeExtensibleString();
        this.password.setValue(password);
    }

    public void setPassword(final String password, final String bundleKey){
        this.password = new AttributeExtensibleType.AttributeExtensibleString();
        this.password.setValue(password);
        this.password.setAttributeExtensions(CollectionUtils.<QName,Object>mapBuilder().put(new QName("bundleKey"),bundleKey).map());
    }

    public String getPasswordBundleKey() {
        if (this.password != null && this.password.getAttributeExtensions() != null) {
            return (String) this.password.getAttributeExtensions().get(new QName("bundleKey"));
        }
        return null;
    }

    /**
     * Get the directory
     *
     * @return The directory
     */
    @XmlElement(name="Directory")
    @Nullable
    public String getDirectory() {
        return directory;
    }


    /**
     * Set the directory.
     *
     * @param directory The directory
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * Get if verify server certificate
     *
     * @return whether to verify server certificate
     */
    @XmlElement(name="VerifyServerCert")
    @Nullable
    public Boolean isVerifyServerCert() {
        return verifyServerCert;
    }

    /**
     * Set if verify server certificate
     *
     * @param verifyServerCert whether to verify server certificate
     */
    public void setVerifyServerCert(Boolean verifyServerCert) {
        this.verifyServerCert = verifyServerCert;
    }

    /**
     * Get the security (required)
     *
     * @return The security
     */
    @XmlElement(name="Security", required=true)
    public SecurityType getSecurity() {
        return security;
    }

    /**
     * Set the security.
     *
     * @param security The security
     */
    public void setSecurity(SecurityType security) {
        this.security = security;
    }

    /**
     * Get if enabled (required)
     *
     * @return Is enabled
     */
    @XmlElement(name="Enabled", required=true)
    public Boolean isEnabled() {
        return enabled;
    }

    /**
     * Set if enabled
     *
     * @param enabled Is enabled
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }


    /**
     * Security type
     */
    @XmlType(name="SecurityTypeType")
    public enum SecurityType {
        @XmlEnumValue("FTP_UNSECURED") ftp,
        @XmlEnumValue("FTPS_EXPLICIT") ftpsExplicit,
        @XmlEnumValue("FTPS_IMPLICIT") ftpsImplicit
    }

    //- PROTECTED

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    //- PACKAGE

    AuditFtpConfig() {
    }

    //- PRIVATE

    private String host;
    private Integer port ;
    private Integer timeout;
    private String user;
    private AttributeExtensibleType.AttributeExtensibleString password;
    private String directory;
    private Boolean verifyServerCert;
    private SecurityType security;
    private Boolean enabled;
}
