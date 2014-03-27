package com.l7tech.policy;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.Transient;
import javax.validation.constraints.Size;

/**
 * Represents a revision of some policy XML.
 * This is used only by the SSM GUI to provide a versioning service to admins.
 * This class is not used at all by the Gateway runtime.
 */
public class PolicyVersion extends NamedEntityImp {
    private long ordinal;
    private long time;
    private Goid userProviderGoid;
    private String userLogin;
    private String xml;
    private Goid policyGoid;
    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Goid getPolicyGoid() {
        return policyGoid;
    }

    public void setPolicyGoid(Goid policyGoid) {
        this.policyGoid = policyGoid;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public Goid getUserProviderGoid() {
        return userProviderGoid;
    }

    public void setUserProviderGoid(Goid userProviderGoid) {
        this.userProviderGoid = userProviderGoid;
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

    @Size(max = 255)
    @Override
    @Transient
    public String getName() {
        return super.getName();
    }

    public String toString() {
        return "[PolicyVersion goid=" + getGoid() + " policyGoid=" + getPolicyGoid() + ']';
    }
}
