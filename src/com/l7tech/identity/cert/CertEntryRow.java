package com.l7tech.identity.cert;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Oct 23, 2003
 * Time: 11:04:50 AM
 * $Id$
 *
 * This bean class is meant to be used by hibernate only.
 * (because hibernate needs to map a class type to a table)
 */
public class CertEntryRow {
    public long getProviderId() {
        return providerId;
    }

    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
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

    private long providerId;
    private String userLogin;
    private String cert;
    private int resetCounter;
    private long oid = -1;
}
