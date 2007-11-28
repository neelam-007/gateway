package com.l7tech.common.policy;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * Represents a revision of some policy XML.
 * This is used only by the SSM GUI to provide a versioning service to admins.
 * This class is not used at all by the Gateway runtime.
 */
public class PolicyVersion extends NamedEntityImp {
    private long ordinal;
    private long time;
    private long userProviderOid;
    private String userLogin;
    private long parentVersionOid;
    private String xml;
    private long policyOid;
    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getPolicyOid() {
        return policyOid;
    }

    public void setPolicyOid(long policyOid) {
        this.policyOid = policyOid;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public long getParentVersionOid() {
        return parentVersionOid;
    }

    public void setParentVersionOid(long parentVersionOid) {
        this.parentVersionOid = parentVersionOid;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public long getUserProviderOid() {
        return userProviderOid;
    }

    public void setUserProviderOid(long userProviderOid) {
        this.userProviderOid = userProviderOid;
    }

    /**
     * @return the timestamp as milliseconds since the epoch
     */
    public long getTime() {
        return time;
    }

    /**
     * @param time the timestamp as milliseconds since the epoch
     */
    public void setTime(long time) {
        this.time = time;
    }

    public long getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(long ordinal) {
        this.ordinal = ordinal;
    }
}
