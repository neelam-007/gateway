package com.l7tech.identity.internal;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A bean that holds a particular password that was changed at a particular time.
 *
 * User: dlee
 * Date: Jun 20, 2008
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="password_history")
public class PasswordChangeRecord extends PersistentEntityImp {
    private static final long serialVersionUID = 1988465921721711468L;

    private InternalUser internalUser;
    private long lastChanged; //that last changed timestamp
    private String prevHashedPassword; //previous password before password was changed

    /**
     * For hibernate only.
     */
    public PasswordChangeRecord() {
    }

    public PasswordChangeRecord(InternalUser internalUser, long lastChanged, String prevHashedPassword) {
        if(internalUser == null) throw new NullPointerException("internalUser cannot be null.");
        if(prevHashedPassword == null || prevHashedPassword.trim().isEmpty())
            throw new IllegalArgumentException("prevHashedPassword cannot be null or empty.");
        
        this.internalUser = internalUser;
        this.lastChanged = lastChanged;
        this.prevHashedPassword = prevHashedPassword;
    }

    @ManyToOne
    @JoinColumn(name="internal_user_goid", nullable=false)
    public InternalUser getInternalUser() {
        return internalUser;
    }

    public void setInternalUser(InternalUser internalUser) {
        this.internalUser = internalUser;
    }

    @Column(name="last_changed")
    public long getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(long lastChanged) {
        this.lastChanged = lastChanged;
    }

    @Column(name="prev_password", nullable = false)
    public String getPrevHashedPassword() {
        return prevHashedPassword;
    }

    public void setPrevHashedPassword(String prevHashedPassword) {
        this.prevHashedPassword = prevHashedPassword;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;

        PasswordChangeRecord that = (PasswordChangeRecord) obj;

        return lastChanged == that.lastChanged &&
               prevHashedPassword != null ? prevHashedPassword.equals(that.prevHashedPassword) : that.prevHashedPassword == null;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (prevHashedPassword != null ? prevHashedPassword.hashCode() : 0);
        result = 31 * result + (int) (lastChanged ^ (lastChanged >>> 32));
        return result;
    }
}
