package com.l7tech.gateway.common.resources;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Bean for HTTP proxy configuration properties.
 */
@Embeddable
public class HttpProxyConfiguration implements Serializable {

    //- PUBLIC

    public HttpProxyConfiguration(){
        locked = false;
    }

    public HttpProxyConfiguration( final HttpProxyConfiguration httpProxyConfiguration,
                                   final boolean lock ) {
        this.locked = lock;
        if ( httpProxyConfiguration != null ) {
            this.host = httpProxyConfiguration.host;
            this.port = httpProxyConfiguration.port;
            this.username = httpProxyConfiguration.username;
            this.passwordOid = httpProxyConfiguration.passwordOid;
        }
    }

    @Column(name="proxy_host", length=128)
    public String getHost() {
        return host;
    }

    public void setHost( final String host ) {
        checkLocked();
        this.host = host;
    }

    @Column(name="proxy_port")
    public int getPort() {
        return port;
    }

    public void setPort( final int port ) {
        checkLocked();
        this.port = port;
    }

    @Column(name="proxy_username", length=255)
    public String getUsername() {
        return username;
    }

    public void setUsername( final String username ) {
        checkLocked();
        this.username = username;
    }

    @Column(name="proxy_password_oid")
    public Long getPasswordOid() {
        return passwordOid;
    }

    public void setPasswordOid( final Long passwordOid ) {
        checkLocked();
        this.passwordOid = passwordOid;
    }

    //- PRIVATE

    private final boolean locked;
    private String host;
    private int port;
    private String username;
    private Long passwordOid;

    private void checkLocked() {
        if ( locked ) throw new IllegalStateException("Cannot update locked configuration");
    }
}
