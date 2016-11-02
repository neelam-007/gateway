package com.l7tech.identity.internal;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.PersistentGroup;

import javax.persistence.Column;
import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Proxy;

@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="internal_group")
public class InternalGroup extends PersistentGroup {

    private boolean enabled = true;

    public InternalGroup() {
        this(null);
    }

    public InternalGroup(String name) {
        super(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, name);
    }

    public String toString() {
        return "com.l7tech.identity.internal.InternalGroup." +
                "\n\tname=" + _name +
                "\n\tproviderId=" + getProviderId() +
                "\n\tenabled=" + enabled;
    }

    @Override
    public void copyFrom(PersistentGroup imp ) {
        super.copyFrom(imp);
        if(imp instanceof InternalGroup){
            InternalGroup ig = (InternalGroup) imp;
            enabled = ig.enabled;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        InternalGroup that = (InternalGroup) o;
        return that.enabled == enabled;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result += 31 * result + Boolean.valueOf(enabled).hashCode();
        return result;
    }

    @Column(name="enabled")
    public boolean isEnabled(){
        return enabled;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }
}
