package com.l7tech.kerberos;

import java.io.Serializable;
import java.util.Date;

public class KeyTabEntryInfo implements Serializable {

    private String kdc;
    private String realm;
    private String principalName;
    private Date date;
    private Integer version;
    private int etype;

    public String getKdc() {
        return kdc;
    }

    public void setKdc(String kdc) {
        this.kdc = kdc;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public int getEType() {
        return etype;
    }

    public void setEtype(int etype) {
        this.etype = etype;
    }
}
