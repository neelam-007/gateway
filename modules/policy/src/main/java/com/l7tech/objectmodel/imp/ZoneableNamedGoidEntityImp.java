package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import com.l7tech.objectmodel.migration.Migration;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Convenience superclass for named entities that can be placed into security zones.
 */
@MappedSuperclass
public abstract class ZoneableNamedGoidEntityImp extends NamedGoidEntityImp implements ZoneableEntity {
    protected SecurityZone securityZone;

    protected ZoneableNamedGoidEntityImp() {
    }

    protected ZoneableNamedGoidEntityImp(NamedGoidEntityImp entity) {
        super(entity);
    }

    @Override
    @ManyToOne
    @JoinColumn(name = "security_zone_oid")
    @XmlTransient  // TODO remove XmlTransient and expose security zone in generated XML
    @Migration(dependency = false)
    public SecurityZone getSecurityZone() {
        return securityZone;
    }

    @Override
    public void setSecurityZone(SecurityZone securityZone) {
        this.securityZone = securityZone;
    }

    // equals and hashCode are not overridden here -- subclasses may or may not choose to include the security zone
    // in the equals and hashCode method
}
