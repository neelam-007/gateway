package com.l7tech.server.identity.cert;

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
public class CertEntryRow {
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

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public int getResetCounter() {
        return resetCounter;
    }

    public void setResetCounter(int resetCounter) {
        this.resetCounter = resetCounter;
    }

    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    private long provider;
    private String login;
    private String cert;
    private int resetCounter;
    private long oid = -1;
}
