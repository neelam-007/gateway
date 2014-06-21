package com.l7tech.gateway.common.resources;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.objectmodel.imp.ZoneableEntityImp;
import com.l7tech.policy.UsesPrivateKeys;
import com.l7tech.search.Dependency;
import com.l7tech.security.rbac.RbacAttribute;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * HTTP configuration persistent entity.
 */
@Entity
@Proxy(lazy=false)
@Table(name="http_configuration")
public class HttpConfiguration extends ZoneableEntityImp implements UsesPrivateKeys {

    //- PUBLIC

    /**
     * Enumerated type for HTTP protocols.
     */
    public enum Protocol {
        /**
         * The HTTP protocol
         */
        HTTP,

        /**
         * The HTTPS protocol
         */
        HTTPS;
        
        public boolean matches( final String protocol ) {
            return name().equalsIgnoreCase( protocol );
        }
    }

    /**
     * Enumerated type for optional items.
     */
    public enum Option {
        /**
         * Use the default value
         */
        DEFAULT,

        /**
         * Do not use any value
         */
        NONE,

        /**
         * Use the specified value
         */
        CUSTOM
    }

    public HttpConfiguration() {
    }

    /**
     * Create a new configuration that copies the given configuration.
     *
     * <p>This constructor will copy the identity of the given
     * configuration. If you want to copy the values only then the id
     * and version should be reset.</p>
     *
     * @param httpConfiguration The configuration to copy
     * @param lock True to create a locked (read only) copy
     */
    public HttpConfiguration( final HttpConfiguration httpConfiguration,
                              final boolean lock ) {
        super( httpConfiguration );
        setHost( httpConfiguration.getHost() );
        setPort( httpConfiguration.getPort() );
        setProtocol( httpConfiguration.getProtocol() );
        setPath( httpConfiguration.getPath() );
        setUsername( httpConfiguration.getUsername() );
        setPasswordGoid(httpConfiguration.getPasswordGoid());
        setNtlmHost( httpConfiguration.getNtlmHost() );
        setNtlmDomain( httpConfiguration.getNtlmDomain() );
        setTlsVersion( httpConfiguration.getTlsVersion() );
        setTlsKeyUse( httpConfiguration.getTlsKeyUse() );
        setTlsKeystoreGoid(httpConfiguration.getTlsKeystoreGoid());
        setTlsKeystoreAlias( httpConfiguration.getTlsKeystoreAlias() );
        setTlsCipherSuites( httpConfiguration.getTlsCipherSuites() );
        setConnectTimeout( httpConfiguration.getConnectTimeout() );
        setReadTimeout( httpConfiguration.getReadTimeout() );
        setFollowRedirects( httpConfiguration.isFollowRedirects() );
        setProxyUse( httpConfiguration.getProxyUse() );
        setProxyConfiguration( new HttpProxyConfiguration(httpConfiguration.getProxyConfiguration(), lock) );
        setSecurityZone(httpConfiguration.getSecurityZone());
        if ( lock ) lock();
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @RbacAttribute
    @NotNull
    @Size(min=1, max = 128)
    @Column(name="host", nullable=false, length=128)
    public String getHost() {
        return host;
    }

    public void setHost( final String host ) {
        checkLocked();
        this.host = host;
    }

    @RbacAttribute
    @Min(0)
    @Max(0xFFFF)
    @Column(name="port")
    public int getPort() {
        return port;
    }

    public void setPort( final int port ) {
        checkLocked();
        this.port = port;
    }

    @RbacAttribute
    @Column(name="protocol")
    @Enumerated(EnumType.STRING)
    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol( final Protocol protocol ) {
        checkLocked();
        this.protocol = protocol;
    }

    @RbacAttribute
    @Size(max = 4096)
    @Column(name="path", length=4096)
    public String getPath() {
        return path;
    }

    public void setPath( final String path ) {
        checkLocked();
        this.path = path;
    }

    @RbacAttribute
    @Size(max = 255)
    @Column(name="username", length=255)
    public String getUsername() {
        return username;
    }

    public void setUsername( final String username ) {
        checkLocked();
        this.username = username;
    }

    /**
     * Get the identifier of the related secure password.
     *
     * @return The identifier or null.
     */
    @Column(name="password_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getPasswordGoid() {
        return passwordGoid;
    }

    public void setPasswordGoid(final Goid passwordGoid) {
        checkLocked();
        this.passwordGoid = passwordGoid;
    }

    @RbacAttribute
    @Size(max = 128)
    @Column(name="ntlm_host", length=128)
    public String getNtlmHost() {
        return ntlmHost;
    }

    public void setNtlmHost( final String ntlmHost ) {
        checkLocked();
        this.ntlmHost = ntlmHost;
    }

    @RbacAttribute
    @Size(max = 255)
    @Column(name="ntlm_domain", length=255)
    public String getNtlmDomain() {
        return ntlmDomain;
    }

    public void setNtlmDomain( final String ntlmDomain ) {
        checkLocked();
        this.ntlmDomain = ntlmDomain;
    }

    @Size(max = 8)
    @Pattern(regexp="ANY|SSLv3|TLSv1|TLSv1.1|TLSv1.2")
    @Column(name="tls_version", length=8)
    public String getTlsVersion() {
        return tlsVersion;
    }

    public void setTlsVersion( final String tlsVersion ) {
        checkLocked();
        this.tlsVersion = tlsVersion;
    }

    @Column(name="tls_key_use")
    @Enumerated(EnumType.STRING)
    public Option getTlsKeyUse() {
        return tlsKeyUse;
    }

    public void setTlsKeyUse( final Option tlsKeyUse ) {
        checkLocked();
        this.tlsKeyUse = tlsKeyUse;
    }

    @Column(name="tls_keystore_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getTlsKeystoreGoid() {
        return tlsKeystoreGoid;
    }

    public void setTlsKeystoreGoid(final Goid tlsKeystoreGoid) {
        checkLocked();
        this.tlsKeystoreGoid = tlsKeystoreGoid;
    }

    @Size(max = 255)
    @Column(name="tls_key_alias", length=255)
    public String getTlsKeystoreAlias() {
        return tlsKeystoreAlias;
    }

    public void setTlsKeystoreAlias( final String tlsKeystoreAlias ) {
        checkLocked();
        this.tlsKeystoreAlias = tlsKeystoreAlias;
    }

    /**
     * @return TLS cipher suite names to enable, comma delimited, or null to use global defaults.
     */
    @Size(max = 4096)
    @Column(name="tls_cipher_suites", length=4096)
    public String getTlsCipherSuites() {
        return tlsCipherSuites;
    }

    public void setTlsCipherSuites( final String tlsCipherSuites ) {
        this.tlsCipherSuites = tlsCipherSuites;
    }

    /**
     * The connection timeout in milliseconds.
     */
    @Column(name="timeout_connect")
    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout( final int connectTimeout ) {
        checkLocked();
        this.connectTimeout = connectTimeout;
    }

    /**
     * The read timeout in milliseconds.
     */
    @Column(name="timeout_read")
    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout( final int readTimeout ) {
        checkLocked();
        this.readTimeout = readTimeout;
    }

    @Column(name="follow_redirects")
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects( final boolean followRedirects ) {
        checkLocked();
        this.followRedirects = followRedirects;
    }

    @Column(name="proxy_use")
    @Enumerated(EnumType.STRING)
    public Option getProxyUse() {
        return proxyUse;
    }

    public void setProxyUse( final Option proxyUse ) {
        checkLocked();
        this.proxyUse = proxyUse;
    }

    @Valid
    @Embedded
    @Dependency(searchObject = true)
    public HttpProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    public void setProxyConfiguration( final HttpProxyConfiguration proxyConfiguration ) {
        checkLocked();
        this.proxyConfiguration = proxyConfiguration;
    }

    @Override
    @Transient
    public SsgKeyHeader[] getPrivateKeysUsed() {
        if(getTlsKeystoreAlias() != null) {
            return new SsgKeyHeader[]{new SsgKeyHeader(getTlsKeystoreGoid() + ":" + getTlsKeystoreAlias(), getTlsKeystoreGoid(), getTlsKeystoreAlias(), getTlsKeystoreAlias())};
        }
        return null;
    }

    @Override
    public void replacePrivateKeyUsed(@org.jetbrains.annotations.NotNull final SsgKeyHeader oldSSGKeyHeader, @org.jetbrains.annotations.NotNull final SsgKeyHeader newSSGKeyHeader) {
        if(getTlsKeystoreAlias() != null) {
            if(Goid.equals(getTlsKeystoreGoid(), oldSSGKeyHeader.getKeystoreId()) && getTlsKeystoreAlias().equals(oldSSGKeyHeader.getAlias())){
                setTlsKeystoreGoid(newSSGKeyHeader.getKeystoreId());
                setTlsKeystoreAlias(newSSGKeyHeader.getAlias());
            }
        }
    }

    //- PRIVATE

    private String host;
    private int port;
    private Protocol protocol;
    private String path;
    private String username;
    private Goid passwordGoid;
    private String ntlmHost;
    private String ntlmDomain;
    private String tlsVersion;
    private Option tlsKeyUse = Option.DEFAULT;
    private Goid tlsKeystoreGoid;
    private String tlsKeystoreAlias;
    private String tlsCipherSuites;
    private int connectTimeout = -1;
    private int readTimeout = -1;
    private boolean followRedirects;
    private Option proxyUse = Option.DEFAULT;
    private HttpProxyConfiguration proxyConfiguration = new HttpProxyConfiguration();
}
