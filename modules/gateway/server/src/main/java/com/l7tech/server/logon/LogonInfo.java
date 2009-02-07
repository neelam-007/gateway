package com.l7tech.server.logon;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.*;

import org.hibernate.annotations.Proxy;

/**
 * The logon information object stores information about the user that has attempted to log into the system.
 * Such information are the number of failed attempts and the last login attempt was made.  The purpose of this is
 * to be able to track down the user activites upon logins.
 *
 * User: dlee
 * Date: Jun 24, 2008
 */
@XmlRootElement
@Proxy(lazy=false)
@Entity
@Table(name="logon_info")
public class LogonInfo extends PersistentEntityImp {
    private String login;
    private long providerId;
    private long lastAttempted;   //last login attempt
    private int failCount;  //number of login failure attempts

    public LogonInfo() {
        this.failCount = 0;
    }

    public LogonInfo(long providerId, String login) {
        this.providerId = providerId;
        this.login = login;
        this.lastAttempted = System.currentTimeMillis();
        this.failCount = 0;
    }

    @Column(name="provider_oid")
    public long getProviderId() {
        return providerId;
    }

    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    @Column(name="login")
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Column(name="last_attempted")
    public long getLastAttempted() {
        return lastAttempted;
    }

    public void setLastAttempted(long lastAttempted) {
        this.lastAttempted = lastAttempted;
    }

    @Column(name="fail_count")
    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LogonInfo logonInfo = (LogonInfo) o;

        if (failCount != logonInfo.failCount) return false;
        if (lastAttempted != logonInfo.lastAttempted) return false;
        if (providerId != logonInfo.providerId) return false;
        if (login != null ? !login.equals(logonInfo.login) : logonInfo.login != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (login != null ? login.hashCode() : 0);
        result = 31 * result + (int) (providerId ^ (providerId >>> 32));
        result = 31 * result + (int) (lastAttempted ^ (lastAttempted >>> 32));
        result = 31 * result + failCount;
        return result;
    }

    /**
     * Perform a deep copy.
     *
     * @param logonInfo   The logon info object's attributes that will be copied
     */
    public void copy(LogonInfo logonInfo) {
        this.providerId = logonInfo.getProviderId();
        this.login = logonInfo.getLogin();
        this.lastAttempted = logonInfo.getLastAttempted();
        this.failCount = logonInfo.getFailCount();
    }

    public void failLogonAttempt(long time) {
        this.lastAttempted = time;
        this.failCount++;
    }

    public void failLogonAttemptWithoutTimestamp() {
        this.failCount++;
    }

    public void successfulLogonAttempt(long time ) {
        this.lastAttempted = time;
        this.failCount = 0;
    }

    public void resetFailCount(long time) {
        this.lastAttempted = time;
        this.failCount = 0;
    }
}
