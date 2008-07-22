package com.l7tech.server.identity.cert;

import com.l7tech.security.cert.X509Entity;

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
public class CertEntryRow extends X509Entity {
    public long getProvider() {
        return provider;
    }

    public void setProvider(long providerId) {
        this.provider = providerId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String userLogin) {
        this.login = userLogin;
    }

    public int getResetCounter() {
        return resetCounter;
    }

    public void setResetCounter(int resetCounter) {
        this.resetCounter = resetCounter;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId( String userId ) {
        this.userId = userId;
    }

    private long provider;
    private String login;
    private String userId;
    private int resetCounter;
}
