package com.l7tech.server.identity.cert;

import com.l7tech.security.cert.X509Entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.AttributeOverrides;
import javax.persistence.AttributeOverride;

/**
 * Bean representation of a row in the client_cert table. This bean class is meant to be used by hibernate only.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Oct 23, 2003<br/>
 * $Id$
 */
@Entity
@Table(name="client_cert")
@AttributeOverride(name="certBase64",column=@Column(name="cert"))
public class CertEntryRow extends X509Entity {

    @Column(name="provider",nullable=false)
    public long getProvider() {
        return provider;
    }

    public void setProvider(long providerId) {
        mutate();
        this.provider = providerId;
    }

    @Column(name="login", length=255)
    public String getLogin() {
        return login;
    }

    public void setLogin(String userLogin) {
        mutate();
        this.login = userLogin;
    }

    @Column(name="reset_counter", nullable=false)
    public int getResetCounter() {
        return resetCounter;
    }

    public void setResetCounter(int resetCounter) {
        mutate();
        this.resetCounter = resetCounter;
    }

    @Column(name="user_id",length=255)
    public String getUserId() {
        return userId;
    }

    public void setUserId( String userId ) {
        mutate();
        this.userId = userId;
    }

    private long provider;
    private String login;
    private String userId;
    private int resetCounter;
}
