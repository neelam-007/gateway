package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.*;

/**
 * The HttpConfigurationMO managed object represents a http configuration.
 * <p/>
 * The Accessor for http configurations supports read and write. HTTP configurations can be accessed by identifier.
 *
 * @author Victor Kazakov
 * @see com.l7tech.gateway.api.ManagedObjectFactory#createHttpConfiguration()
 */
@XmlRootElement(name = "HttpConfiguration")
@XmlType(name = "HttpConfigurationType", propOrder = {"host", "port", "protocol", "path", "username", "passwordId", "ntlmHost", "ntlmDomain", "tlsVersion", "tlsKeyUse", "tlsKeystoreId", "tlsKeystoreAlias", "tlsCipherSuites", "connectTimeout", "readTimeout", "followRedirects", "proxyUse", "proxyConfiguration", "extensions", "extension"})
@AccessorSupport.AccessibleResource(name = "httpConfigurations")
public class HttpConfigurationMO extends ElementExtendableAccessibleObject {
    private String host;
    private int port;
    private Protocol protocol;
    private String path;
    private String username;
    private String passwordId;
    private String ntlmHost;
    private String ntlmDomain;
    private String tlsVersion;
    private Option tlsKeyUse;
    private String tlsKeystoreId;
    private String tlsKeystoreAlias;
    private String tlsCipherSuites;
    private int connectTimeout;
    private int readTimeout;
    private boolean followRedirects;
    private Option proxyUse;
    private HttpProxyConfiguration proxyConfiguration;

    HttpConfigurationMO() {
    }

    /**
     * Get the host name of the http configuration. This is required and must not ve null.
     *
     * @return the host name of the http configuration
     */
    @XmlElement(name = "Host", required = true)
    public String getHost() {
        return host;
    }

    /**
     * Set the host name of the http configuration
     *
     * @param host The host name to set the http configuration for
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the port specified. 0 means any port. Defaults to 0
     *
     * @return The port used.
     */
    @XmlElement(name = "Port", defaultValue = "0")
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number. 0 for any port
     *
     * @param port The port number to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the protocol used. Either: 'any', 'http', or 'https'. Defaults to 'any'
     *
     * @return The protocol used.
     */
    @XmlElement(name = "Protocol", defaultValue = "any")
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Set the protocol. Either: 'any', 'http', or 'https'
     *
     * @param protocol The protocol
     */
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Get the path. This is required and must not ve null.
     *
     * @return The uri path
     */
    @XmlElement(name = "Path", required = true)
    public String getPath() {
        return path;
    }

    /**
     * Set the path
     *
     * @param path The path to set it to
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get the user name. If a user name exists then a password id must also be present. Defaults to null
     *
     * @return The username
     */
    @XmlElement(name = "Username")
    public String getUsername() {
        return username;
    }

    /**
     * Set the user name
     *
     * @param username The username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the password id. This is the goid of the password. If a user name exists then a password id must also be
     * present. Defaults to null
     *
     * @return The password id.
     */
    @XmlElement(name = "PasswordId")
    public String getPasswordId() {
        return passwordId;
    }

    /**
     * Set the password id.
     *
     * @param passwordId The password ID.
     */
    public void setPasswordId(String passwordId) {
        this.passwordId = passwordId;
    }

    /**
     * Get the NTLM Host. Defaults to null
     *
     * @return The NTLM Host
     */
    @XmlElement(name = "NtlmHost")
    public String getNtlmHost() {
        return ntlmHost;
    }

    /**
     * Set the NTLM Host
     *
     * @param ntlmHost The NTLM Host
     */
    public void setNtlmHost(String ntlmHost) {
        this.ntlmHost = ntlmHost;
    }

    /**
     * Get the NTLM Domain. Defaults to null
     *
     * @return The NTLM Domain
     */
    @XmlElement(name = "NtlmDomain")
    public String getNtlmDomain() {
        return ntlmDomain;
    }

    /**
     * Set the NTLM Domain
     *
     * @param ntlmDomain The ntlm domain
     */
    public void setNtlmDomain(String ntlmDomain) {
        this.ntlmDomain = ntlmDomain;
    }

    /**
     * The TLS version used.
     * <p/>
     * One of: "ANY", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2". Defaults to 'any'
     *
     * @return The tls version
     */
    @XmlElement(name = "TlsVersion", defaultValue = "ANY")
    public String getTlsVersion() {
        return tlsVersion;
    }

    /**
     * Set the tls version
     *
     * @param tlsVersion The tls version
     */
    public void setTlsVersion(String tlsVersion) {
        this.tlsVersion = tlsVersion;
    }

    /**
     * Specified what typeof tls key to use. Either: 'default', 'none', or 'custom'
     *
     * @return They type of tls key used.
     */
    @XmlElement(name = "TlsKeyUse", defaultValue = "default")
    public Option getTlsKeyUse() {
        return tlsKeyUse;
    }

    /**
     * Sets the type of tls key to use.
     *
     * @param tlsKeyUse The type of tls key to use
     */
    public void setTlsKeyUse(Option tlsKeyUse) {
        this.tlsKeyUse = tlsKeyUse;
    }

    /**
     * The tls key id. This is only required if the TlsKeyUse is 'custom'
     *
     * @return The tls key id
     */
    @XmlElement(name = "TlsKeystoreId")
    public String getTlsKeystoreId() {
        return tlsKeystoreId;
    }

