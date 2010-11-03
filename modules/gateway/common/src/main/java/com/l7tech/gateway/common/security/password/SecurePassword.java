package com.l7tech.gateway.common.security.password;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

/**
 * Holds an encrypted password.
 */
@Entity
@Proxy(lazy=false)
@Table(name="secure_password")
public class SecurePassword extends NamedEntityImp {
    private String description;
    private String encodedPassword;
    private Date lastUpdate;
    private boolean usageFromVariable;
    
    public SecurePassword() {
    }

    public SecurePassword(String name, long lastUpdate) {
        setName(name);
        this.lastUpdate = new Date(lastUpdate);
    }

    public SecurePassword(String name) {
        this(name, new Date().getTime());
    }

    @Column(name = "description", length = 256, nullable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name = "usage_from_variable")
    public boolean isUsageFromVariable() {
        return usageFromVariable;
    }

    public void setUsageFromVariable(boolean usageFromVariable) {
        this.usageFromVariable = usageFromVariable;
    }

    @Column(name = "encoded_password", length = 256, nullable = false)
    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    @Column(name = "last_update")
    public long getLastUpdate() {
        return lastUpdate == null ? 0 : lastUpdate.getTime();
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = new Date(lastUpdate);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Transient
    public Date getLastUpdateAsDate() {
        return lastUpdate;
    }

    @Override
    public String toString() {
        return getName();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurePassword)) return false;
        if (!super.equals(o)) return false;

        SecurePassword that = (SecurePassword) o;

        if (usageFromVariable != that.usageFromVariable) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (encodedPassword != null ? !encodedPassword.equals(that.encodedPassword) : that.encodedPassword != null)
            return false;
        if (lastUpdate != null ? !lastUpdate.equals(that.lastUpdate) : that.lastUpdate != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (encodedPassword != null ? encodedPassword.hashCode() : 0);
        result = 31 * result + (lastUpdate != null ? lastUpdate.hashCode() : 0);
        result = 31 * result + (usageFromVariable ? 1 : 0);
        return result;
    }
}
