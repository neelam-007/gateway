/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.cert;

import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.X509Entity;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

/**
 * A certificate for a user in an SSG Identity Provider; not necessarily a certificate issued by the SSG's CA.
 */
@Entity
@Proxy(lazy=false)
@Table(name="client_cert")
@AttributeOverride(name="certBase64",column=@Column(name="cert"))
public class CertEntryRow extends X509Entity {

    @Column(name="provider", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getProvider() {
        return provider;
    }

    public void setProvider(Goid providerId) {
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

    private Goid provider;
    private String login;
    private String userId;
    private int resetCounter;
}
