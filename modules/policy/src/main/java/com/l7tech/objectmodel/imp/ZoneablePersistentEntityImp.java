package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import org.jetbrains.annotations.Nullable;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Convenience superclass for PersistentEntities that can be placed into security zones.
 */
@MappedSuperclass
public abstract class ZoneablePersistentEntityImp extends PersistentEntityImp implements ZoneableEntity {
    protected SecurityZone securityZone;

    protected ZoneablePersistentEntityImp() {
        super();
    }

    protected ZoneablePersistentEntityImp(final PersistentEntity entity) {
        super(entity);
    }

    @Override
    @ManyToOne
    @JoinColumn(name = "security_zone_oid")
    @XmlTransient
    public SecurityZone getSecurityZone() {
        return securityZone;
    }

    @Override
    public void setSecurityZone(@Nullable final SecurityZone securityZone) {
        this.securityZone = securityZone;
    }
}