    /**
     * Returns the tls key id
     *
     * @param tlsKeystoreId The tls key id
     */
    public void setTlsKeystoreId(String tlsKeystoreId) {
        this.tlsKeystoreId = tlsKeystoreId;
    }

    /**
     * The tls key alias. This is only required if the TlsKeyUse is 'custom'
     *
     * @return The tls key alias
     */
    @XmlElement(name = "TlsKeystoreAlias")
    public String getTlsKeystoreAlias() {
        return tlsKeystoreAlias;
    }

    /**
     * Sets the tls key alias
     *
     * @param tlsKeystoreAlias The tls key alias
     */
    public void setTlsKeystoreAlias(String tlsKeystoreAlias) {
        this.tlsKeystoreAlias = tlsKeystoreAlias;
    }

    /**
     * Returns the tls cipher suites used. Null means to use the default cipher suites
     *
     * @return The cipher suites used.
     */
    @XmlElement(name = "TlsCipherSuites")
    public String getTlsCipherSuites() {
        return tlsCipherSuites;
    }

    /**
     * Sets the tls cipher suites used.
     *
     * @param tlsCipherSuites The tls cipher suites used.
     */
    public void setTlsCipherSuites(String tlsCipherSuites) {
        this.tlsCipherSuites = tlsCipherSuites;
    }

    /**
     * Returns the connection timeout in milliseconds. -1 means use the system default
     *
     * @return The connection timeout
     */
    @XmlElement(name = "ConnectTimeout", defaultValue = "-1")
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the connection timeout
     *
     * @param connectTimeout The connection timeout
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Retrieved the Read timeout. -1 means use the system default
     *
     * @return The read timeout
     */
    @XmlElement(name = "ReadTimeout", defaultValue = "-1")
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout
     *
     * @param readTimeout The read timeout
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Retrieves the follow redirects flag. Defaults to false
     *
     * @return The follow redirects flag
     */
    @XmlElement(name = "FollowRedirects", defaultValue = "false")
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * Sets the follow redirects flag.
     *
     * @param followRedirects The follow redirects flag
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Retrieves the proxy use options. Either: 'default', 'none', or 'custom'
     *
     * @return The proxy use option.
     */
    @XmlElement(name = "ProxyUse", defaultValue = "default")
    public Option getProxyUse() {
        return proxyUse;
    }

    /**
     * Sets the proxy use option.
     *
     * @param proxyUse The proxy use option.
     */
    public void setProxyUse(Option proxyUse) {
        this.proxyUse = proxyUse;
    }

    /**
     * Retrieves the proxy configuration. This is only needed when the proxy use is set to 'custom'
     *
     * @return The proxy use configuration.
     */
    @XmlElement(name = "ProxyConfiguration")
    public HttpProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    /**
     * Sets the proxy use configuration.
     *
     * @param proxyConfiguration The proxy use configurations
     */
    public void setProxyConfiguration(HttpProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    @XmlType(name = "HttpProxyConfigurationType", propOrder = {"host", "port", "username", "passwordId"})
    public static class HttpProxyConfiguration {
        private String host;
        private int port;
        private String username;
        private String passwordId;

        /**
         * The proxy host. Required
         *
         * @return The proxy host
         */
        @XmlElement(name = "Host")
        public String getHost() {
            return host;
        }

        /**
         * Sets the proxy host.
         *
         * @param host The proxy host
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * Retrieves the proxy port. Required.
         *
         * @return The proxy port
         */
        @XmlElement(name = "Port")
        public int getPort() {
            return port;
        }

        /**
         * Sets the proxy port
         *
         * @param port The proxy port
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * The proxy user name
         *
         * @return The proxy username
         */
        @XmlElement(name = "Username")
        public String getUsername() {
            return username;
        }

        /**
         * Sets the proxy username
         *
         * @param username The proxy username
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * Retrieves the proxy password i d. This is required if a proxy username is specified.
         *
         * @return The proxy password id
         */
        @XmlElement(name = "PasswordId")
        public String getPasswordId() {
            return passwordId;
        }

        /**
         * Sets the proxy password id.
         *
         * @param passwordId The proxy password id.
         */
        public void setPasswordId(String passwordId) {
            this.passwordId = passwordId;
        }
    }

    /**
     * Enumerated type for HTTP protocols.
     */
    @XmlEnum(String.class)
    @XmlType(name = "HttpConfigurationProtocolType")
    public enum Protocol {
        /**
         * Any protocol
         */
        @XmlEnumValue("any")ANY,

        /**
         * The HTTP protocol
         */
        @XmlEnumValue("http")HTTP,

        /**
         * The HTTPS protocol
         */
        @XmlEnumValue("https")HTTPS;
    }

    /**
     * Enumerated type for optional items.
     */
    @XmlEnum(String.class)
    @XmlType(name = "HttpConfigurationOptionType")
    public enum Option {
        /**
         * Use the default value
         */
        @XmlEnumValue("default")DEFAULT,

        /**
         * Do not use any value
         */
        @XmlEnumValue("none")NONE,

        /**
         * Use the specified value
         */
        @XmlEnumValue("custom")CUSTOM
    }
}
