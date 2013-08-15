package com.l7tech.identity;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.GoidEntityImp;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.*;

import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

/**
 * The logon information object stores information about the user that has attempted to log into the system.
 * Such information are the number of failed attempts and the last login attempt was made.  The purpose of this is
 * to be able to track down the user activities upon logins.
 *
 * This entity is only ever created for administrative users. It is possible that if administrative users lose their
 * permission that LogonInfo entities may exist for users who are no longer administrators.
 *
 * User: dlee
 * Date: Jun 24, 2008
 */
@XmlRootElement
@Proxy(lazy=false)
@Entity
@Table(name="logon_info")
public class LogonInfo extends GoidEntityImp {


    private String login;
    private Goid providerId;
    private long lastAttempted;   //last login attempt
    private int failCount;  //number of login failure attempts
    private long lastActivity;
    private State state = State.ACTIVE;

    public LogonInfo() {
        this.failCount = 0;
    }

    /**
     * Create a new LogonInfo for a user. Once persisted the current time is used for the users last activity. This
     * allows the users account to be tracked for inactivity from when the LogonInfo is created and persisted.
     *
     * @param providerId users provider id
     * @param login users unique logon
     */
    public LogonInfo(Goid providerId, String login) {
        this.providerId = providerId;
        this.login = login;
        this.lastAttempted = -1;
        this.failCount = 0;
        this.lastActivity = System.currentTimeMillis();
    }

    static public enum State {
        ACTIVE, EXCEED_ATTEMPT, INACTIVE
    }

    @Column(name="provider_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getProviderId() {
        return providerId;
    }

    public void setProviderId(Goid providerId) {
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

    @Column(name="last_activity")                 
    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    @Column(name="state")
    @Enumerated(EnumType.STRING)
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
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
        if (providerId != null ? !providerId.equals(logonInfo.providerId) : logonInfo.providerId != null) return false;
        if (login != null ? !login.equals(logonInfo.login) : logonInfo.login != null) return false;
        if (state != logonInfo.state) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (login != null ? login.hashCode() : 0);
        result = 31 * result + (providerId != null ? providerId.hashCode() : 0);
        result = 31 * result + (int) (lastAttempted ^ (lastAttempted >>> 32));
        result = 31 * result + failCount;
        result = 31 * result + (state != null ? state.hashCode() : 0);

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
        setLastAttempted(time);
        setFailCount(getFailCount() + 1);
    }

    public void resetFailCount(long time) {
        setLastAttempted(time);
        setFailCount(0);
    }
}
