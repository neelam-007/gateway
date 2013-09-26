package com.l7tech.gateway.common.resources;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.XmlSafe;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Bean for HTTP proxy configuration properties.
 */
@Embeddable
@XmlSafe
public class HttpProxyConfiguration implements Serializable {

    //- PUBLIC

    @XmlSafe
    public HttpProxyConfiguration(){
        locked = false;
    }

    @XmlSafe
    public HttpProxyConfiguration( final HttpProxyConfiguration httpProxyConfiguration,
                                   final boolean lock ) {
        this.locked = lock;
        if ( httpProxyConfiguration != null ) {
            this.host = httpProxyConfiguration.host;
            this.port = httpProxyConfiguration.port;
            this.username = httpProxyConfiguration.username;
            this.passwordGoid = httpProxyConfiguration.passwordGoid;
        }
    }

    @Size(max = 128)
    @XmlSafe
    @Column(name="proxy_host", length=128)
    public String getHost() {
        return host;
    }

    @XmlSafe
    public void setHost( final String host ) {
        checkLocked();
        this.host = host;
    }

    @Min(0)
    @Max(0xFFFF)
    @XmlSafe
    @Column(name="proxy_port")
    public int getPort() {
        return port;
    }

    @XmlSafe
    public void setPort( final int port ) {
        checkLocked();
        this.port = port;
    }

    @Size(max = 255)
    @XmlSafe
    @Column(name="proxy_username", length=255)
    public String getUsername() {
        return username;
    }

    @XmlSafe
    public void setUsername( final String username ) {
        checkLocked();
        this.username = username;
    }

    @XmlSafe
    @Column(name="proxy_password_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.SECURE_PASSWORD)
    public Goid getPasswordGoid() {
        return passwordGoid;
    }

    @XmlSafe
    public void setPasswordGoid(final Goid passwordGoid) {
        checkLocked();
        this.passwordGoid = passwordGoid;
    }

    /**
     * @deprecated This is only needed here for de-serialization purposed for the ioHttpProxy cluster property
     */
    @Deprecated
    @XmlSafe
    public void setPasswordOid(final Long passwordOid) {
        checkLocked();
        this.passwordGoid = GoidUpgradeMapper.mapOid(EntityType.SECURE_PASSWORD, passwordOid);
    }

    //- PRIVATE

    private final boolean locked;
    private String host;
    private int port;
    private String username;
    private Goid passwordGoid;

    private void checkLocked() {
        if ( locked ) throw new IllegalStateException("Cannot update locked configuration");
    }
}
